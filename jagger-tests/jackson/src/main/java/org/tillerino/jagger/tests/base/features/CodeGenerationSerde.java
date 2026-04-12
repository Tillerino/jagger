package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import lombok.RequiredArgsConstructor;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.AnAnnotation;
import org.tillerino.jagger.tests.model.AnotherAnnotation;
import org.tillerino.jagger.tests.model.NoFieldsRecord;

public interface CodeGenerationSerde {
    interface DefaultBehaviourOnInterface {
        @JsonOutput
        void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    abstract class DefaultBehaviourOnClass {
        final ClassLoader cl;

        protected DefaultBehaviourOnClass(ClassLoader cl) {
            this.cl = cl;
        }

        @JsonOutput
        abstract void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    @JsonConfig(onGeneratedClass = AnAnnotation.class, onGeneratedConstructors = AnotherAnnotation.class)
    interface WithAnnotationsOnInterface {
        @JsonOutput
        void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    @JsonConfig(onGeneratedClass = AnAnnotation.class, onGeneratedConstructors = AnotherAnnotation.class)
    abstract class WithAnnotationsOnClass {
        final ClassLoader cl;

        protected WithAnnotationsOnClass(ClassLoader cl) {
            this.cl = cl;
        }

        @JsonOutput
        abstract void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    @JsonConfig(addGeneratedAnnotationToClass = false, addGeneratedAnnotationToMethods = true)
    interface ReverseGeneratedAnnotationOnInterface {
        @JsonOutput
        void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    @JsonConfig(addGeneratedAnnotationToClass = false, addGeneratedAnnotationToMethods = true)
    @RequiredArgsConstructor
    abstract class ReverseGeneratedAnnotationOnClass {
        final ClassLoader cl;

        @JsonOutput
        abstract void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }

    @JsonConfig(onGeneratedConstructors = AnotherAnnotation.class, addGeneratedAnnotationToMethods = true)
    interface SpecialCaseInterfaceWithAnnotationOnConstructorAndGeneratedOnMethods {
        @JsonOutput
        void write(NoFieldsRecord model, JsonGenerator out) throws Exception;
    }
}
