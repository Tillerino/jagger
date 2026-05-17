package org.tillerino.jagger.processor.databind;

import com.squareup.javapoet.CodeBlock.Builder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.tillerino.jagger.api.DeserializationContext;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.ext.PrototypeDetector;
import org.tillerino.jagger.processor.ext.PrototypeKind;
import org.tillerino.jagger.processor.ext.PrototypeKind.TemplatablePrototypeKind;
import org.tillerino.jagger.processor.util.Annotations.AnnotationMirrorWrapper;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;
import org.tillerino.jagger.processor.util.PlainTypeName;

public class DatabindPrototypeDetector implements PrototypeDetector {
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

    static final String JSON_INPUT = "org.tillerino.jagger.annotations.JsonInput";
    static final String JSON_OUTPUT = "org.tillerino.jagger.annotations.JsonOutput";

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

    public DatabindPrototypeDetector(JaggerContext ctx) {
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
    }

    @Override
    public Collection<String> supportedAnnotationTypes() {
        return Arrays.asList(JSON_INPUT, JSON_OUTPUT);
    }

    @Override
    public Optional<PrototypeKind> detect(InstantiatedMethod m, AnnotationMirrorWrapper annotation) {
        return detectJsonInput(m).or(() -> detectJsonOutput(m));
    }

    private Optional<PrototypeKind> detectJsonInput(InstantiatedMethod m) {
        if (m.findAnnotation(JSON_INPUT).isPresent()
                && m.returnType().getKind() != TypeKind.VOID
                && !m.parameters().isEmpty()) {
            return PrototypeKind.detect(
                    PrototypeKind.nullableTypeList(
                            jacksonJsonParser, gsonJsonReader, fastjson2JsonReader, jakartaJsonParser, jaggerReader),
                    m,
                    ctx,
                    (externalType, externalParameter, otherParameters) -> new DatabindPrototypeKind(
                            Direction.INPUT,
                            List.of(m.returnType(), externalType),
                            otherParameters,
                            deserializationContext));
        }
        return Optional.empty();
    }

    private Optional<PrototypeKind> detectJsonOutput(InstantiatedMethod m) {
        if (m.findAnnotation(JSON_OUTPUT).isPresent()
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
                    (externalType, externalParameter, otherParameters) -> new DatabindPrototypeKind(
                            Direction.OUTPUT,
                            List.of(otherParameters.get(0).type(), externalType),
                            otherParameters.subList(1, otherParameters.size()),
                            serializationContext));
        }
        return Optional.empty();
    }

    record DatabindPrototypeKind(
            Direction specialization,
            List<TypeMirror> types,
            List<InstantiatedVariable> otherParameters,
            TypeMirror contextType)
            implements TemplatablePrototypeKind {
        @Override
        public TemplatablePrototypeKind withTypes(List<TypeMirror> newTypes) {
            return new DatabindPrototypeKind(specialization, newTypes, otherParameters, contextType);
        }

        @Override
        public String defaultMethodName() {
            return (specialization == Direction.INPUT ? "read" : "write") + PlainTypeName.of(types().get(0));
        }

        @Override
        public Builder generateCode(CodeGeneratorContext context) {
            if (specialization == Direction.INPUT) {
                return switch (types().get(1).toString()) {
                    case JACKSON_JSON_PARSER -> new JacksonJsonParserReaderGenerator(context).build();
                    case GSON_JSON_READER -> new GsonJsonReaderReaderGenerator(context).build();
                    case FASTJSON_2_JSONREADER -> new Fastjson2ReaderGenerator(context).build();
                    case JAKARTA_JSON_PARSER -> new JakartaJsonParserGenerator(context).build();
                    case JAGGER_READER -> new JaggerReaderGenerator(context).build();
                    default -> throw new ContextedRuntimeException("Unknown input type: " + types().get(1));
                };
            }

            return switch (types().get(1).toString()) {
                case JACKSON_JSON_GENERATOR -> new JacksonJsonGeneratorWriterGenerator(context).build();
                case GSON_JSON_WRITER -> new GsonJsonWriterWriterGenerator(context).build();
                case FASTJSON_2_JSONWRITER -> new Fastjson2WriterGenerator(context).build();
                case JAKARTA_JSON_GENERATOR -> new JakartaJsonGeneratorGenerator(context).build();
                case NANOJSON_JSON_WRITER -> new NanojsonWriterGenerator(context).build();
                case JAGGER_WRITER -> new JaggerWriterGenerator(context).build();
                default -> throw new ContextedRuntimeException("Unknown output type: " + types().get(1));
            };
        }
    }

    enum Direction {
        INPUT,
        OUTPUT,
        ;
    }
}
