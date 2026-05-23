package org.tillerino.jagger.tests.base.features;

import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.CodeAssertions.CompileUnitAssert;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.features.TemplatesSerde.CallsTemplatePrototypes;
import org.tillerino.jagger.tests.base.features.TemplatesSerde.MultipleTemplateAnnotationsAndOneCustom;
import org.tillerino.jagger.tests.base.features.TemplatesSerde.TemplatedSerde;
import org.tillerino.jagger.tests.model.AnEnum;
import org.tillerino.jagger.tests.model.ScalarFieldsRecord;

class TemplatesTest extends ReferenceTest {
    TemplatedSerde templatedSerde = SerdeUtil.impl(TemplatedSerde.class);

    @Test
    void templatesGenerateMethods() throws Exception {
        CompileUnitAssert assertThat = assertThatImpl(TemplatedSerde.class);
        assertThat.method("readPrimitiveDouble");
        assertThat.method("writePrimitiveDouble");
        assertThat.method("readAnEnum");
        assertThat.method("writeAnEnum");
        assertThat.method("readArrayOfPrimitiveDouble");
        assertThat.method("writeArrayOfPrimitiveDouble");
        assertThat.method("readArrayOfAnEnum").calls("readAnEnum");
        assertThat.method("writeArrayOfAnEnum").calls("writeGenericArray").references("writeAnEnum");
    }

    @Test
    void canDelegateToAndReferenceTemplatedPrototypes() throws Exception {
        CompileUnitAssert assertThat = assertThatImpl(CallsTemplatePrototypes.class);
        assertThat.methods().hasSize(2); // nothing was added here that was declared on the referenced blueprint
        assertThat
                .method("readHasAnEnumArrayProperty")
                .calls("readArrayOfAnEnum")
                .calls("readArrayOfPrimitiveDouble");
        // AnEnum[] is written with the generic method instead of the template because the generic method is first.
        // Maybe pick the most specific option at some point?
        assertThat
                .method("writeHasAnEnumArrayProperty")
                .calls("writeGenericArray")
                .references("writeArrayOfPrimitiveDouble")
                .references("writeAnEnum");
    }

    @Test
    void multipleTemplateAnnotationsArePickedUp() throws Exception {
        CompileUnitAssert assertThat = assertThatImpl(MultipleTemplateAnnotationsAndOneCustom.class);
        assertThat
                .methods()
                .extracting(NodeWithSimpleName::getNameAsString)
                .containsExactly("writeAnEnumList", "writeAnEnum", "readAnEnum");
    }

    @Nested
    class AutoTemplatesSerde {
        @Test
        void autoTemplateGeneratesMethodsOnDemand() throws Exception {
            CompileUnitAssert assertThat = assertThatImpl(TemplatesSerde.AutoTemplatesSerde.class);
            assertThat.method("readScalarFieldsRecord").calls("readString");
            assertThat.method("writeScalarFieldsRecord").calls("writeString");
        }

        @Test
        void roundTrip() throws Exception {
            TemplatesSerde$AutoTemplatesSerdeImpl serde = new TemplatesSerde$AutoTemplatesSerdeImpl();
            outputUtils.roundTrip(
                    new ScalarFieldsRecord(
                            true,
                            (byte) 1,
                            (short) 2,
                            3,
                            4L,
                            'c',
                            1.0f,
                            2.0d,
                            true,
                            (byte) 1,
                            (short) 2,
                            3,
                            4L,
                            'c',
                            1.0f,
                            2.0d,
                            "test",
                            AnEnum.ANOTHER_VALUE),
                    serde::writeScalarFieldsRecord,
                    serde::readScalarFieldsRecord,
                    new TypeReference<ScalarFieldsRecord>() {});
        }

        @Test
        void allDelegatorsAreCalled() throws Exception {
            CompileUnitAssert assertThat = assertThatImpl(TemplatesSerde.AutoTemplatesSerde.class);
            assertThat
                    .method("writeScalarFieldsRecord")
                    .calls("writeBoolean")
                    .calls("writeByte")
                    .calls("writeShort")
                    .calls("writeInteger")
                    .calls("writeLong")
                    .calls("writeCharacter")
                    .calls("writeFloat")
                    .calls("writeDouble")
                    .calls("writeString")
                    .calls("writePrimitiveBoolean")
                    .calls("writePrimitiveByte")
                    .calls("writePrimitiveShort")
                    .calls("writePrimitiveInt")
                    .calls("writePrimitiveLong")
                    .calls("writePrimitiveChar")
                    .calls("writePrimitiveFloat")
                    .calls("writePrimitiveDouble")
                    .calls("writeAnEnum");
            assertThat
                    .method("readScalarFieldsRecord")
                    .calls("readBoolean")
                    .calls("readByte")
                    .calls("readShort")
                    .calls("readInteger")
                    .calls("readLong")
                    .calls("readCharacter")
                    .calls("readFloat")
                    .calls("readDouble")
                    .calls("readString")
                    .calls("readPrimitiveBoolean")
                    .calls("readPrimitiveByte")
                    .calls("readPrimitiveShort")
                    .calls("readPrimitiveInt")
                    .calls("readPrimitiveLong")
                    .calls("readPrimitiveChar")
                    .calls("readPrimitiveFloat")
                    .calls("readPrimitiveDouble")
                    .calls("readAnEnum");
        }
    }
}
