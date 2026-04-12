# Code Generation

Generated classes always append `Impl` to the name of the implemented interface or extended class.
If the parent is nested inside another type, a dollar sign is used as a separator.
- `some.package.Blueprint` -> `some.package.BlueprintImpl`.
- `some.package.Wrapper.Blueprint` -> `some.package.Wrapper$BlueprintImpl`.

## Constructors

When a generated class extends a class with any number of constructors, matching constructors are generated.

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/CodeGenerationSerde.java#L17-L22

abstract class DefaultBehaviourOnClass {
    final ClassLoader cl;

    protected DefaultBehaviourOnClass(ClassLoader cl) {
        this.cl = cl;
    }
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/CodeGenerationSerde$DefaultBehaviourOnClassImpl.java#L11-L14

public class CodeGenerationSerde$DefaultBehaviourOnClassImpl extends CodeGenerationSerde.DefaultBehaviourOnClass {
  CodeGenerationSerde$DefaultBehaviourOnClassImpl(ClassLoader cl) {
    super(cl);
  }
```

## `@Generated` annotation

By default, generated classes are annotated with `@org.tillerino.jagger.annotations.Generated`:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/CodeGenerationSerde$DefaultBehaviourOnInterfaceImpl.java#L9-L12

@Generated
public class CodeGenerationSerde$DefaultBehaviourOnInterfaceImpl implements CodeGenerationSerde.DefaultBehaviourOnInterface {
  @Override
  public void write(NoFieldsRecord model, JsonGenerator out) throws Exception {
```

Methods and constructors are not annotated by default.
Both the class and method/constructor behaviour can be configured with `@JsonConfig`:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/CodeGenerationSerde.java#L46-L47

@JsonConfig(addGeneratedAnnotationToClass = false, addGeneratedAnnotationToMethods = true)
interface ReverseGeneratedAnnotationOnInterface {
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/CodeGenerationSerde$ReverseGeneratedAnnotationOnInterfaceImpl.java#L9-L12

public class CodeGenerationSerde$ReverseGeneratedAnnotationOnInterfaceImpl implements CodeGenerationSerde.ReverseGeneratedAnnotationOnInterface {
  @Override
  @Generated
  public void write(NoFieldsRecord model, JsonGenerator out) throws Exception {
```

## Custom annotations

You might want to add custom annotations to your generated classes, e.g. for injection.
Use `onGeneratedClass` and `onGeneratedConstructors` in `@JsonConfig` to specify custom annotations to be added:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/CodeGenerationSerde.java#L34-L40

@JsonConfig(onGeneratedClass = AnAnnotation.class, onGeneratedConstructors = AnotherAnnotation.class)
abstract class WithAnnotationsOnClass {
    final ClassLoader cl;

    protected WithAnnotationsOnClass(ClassLoader cl) {
        this.cl = cl;
    }
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/CodeGenerationSerde$WithAnnotationsOnClassImpl.java#L12-L18

@Generated
@AnAnnotation
public class CodeGenerationSerde$WithAnnotationsOnClassImpl extends CodeGenerationSerde.WithAnnotationsOnClass {
  @AnotherAnnotation
  CodeGenerationSerde$WithAnnotationsOnClassImpl(ClassLoader cl) {
    super(cl);
  }
```

When specifying `onGeneratedConstructors` on an interface with no explicit constructor, a constructor is forced:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/CodeGenerationSerde.java#L28-L29

@JsonConfig(onGeneratedClass = AnAnnotation.class, onGeneratedConstructors = AnotherAnnotation.class)
interface WithAnnotationsOnInterface {
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/CodeGenerationSerde$WithAnnotationsOnInterfaceImpl.java#L11-L16

@Generated
@AnAnnotation
public class CodeGenerationSerde$WithAnnotationsOnInterfaceImpl implements CodeGenerationSerde.WithAnnotationsOnInterface {
  @AnotherAnnotation
  CodeGenerationSerde$WithAnnotationsOnInterfaceImpl() {
  }
```

For now, only annotations without non-default properties are supported.