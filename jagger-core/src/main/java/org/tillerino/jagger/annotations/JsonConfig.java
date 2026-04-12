package org.tillerino.jagger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface JsonConfig {
    /**
     * Uses mappers from these classes (recursively). These are merged in order, i.e. later elements take precedence
     * when resolving conflicts.
     */
    Class<?>[] uses() default {};

    /**
     * Uses mappers and configuration from these classes. These are merged in order, i.e. later elements take precedence
     * when resolving conflicts.
     */
    Class<?>[] config() default {};

    /**
     * Add these annotations to the generated class.
     *
     * @return only annotations without non-default values can be used. E.g. {@code jakarta.inject.Inject}.
     */
    Class<?>[] onGeneratedClass() default {};

    /**
     * Add these annotations to the generated class.
     *
     * @return only annotations without non-default values can be used. E.g. {@code jakarta.inject.Inject}.
     */
    Class<?>[] onGeneratedConstructors() default {};

    /** Adds {@link Generated} to generated classes. */
    boolean addGeneratedAnnotationToClass() default true;

    /** Adds {@link Generated} to generated constructors and methods. */
    boolean addGeneratedAnnotationToMethods() default false;

    /**
     * Determines if the annotated element should be implemented.
     *
     * <p>By default, all methods are implemented.
     *
     * @return see {@link ImplementationMode}
     */
    ImplementationMode implement() default ImplementationMode.DEFAULT;

    /**
     * Determines if the annotated element can be called from other serializers.
     *
     * <p>By default, serializer implementations will always call suitable methods on other serializers that are
     * reachable through the {@link #uses()}.
     *
     * @return see {@link DelegateeMode}
     */
    DelegateeMode delegateTo() default DelegateeMode.DEFAULT;

    /**
     * Determines how unknown properties are treated.
     *
     * @return see {@link UnknownPropertiesMode}
     */
    UnknownPropertiesMode unknownProperties() default UnknownPropertiesMode.DEFAULT;

    VerificationMode verifySymmetry() default VerificationMode.NO_VERIFICATION;

    enum DelegateeMode {
        /** The annotated element can be called from other serializers. */
        DELEGATE_TO,
        /** The annotated element cannot be called from other serializers. */
        DO_NOT_DELEGATE_TO,
        /** Defaults to {@link #DELEGATE_TO} */
        DEFAULT,
        ;

        public boolean canBeDelegatedTo() {
            return this != DO_NOT_DELEGATE_TO;
        }
    }

    enum ImplementationMode {
        DO_IMPLEMENT,
        DO_NOT_IMPLEMENT,
        /** Defaults to {@link #DO_IMPLEMENT} */
        DEFAULT,
        ;

        public boolean shouldImplement() {
            return this != DO_NOT_IMPLEMENT;
        }
    }

    enum UnknownPropertiesMode {
        THROW,
        IGNORE,
        /** Defaults to {@link #THROW} */
        DEFAULT,
        ;
    }

    enum VerificationMode {
        NO_VERIFICATION,

        /** Log warnings, but do not fail compilation. */
        WARN,

        /** Fail compilation if an verification fails. */
        FAIL;
    }
}
