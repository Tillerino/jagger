package org.tillerino.jagger.processor.databind;

import com.squareup.javapoet.CodeBlock.Builder;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.processor.Detector;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PrototypeKind;

public class DatabindDetector implements Detector {
    static final String JACKSON_JSON_GENERATOR = "com.fasterxml.jackson.core.JsonGenerator";
    static final String JACKSON_JSON_PARSER = "com.fasterxml.jackson.core.JsonParser";

    static final String GSON_JSON_READER = "com.google.gson.stream.JsonReader";
    static final String GSON_JSON_WRITER = "com.google.gson.stream.JsonWriter";

    static final String FASTJSON_2_JSONREADER = "com.alibaba.fastjson2.JSONReader";
    static final String FASTJSON_2_JSONWRITER = "com.alibaba.fastjson2.JSONWriter";

    static final String JAKARTA_JSON_PARSER = "org.tillerino.jagger.helpers.JakartaJsonParserHelper.JsonParserWrapper";
    static final String JAKARTA_JSON_GENERATOR = "jakarta.json.stream.JsonGenerator";

    static final String NANOJSON_JSON_WRITER = "com.grack.nanojson.JsonAppendableWriter";

    static final String JAGGER_READER = "org.tillerino.jagger.api.JaggerReader";
    static final String JAGGER_WRITER = "org.tillerino.jagger.api.JaggerWriter";

    private final JaggerContext ctx;

    public final TypeMirror jacksonJsonGenerator;
    public final TypeMirror jacksonJsonParser;

    public final TypeMirror gsonJsonReader;
    public final TypeMirror gsonJsonWriter;

    public final TypeMirror fastjson2JsonReader;
    public final TypeMirror fastjson2JsonWriter;

    public final TypeMirror jakartaJsonParser;
    public final TypeMirror jakartaJsonGenerator;

    public final TypeMirror nanojsonJsonWriter;

    public final TypeMirror jaggerReader;
    public final TypeMirror jaggerWriter;

    public final TypeMirror serializationContext;
    public final TypeMirror deserializationContext;
    private final TypeElement jsonInput;
    private final TypeElement jsonOutput;

    public DatabindDetector(JaggerContext ctx) {
        this.ctx = ctx;

        jacksonJsonGenerator = ctx.commonTypes.nullableTypeMirror(JACKSON_JSON_GENERATOR);
        jacksonJsonParser = ctx.commonTypes.nullableTypeMirror(JACKSON_JSON_PARSER);

        gsonJsonReader = ctx.commonTypes.nullableTypeMirror(GSON_JSON_READER);
        gsonJsonWriter = ctx.commonTypes.nullableTypeMirror(GSON_JSON_WRITER);

        fastjson2JsonReader = ctx.commonTypes.nullableTypeMirror(FASTJSON_2_JSONREADER);
        fastjson2JsonWriter = ctx.commonTypes.nullableTypeMirror(FASTJSON_2_JSONWRITER);

        jakartaJsonParser = ctx.commonTypes.nullableTypeMirror(JAKARTA_JSON_PARSER);
        jakartaJsonGenerator = ctx.commonTypes.nullableTypeMirror(JAKARTA_JSON_GENERATOR);

        nanojsonJsonWriter = ctx.commonTypes.nullableTypeMirror(NANOJSON_JSON_WRITER);

        jaggerReader = ctx.commonTypes.nullableTypeMirror(JAGGER_READER);
        jaggerWriter = ctx.commonTypes.nullableTypeMirror(JAGGER_WRITER);

        serializationContext = ctx.elements
                .getTypeElement(SerializationContext.class.getName())
                .asType();
        deserializationContext = ctx.elements
                .getTypeElement(DeserializationContext.class.getName())
                .asType();

        jsonInput = ctx.elements.getTypeElement("org.tillerino.jagger.annotations.JsonInput");
        jsonOutput = ctx.elements.getTypeElement("org.tillerino.jagger.annotations.JsonOutput");
    }

    @Override
    public List<TypeElement> supportedAnnotationTypes() {
        return List.of(jsonInput, jsonOutput);
    }

