package org.tillerino.jagger.processor.ext;

import static java.util.Arrays.asList;

import com.squareup.javapoet.CodeBlock;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;
import org.tillerino.jagger.processor.GeneratedClass;
import org.tillerino.jagger.processor.JaggerContext;
import org.tillerino.jagger.processor.JaggerPrototype;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Generics.TypeVar;
import org.tillerino.jagger.processor.util.Exceptions;
import org.tillerino.jagger.processor.util.InstantiatedMethod;
import org.tillerino.jagger.processor.util.InstantiatedMethod.InstantiatedVariable;

/**
 * A light abstraction for code that can be generated.
 *
 * <p>Instances are detected by {@link PrototypeDetector}, turned into {@link JaggerPrototype}. Eventually, code will be
 * generated via {@link #generateCode(CodeGeneratorContext)}.
 *
 * <p>Code that is suitable for delegation or templating should implement {@link TemplatablePrototypeKind}.
 */
public interface PrototypeKind {
    /**
     * Generates the code for the detected prototype.
     *
     * @param context see {@link CodeGeneratorContext}
     * @return the body of the generated method
     */
    CodeBlock.Builder generateCode(CodeGeneratorContext context);

    /**
     * By default, code is only generated for unimplemented methods. If you want to generate decorators, use this
     *
     * @param config the resolved configuration for this prototype
     * @return true if code is generated even if there is a suitable super implementation
     */
    default boolean decorates(AnyConfig config) {
        return false;
    }

    static Optional<PrototypeKind> detect(
            List<TypeMirror> externalTypes, InstantiatedMethod m, JaggerContext ctx, PrototypeKindInstantiator c) {
        for (TypeMirror externalType : externalTypes) {
            if (externalType == null) {
                continue;
            }
            externalType = ctx.types.erasure(externalType);
            if (variablesContain(m.parameters(), externalType, ctx)) {
                InstantiatedVariable externalParameter = variableOfType(m.parameters(), externalType, ctx);
                List<InstantiatedVariable> otherParameters = variablesExcept(m.parameters(), externalType, ctx);
                return Optional.of(c.instantiate(externalType, externalParameter, otherParameters));
            }
        }
        return Optional.empty();
    }

    static boolean variablesContain(
            List<InstantiatedVariable> variables, TypeMirror fullyQualified, JaggerContext ctx) {
        for (InstantiatedVariable variable : variables) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return true;
            }
        }
        return false;
    }

    static InstantiatedVariable variableOfType(
            List<InstantiatedVariable> variables, TypeMirror fullyQualified, JaggerContext ctx) {
        for (InstantiatedVariable variable : variables) {
            if (ctx.types.isAssignable(variable.type(), fullyQualified)) {
                return variable;
            }
        }
        throw Exceptions.unexpected();
    }

    static List<InstantiatedVariable> variablesExcept(
            List<InstantiatedVariable> variables, TypeMirror excludeFullyQualified, JaggerContext ctx) {
        return variables.stream()
                .filter(v -> !ctx.types.isAssignable(v.type(), excludeFullyQualified))
                .toList();
    }

    static List<TypeMirror> nullableTypeList(TypeMirror... nullableTypes) {
        return asList(nullableTypes);
    }

    interface TemplatablePrototypeKind extends PrototypeKind {
        /**
         * Optional. A Specialization that can be used in addition to the own class and {@link #types()} to match
         * prototypes. For example READ vs WRITE.
         *
         * @return compared via null-safe equals. If your prototype has no specialization, null is fine.
         */
        default Object specialization() {
            return null;
        }

        List<TypeMirror> types();

        /**
         * Used to generate method names for templated prototypes.
         *
         * @return a unique name within a blueprint
         */
        String defaultMethodName();

        /** See {@link #withTypesPrefix(List)} */
        TemplatablePrototypeKind withTypes(List<TypeMirror> newTypes);

        default boolean matches(
                TemplatablePrototypeKind other,
                JaggerContext ctx,
                Map<TypeVar, TypeMirror> typeBindings,
                Set<TypeVar> freeTypeVariables) {
            if (!Objects.equals(specialization(), other.specialization()) || getClass() != other.getClass()) {
                return false;
            }
            for (int i = 0; i < types().size(); i++) {
                if (!ctx.generics.typeBindingsSatisfyingEquality(
                        other.types().get(i), types().get(i), typeBindings, freeTypeVariables)) {
                    return false;
                }
            }
            return true;
        }

        /** This is used to find delegates or instantiate templates. */
        default TemplatablePrototypeKind withTypesPrefix(List<TypeMirror> replacement) {
            return withTypes(
                    Stream.concat(replacement.stream(), types().stream().skip(replacement.size()))
                            .toList());
        }
    }

    interface PrototypeKindInstantiator {
        PrototypeKind instantiate(
                TypeMirror externalBaseKind,
                InstantiatedVariable externalParameter,
                List<InstantiatedVariable> otherParameters);
    }

    /**
     * Everything that is available to the generated code.
     *
     * @param ctx The overall context of the annotation processor. This contains utility classes for generics,
     *     delegation, and many more.
     * @param prototype The method that is being generated.
     * @param generatedClass The class that will contain the generated code.
     */
    record CodeGeneratorContext(JaggerContext ctx, JaggerPrototype prototype, GeneratedClass generatedClass) {}
}
