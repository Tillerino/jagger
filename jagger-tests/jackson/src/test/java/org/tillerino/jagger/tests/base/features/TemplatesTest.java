package org.tillerino.jagger.tests.base.features;

import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.CodeAssertions.CompileUnitAssert;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.base.features.TemplatesSerde.*;

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
}
