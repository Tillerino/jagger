package org.tillerino.jagger.processor;

import com.squareup.javapoet.*;
import com.squareup.javapoet.FieldSpec.Builder;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;
import org.tillerino.jagger.processor.config.AnyConfig;
import org.tillerino.jagger.processor.features.Enums.EnumValuesField;
import org.tillerino.jagger.processor.features.Verification.ForBlueprint;
import org.tillerino.jagger.processor.util.FullyQualifiedName.FullyQualifiedClassName.TopLevelClassName;
import org.tillerino.jagger.processor.util.PlainTypeName;
import org.tillerino.jagger.processor.util.Snippet;

/** Keeps track of the delegate readers/writers that are collected while processing a blueprint. */
public class GeneratedClass {
    Map<String, DelegateeField> delegateeFields = new LinkedHashMap<>();
    Map<String, EnumValuesField> enumFields = new LinkedHashMap<>();
    Map<String, RequiredField> requiredFields = new LinkedHashMap<>();
    public final TypeSpec.Builder typeBuilder;
    public final List<Consumer<JavaFile.Builder>> fileBuilderMods = new ArrayList<>();
    private final JaggerContext ctx;
    public final JaggerBlueprint blueprint;
    public final ForBlueprint verificationForBlueprint;
    public boolean hasNoArgConstructor = true;

    public GeneratedClass(TypeSpec.Builder typeBuilder, JaggerContext ctx, JaggerBlueprint blueprint) {
        this.typeBuilder = typeBuilder;
        this.ctx = ctx;
        this.blueprint = blueprint;
        this.verificationForBlueprint = ctx.verification.startBlueprint(blueprint);
    }

    /**
     * Returns the field name for the given blueprint. If it does not exist yet, it is created.
     *
     * @param caller the blueprint which is currently being processed
     * @param callee the blueprint which is being called from caller
     * @return the field name
     */
    public Snippet getOrCreateDelegateeField(JaggerBlueprint caller, JaggerBlueprint callee, boolean implAsType) {
        if (caller == callee) {
            return Snippet.of("this");
        }
        String fieldName =
                StringUtils.uncapitalize(callee.className.className()) + "$" + delegateeFields.size() + "$delegate";
        DelegateeField delegatee =
                new DelegateeField(fieldName, callee, implAsType, new PotentialProviderCall(fieldName));
        return delegateeFields.merge(
                        callee.className.importName(),
                        delegatee,
                        (x, y) -> x.implAsType ? x : new DelegateeField(x.name, x.blueprint, y.implAsType, x.access))
                .access;
    }

    public Snippet getOrCreateUsedBlueprintWithTypeField(TypeMirror targetType, AnyConfig config) {
        return getOrCreateUsedBlueprintWithTypeField(targetType, blueprint, config);
    }

