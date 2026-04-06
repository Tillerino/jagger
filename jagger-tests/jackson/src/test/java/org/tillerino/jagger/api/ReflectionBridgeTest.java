package org.tillerino.jagger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.api.ReflectionBridge.DeserializerDescription;
import org.tillerino.jagger.api.ReflectionBridge.SerializerDescription;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.ScalarFieldsRecordSerde;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

class ReflectionBridgeTest extends ReferenceTest {
    Type type = JaxRsInterfaceOrSomething.class.getMethods()[0].getGenericReturnType();

    @Test
    void testFindDeserializer() throws Exception {
        ReflectionBridge reflectionBridge =
                new ReflectionBridge(List.of(SerdeUtil.impl(ScalarFieldsRecordSerde.class)));

        Optional<DeserializerDescription<JsonParser>> deserializerMaybe =
                reflectionBridge.findDeserializer(type, JsonParser.class);

        assertThat(deserializerMaybe).isNotEmpty();
        inputUtils.withJsonParser("{}", parser -> {
            assertThat(deserializerMaybe.get().invoke(parser)).isInstanceOf(ScalarFieldsRecord.class);
            return null;
        });
    }

    @Test
    void testFindSerializer() throws Exception {
        ReflectionBridge reflectionBridge =
                new ReflectionBridge(List.of(SerdeUtil.impl(ScalarFieldsRecordSerde.class)));

        Optional<SerializerDescription<JsonGenerator>> serializerMaybe =
                reflectionBridge.findSerializer(type, JsonGenerator.class);

        assertThat(serializerMaybe).isNotEmpty();
        assertThat(outputUtils.withJsonGenerator(generator -> {
                    ScalarFieldsRecord value = new ScalarFieldsRecord(
                            false,
                            (byte) 1,
                            (short) 2,
                            3,
                            4L,
                            'c',
                            4f,
                            5.0d,
                            false,
                            (byte) 1,
                            (short) 2,
                            3,
                            4L,
                            'c',
                            4f,
                            5.0d,
                            "six",
                            AnEnum.SOME_VALUE);
                    serializerMaybe.get().invoke(value, generator);
                }))
                .isNotEmpty();
    }

    //    @Test
    //    void testFindReturningSerializer() {
    //        ReflectionBridge reflectionBridge = new ReflectionBridge(List.of(new
    // org.tillerino.jagger.tests.alt.jsonnode.ScalarFieldsRecord$SerdeImpl()));
    //
    //        Type type = JaxRsInterfaceOrSomethingForJsonNode.class.getMethods()[0].getGenericReturnType();
    //        Optional<ReturningSerializerDescription<JsonNode>> serializerMaybe =
    // reflectionBridge.findReturningSerializer(type, JsonNode.class);
    //
    //        assertThat(serializerMaybe).isNotEmpty();
    //        org.tillerino.jagger.tests.alt.jsonnode.ScalarFieldsRecord value = new
    // org.tillerino.jagger.tests.alt.jsonnode.ScalarFieldsRecord(
    //                false, (byte) 1, (short) 2, 3, 4L, 'c', 4f, 5.0d, false, (byte) 1, (short) 2, 3, 4L, 'c', 4f,
    // 5.0d, "six", AnEnum.SOME_VALUE);
    //        assertThat(serializerMaybe.get().invoke(value)).isNotEmpty();
    //    }

    interface JaxRsInterfaceOrSomething {
        // we just need the return type
        ScalarFieldsRecord someRoute();
    }

    //    interface JaxRsInterfaceOrSomethingForJsonNode {
    //        // we just need the return type
    //        org.tillerino.jagger.tests.alt.jsonnode.ScalarFieldsRecord someRoute();
    //    }
}
