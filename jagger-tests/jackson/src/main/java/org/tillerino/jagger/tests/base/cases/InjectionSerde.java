package org.tillerino.jagger.tests.base.cases;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonInput;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.api.SerializationContext;
import org.tillerino.jagger.tests.model.features.DelegationModel.SelfReferencingRecord;

public interface InjectionSerde {
    @JsonOutput
    void writeList(List<SelfReferencingRecord> list, JsonGenerator gen, InjectionSerializationContext ctx)
            throws Exception;

    @JsonOutput
    default void writeWithInjection(SelfReferencingRecord rec, JsonGenerator gen, InjectionSerializationContext ctx)
            throws Exception {
        String existingName = ctx.names.get(new InjectionSerializationContext.Reference(rec));
        if (existingName != null) {
            // WriterMode.RETURN: return writeString(existingName);
            writeString(existingName, gen);
        } else {
            if (rec != null) {
                // this should crash on duplicate names , but it's just an example
                ctx.names.put(new InjectionSerializationContext.Reference(rec), rec.prop());
            }
            // WriterMode.RETURN: return writeInjectionRecord(rec, ctx);
            writeInjectionRecord(rec, gen, ctx);
        }
    }

    @JsonOutput
    @JsonConfig(delegateTo = JsonConfig.DelegateeMode.DO_NOT_DELEGATE_TO)
    void writeInjectionRecord(SelfReferencingRecord rec, JsonGenerator gen, InjectionSerializationContext ctx)
            throws Exception;

    @JsonOutput
    void writeString(String str, JsonGenerator gen) throws Exception;

    class InjectionSerializationContext extends SerializationContext {
        Map<Reference, String> names = new LinkedHashMap<>();

        static class Reference {
            Object object;

            public Reference(Object object) {
                this.object = object;
            }

            @Override
            public boolean equals(Object o) {
                return object == ((Reference) o).object;
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(object);
            }
        }
    }

    class InjectionDeserializationContext extends SerializationContext {
        Map<String, Object> objects = new LinkedHashMap<>();
    }

    /** NOCOPY */
    @JsonInput
    List<SelfReferencingRecord> readList(JsonParser parser, InjectionDeserializationContext ctx) throws Exception;

    /** NOCOPY This cannot be copied for the other backends because we need to access the generator directly. */
    @JsonInput
    default SelfReferencingRecord readWithInjection(JsonParser parser, InjectionDeserializationContext ctx)
            throws Exception {
        if (!parser.hasCurrentToken()) {
            parser.nextToken();
        }
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            SelfReferencingRecord value = (SelfReferencingRecord) ctx.objects.get(parser.getText());
            parser.nextToken();
            return value;
        }
        SelfReferencingRecord value = readInjectionRecord(parser, ctx);
        if (value != null) {
            ctx.objects.put(value.prop(), value);
        }
        return value;
    }

    /** NOCOPY */
    @JsonInput
    @JsonConfig(delegateTo = JsonConfig.DelegateeMode.DO_NOT_DELEGATE_TO)
    SelfReferencingRecord readInjectionRecord(JsonParser parser, InjectionDeserializationContext ctx) throws Exception;
}