    @Override
    public Optional<PrototypeKind> detect(InstantiatedMethod m) {
        return detectJsonInput(m).or(() -> detectJsonOutput(m));
    }

    private Optional<PrototypeKind> detectJsonInput(InstantiatedMethod m) {
        if (ctx.annotations.findAnnotation(m.element(), jsonInput).isPresent()
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            return PrototypeKind.detect(
                    PrototypeKind.nullableTypeList(
                            jacksonJsonParser, gsonJsonReader, fastjson2JsonReader, jakartaJsonParser, jaggerReader),
                    m,
                    ctx,
                    (externalType, externalParameter, otherParameters) ->
                            new JsonInput(externalType, m.returnType(), otherParameters, deserializationContext));
        }
        return Optional.empty();
    }

    private Optional<PrototypeKind> detectJsonOutput(InstantiatedMethod m) {
        if (ctx.annotations.findAnnotation(m.element(), jsonOutput).isPresent()
                && m.returnType().getKind() == TypeKind.VOID
                && m.parameters().size() >= 2) {
            return PrototypeKind.detect(
                    PrototypeKind.nullableTypeList(
                            jacksonJsonGenerator,
                            gsonJsonWriter,
                            fastjson2JsonWriter,
                            jakartaJsonGenerator,
                            nanojsonJsonWriter,
                            jaggerWriter),
                    m,
                    ctx,
                    (externalType, externalParameter, otherParameters) -> new JsonOutput(
                            externalType,
                            otherParameters.get(0).type(),
                            otherParameters.subList(1, otherParameters.size()),
                            serializationContext));
        }
        return Optional.empty();
    }

    record JsonInput(
            TypeMirror externalType,
            TypeMirror internalType,
            List<InstantiatedVariable> otherParameters,
            TypeMirror cType)
            implements PrototypeKind {
        @Override
        public PrototypeKind withInternalType(TypeMirror newType) {
            return new JsonInput(externalType, newType, otherParameters, cType);
        }

        @Override
        public Direction direction() {
            return Direction.INPUT;
        }

        @Override
        public String defaultMethodName() {
            return "read" + PrototypeKind.simpleTypeName(internalType());
        }

        @Override
        public Optional<TypeMirror> contextType() {
            return Optional.of(cType);
        }

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return switch (externalType().toString()) {
                case JACKSON_JSON_PARSER -> new JacksonJsonParserReaderGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case GSON_JSON_READER -> new GsonJsonReaderReaderGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case FASTJSON_2_JSONREADER -> new Fastjson2ReaderGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case JAKARTA_JSON_PARSER -> new JakartaJsonParserGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case JAGGER_READER -> new JaggerReaderGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                default -> throw new ContextedRuntimeException("Unknown input type: " + externalType());
            };
        }
    }

    record JsonOutput(
            TypeMirror externalType,
            TypeMirror internalType,
            List<InstantiatedMethod.InstantiatedVariable> otherParameters,
            TypeMirror cType)
            implements PrototypeKind {
        @Override
        public PrototypeKind withInternalType(TypeMirror newType) {
            return new JsonOutput(externalType, newType, otherParameters, cType);
        }

        @Override
        public Direction direction() {
            return Direction.OUTPUT;
        }

        @Override
        public String defaultMethodName() {
            return "write" + PrototypeKind.simpleTypeName(internalType());
        }

        @Override
        public Optional<TypeMirror> contextType() {
            return Optional.of(cType);
        }

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            return switch (externalType().toString()) {
                case JACKSON_JSON_GENERATOR -> new JacksonJsonGeneratorWriterGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case GSON_JSON_WRITER -> new GsonJsonWriterWriterGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case FASTJSON_2_JSONWRITER -> new Fastjson2WriterGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case JAKARTA_JSON_GENERATOR -> new JakartaJsonGeneratorGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case NANOJSON_JSON_WRITER -> new NanojsonWriterGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                case JAGGER_WRITER -> new JaggerWriterGenerator(
                                context.ctx(), context.prototype(), context.generatedClass())
                        .build();
                default -> throw new ContextedRuntimeException("Unknown output type: " + externalType());
            };
        }
    }

    enum Direction {
        INPUT,
        OUTPUT,
        ;
    }
}