    private Snippet getOrCreateUsedBlueprintWithTypeField(
            TypeMirror targetType, JaggerBlueprint calleeBlueprint, @Nullable AnyConfig config) {
        if (ctx.commonTypes.isAssignable(calleeBlueprint.typeElement.asType(), targetType)) {
            return getOrCreateDelegateeField(this.blueprint, calleeBlueprint, false); // TODO probably wrong
        }
        if (config == null) {
            return null;
        }
        for (JaggerBlueprint use : config.reversedUses()) {
            Snippet found = getOrCreateUsedBlueprintWithTypeField(targetType, use, null);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public EnumValuesField getOrCreateEnumField(TypeMirror enumType) {
        return enumFields.computeIfAbsent(
                enumType.toString(), __ -> ctx.enums.createEnumField(enumType, enumFields.size()));
    }

    public void buildFields(Map<JaggerBlueprint, GeneratedClass> others) {
        delegateeFields.values().forEach(value -> value.writeField(typeBuilder, others));
        enumFields
                .values()
                .forEach(value -> value.createFields()
                        .forEach(f ->
                                typeBuilder.addField(f.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                        .build())));
        requiredFields.values().forEach(value -> value.writeField(typeBuilder));
    }

    public String requiredField(TypeMirror type) {
        String name = StringUtils.uncapitalize(PlainTypeName.of(type));
        requiredFields.computeIfAbsent(name, __ -> new RequiredField(name, type));
        return name;
    }

    /**
     * @param others non-null if this should recurse. Each recursive call sets this to null, so recursion is at most one
     *     deep.
     * @return true if anything has changed -> to stop loop
     */
    public boolean propagateRequiresArgsForConstructor(Map<JaggerBlueprint, GeneratedClass> others) {
        if (!hasNoArgConstructor) {
            return false;
        }
        if (!requiredFields.isEmpty() || !blueprint.hasNoArgSuperConstructor()) {
            hasNoArgConstructor = false;
            return true;
        }
        for (DelegateeField delegateeField : delegateeFields.values()) {
            if (delegateeField.access.providerCall) {
                hasNoArgConstructor = false;
                return true;
            }
            GeneratedClass delegate = others.get(delegateeField.blueprint);
            // this explicitly does not recurse, see comment at call site
            if (delegate != null && !delegate.hasNoArgConstructor) {
                hasNoArgConstructor = false;
                return true;
            }
        }
        return false;
    }

    public Stream<FieldSpec> uninitializedFields() {
        return typeBuilder.fieldSpecs.stream().filter(f -> f.hasModifier(Modifier.FINAL) && f.initializer.isEmpty());
    }

    public void writeFile(Filer filer) throws IOException {
        JavaFileObject sourceFile = filer.createSourceFile(blueprint.generatedClassName());
        try (Writer writer = sourceFile.openWriter()) {
            JavaFile.Builder builder = JavaFile.builder(blueprint.className.packageName(), typeBuilder.build());
            fileBuilderMods.forEach(mod -> mod.accept(builder));
            JavaFile file = builder.build();
            file.writeTo(writer);
        }
    }

    @Override
    public String toString() {
        return blueprint.toString();
    }

    record DelegateeField(String name, JaggerBlueprint blueprint, boolean implAsType, PotentialProviderCall access) {
        private void writeField(TypeSpec.Builder classBuilder, Map<JaggerBlueprint, GeneratedClass> others) {
            TopLevelClassName impl = this.blueprint().className.impl();
            ClassName implName = ClassName.get(impl.packageName(), impl.className());
            TypeName fieldType = implAsType
                    ? implName
                    : TypeName.get(this.blueprint().typeElement.asType());
            if (access.providerCall) {
                fieldType = blueprint.ctx.codeGeneration.providerType(fieldType, blueprint.config);
            }
            Builder builder = FieldSpec.builder(fieldType, this.name()).addModifiers(Modifier.FINAL);

            GeneratedClass other = others.get(blueprint);
            if (other.hasNoArgConstructor) {
                builder.initializer("new $T()", implName);
            }

            classBuilder.addField(builder.build());
        }
    }

    public record RequiredField(String name, TypeMirror type) {
        private void writeField(TypeSpec.Builder classBuilder) {
            FieldSpec.Builder field = FieldSpec.builder(TypeName.get(type), name, Modifier.PRIVATE, Modifier.FINAL);
            classBuilder.addField(field.build());
        }
    }

    public void breakCircle(
            Set<JaggerBlueprint> mark, JaggerBlueprint start, Map<JaggerBlueprint, GeneratedClass> all) {
        if (!mark.add(blueprint)) {
            return;
        }
        for (DelegateeField field : delegateeFields.values()) {
            if (field.access.providerCall) {
                continue;
            }
            if (field.blueprint == start) {
                field.access.providerCall = true;
                field.access.literal = ctx.codeGeneration.callProvider(field.access.literal, blueprint.config);
            } else {
                GeneratedClass generatedClass = all.get(field.blueprint);
                if (generatedClass != null) {
                    generatedClass.breakCircle(mark, start, all);
                }
            }
        }
    }

    /**
     * This mutable class allows us to swap out a direct field access for a provider call to access a delegatee. We do
     * not want to go through method generation twice, so we generate methods with these placeholders. Once all methods
     * have been generated, we look for circles and break the circles with provider calls.
     */
    public class PotentialProviderCall implements Snippet {
        String literal;
        boolean providerCall;

        public PotentialProviderCall(String literal) {
            this.literal = literal;
        }

        @Override
        public String toString() {
            return literal;
        }

        @Override
        public Flattened flatten() {
            return new Flattened("$L", new Object[] {this});
        }
    }
}
