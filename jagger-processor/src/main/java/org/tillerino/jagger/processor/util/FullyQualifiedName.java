package org.tillerino.jagger.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

public sealed interface FullyQualifiedName {
    /** slashes instead of dots, no file extension */
    String fileName();

    /** as can be used in an import statement */
    String importName();

    /** for the package declaration at the start of a source file */
    String packageName();

    record PackageName(String name) implements FullyQualifiedName {
        static PackageName of(PackageElement packageElement) {
            return new PackageName(packageElement.getQualifiedName().toString());
        }

        @Override
        public String fileName() {
            return name.replace('.', '/');
        }

        @Override
        public String importName() {
            return name;
        }

        @Override
        public String packageName() {
            return name;
        }
    }

    sealed interface FullyQualifiedClassName extends FullyQualifiedName {
        String className();

        String nameInCompilationUnit();

        TopLevelClassName unnest();

        default TopLevelClassName impl() {
            TopLevelClassName unnested = unnest();
            return new TopLevelClassName(unnested.parentPackage, unnested.className + "Impl");
        }

        static FullyQualifiedClassName of(TypeElement typeElement) {
            Element enclosingElement = typeElement.getEnclosingElement();
            if (enclosingElement instanceof PackageElement packageElement) {
                return new FullyQualifiedClassName.TopLevelClassName(
                        PackageName.of(packageElement),
                        typeElement.getSimpleName().toString());
            }
            if (enclosingElement instanceof TypeElement parentType) {
                return new FullyQualifiedClassName.NestedClassName(
                        FullyQualifiedClassName.of(parentType),
                        typeElement.getSimpleName().toString());
            }
            throw new ContextedRuntimeException(typeElement.toString());
        }

        record TopLevelClassName(PackageName parentPackage, String className) implements FullyQualifiedClassName {
            @Override
            public String fileName() {
                return parentPackage.fileName() + "/" + className;
            }

            @Override
            public String importName() {
                return parentPackage.importName() + "." + className;
            }

            @Override
            public String packageName() {
                return parentPackage.packageName();
            }

            @Override
            public String nameInCompilationUnit() {
                return className;
            }

            @Override
            public TopLevelClassName unnest() {
                return this;
            }
        }

        record NestedClassName(FullyQualifiedClassName parentClass, String className)
                implements FullyQualifiedClassName {
            @Override
            public String fileName() {
                return parentClass.fileName() + "$" + className;
            }

            @Override
            public String importName() {
                return parentClass.importName() + "." + className;
            }

            @Override
            public String packageName() {
                return parentClass.packageName();
            }

            @Override
            public String nameInCompilationUnit() {
                return parentClass.nameInCompilationUnit() + "$" + className;
            }

            @Override
            public TopLevelClassName unnest() {
                TopLevelClassName parentUnnested = parentClass.unnest();
                return new TopLevelClassName(
                        parentUnnested.parentPackage(), parentUnnested.className() + "$" + className);
            }
        }
    }
}
