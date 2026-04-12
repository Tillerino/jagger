package org.tillerino.jagger.tests.base.features;

import static org.tillerino.jagger.tests.CodeAssertions.assertThatImpl;

import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.CodeAssertions.CompileUnitAssert;
import org.tillerino.jagger.tests.ReferenceTest;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.DefaultBehaviourOnClass;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.DefaultBehaviourOnInterface;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.ReverseGeneratedAnnotationOnClass;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.ReverseGeneratedAnnotationOnInterface;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.SpecialCaseInterfaceWithAnnotationOnConstructorAndGeneratedOnMethods;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.WithAnnotationsOnClass;
import org.tillerino.jagger.tests.base.features.CodeGenerationSerde.WithAnnotationsOnInterface;

class CodeGenerationTest extends ReferenceTest {
    @Test
    void defaultBehaviourOnInterface() throws Exception {
        CompileUnitAssert impl = assertThatImpl(DefaultBehaviourOnInterface.class);
        impl.hasAnnotation("@Generated");
        impl.hasNoConstructor();
        impl.method("write").hasAnnotation("@Override").doesNotHaveAnnotation("@Generated");
    }

    @Test
    void defaultBehaviourOnClassHasGenerated() throws Exception {
        CompileUnitAssert impl = assertThatImpl(DefaultBehaviourOnClass.class);
        impl.hasAnnotation("@Generated");
        impl.singleConstructor().doesNotHaveAnnotation("@Generated");
        impl.method("write").hasAnnotation("@Override").doesNotHaveAnnotation("@Generated");
    }

    @Test
    void withAnnotationsOnInterface() throws Exception {
        CompileUnitAssert impl = assertThatImpl(WithAnnotationsOnInterface.class);
        impl.hasAnnotation("@AnAnnotation").hasAnnotation("@Generated");
        impl.singleConstructor().hasAnnotation("@AnotherAnnotation").doesNotHaveAnnotation("@Generated");
        impl.method("write").hasAnnotation("@Override").doesNotHaveAnnotation("@Generated");
    }

    @Test
    void withAnnotationsOnClass() throws Exception {
        CompileUnitAssert impl = assertThatImpl(WithAnnotationsOnClass.class);
        impl.hasAnnotation("@AnAnnotation").hasAnnotation("@Generated");
        impl.singleConstructor().hasAnnotation("@AnotherAnnotation").doesNotHaveAnnotation("@Generated");
        impl.method("write").hasAnnotation("@Override").doesNotHaveAnnotation("@Generated");
    }

    @Test
    void reverseDefaultOnInterface() throws Exception {
        CompileUnitAssert impl = assertThatImpl(ReverseGeneratedAnnotationOnInterface.class);
        impl.doesNotHaveAnnotation("@Generated");
        impl.hasNoConstructor();
        impl.method("write").hasAnnotation("@Override").hasAnnotation("@Generated");
    }

    @Test
    void reverseNoGeneratedOnClass() throws Exception {
        CompileUnitAssert impl = assertThatImpl(ReverseGeneratedAnnotationOnClass.class);
        impl.doesNotHaveAnnotation("@Generated");
        impl.singleConstructor().hasAnnotation("@Generated");
        impl.method("write").hasAnnotation("@Override").hasAnnotation("@Generated");
    }

    @Test
    void specialCaseConstructorHasBothAnnotations() throws Exception {
        CompileUnitAssert impl =
                assertThatImpl(SpecialCaseInterfaceWithAnnotationOnConstructorAndGeneratedOnMethods.class);
        impl.hasAnnotation("@Generated");
        impl.singleConstructor().hasAnnotation("@AnotherAnnotation").hasAnnotation("@Generated");
        impl.method("write").hasAnnotation("@Override").hasAnnotation("@Generated");
    }
}
