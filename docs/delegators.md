# Delegators

<!-- toc -->

- [Basics](#basics)
- [Delegate to other classes](#delegate-to-other-classes)
- [Build a library](#build-a-library)
- [Recursive Types](#recursive-types)
- [Additional arguments](#additional-arguments)

<!-- tocstop -->

## Basics

Jagger will delegate to other suitable `@JsonInput` and `@JsonOutput` methods whenever possible.
This is very important for keeping the generated code small.
Take the following example:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/DelegationSerde.java#L24-L28

@JsonInput
ScalarFieldsRecord deserializeSingle(JsonParser parser) throws Exception;

@JsonInput
List<ScalarFieldsRecord> deserializeList(JsonParser parser) throws Exception;
```

`deserializeList` will then refer to `deserializeSingle` instead of repeating the entire deserialization of `ScalarFieldsRecord`.
```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/DelegationSerde$SimpleDelegationSerdeImpl.java#L326-L343


@Override
public List<ScalarFieldsRecord> deserializeList(JsonParser parser) throws Exception {
  if (!parser.hasCurrentToken()) {
    parser.nextToken();
  }
  if (nextIfCurrentTokenIs(parser, VALUE_NULL)) {
    return null;
  } else if (nextIfCurrentTokenIs(parser, START_ARRAY)) {
    List<ScalarFieldsRecord> container = new ArrayList<>();
    while (!nextIfCurrentTokenIs(parser, END_ARRAY)) {
      container.add(this.deserializeSingle(parser));
    }
    return container;
  } else {
    throw new IOException("Expected array, got " + parser.currentToken() + " at " + parser.getCurrentLocation());
  }
}
```

## Delegate to other classes

To organize your methods, you can use the `uses` attribute of the `@JsonConfig` annotation:
```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/PrimitiveScalarsSerde.java#L8-L10

public interface PrimitiveScalarsSerde {
    @JsonOutput
    void writePrimitiveBooleanX(boolean b, JsonGenerator generator) throws Exception;
```

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/DelegationSerde.java#L62-L65

@JsonConfig(uses = PrimitiveScalarsSerde.class)
interface BoxedScalarsSerde {
    @JsonOutput
    void writeBoxedBooleanX(Boolean b, JsonGenerator generator) throws Exception;
```

The implementation of `BoxedScalarsSerde` will then call instantiate and call the first serializer:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/DelegationSerde$BoxedScalarsSerdeImpl.java#L23-L33

public class DelegationSerde$BoxedScalarsSerdeImpl implements DelegationSerde.BoxedScalarsSerde {
  PrimitiveScalarsSerde primitiveScalarsSerde$0$delegate = new PrimitiveScalarsSerdeImpl();

  @Override
  public void writeBoxedBooleanX(Boolean b, JsonGenerator generator) throws Exception {
    if (b != null) {
      primitiveScalarsSerde$0$delegate.writePrimitiveBooleanX(b, generator);
    } else {
      generator.writeNull();
    }
  }
```

## Build a library

To keep the generated code small and readable, it is recommended to build a library of reusable serializers down to primitives.
Even something as simple as reading a `Float[]` generates a lot of code:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/ScalarArraysSerdeImpl.java#L705-L747

@Override
public Float[] readBoxedFloatArray(JsonParser parser) throws Exception {
  if (!parser.hasCurrentToken()) {
    parser.nextToken();
  }
  if (nextIfCurrentTokenIs(parser, VALUE_NULL)) {
    return null;
  } else if (nextIfCurrentTokenIs(parser, START_ARRAY)) {
    // Like ArrayList
    Object[] array = EmptyArrays.EMPTY_OBJECT_ARRAY;
    int len = 0;
    while (!nextIfCurrentTokenIs(parser, END_ARRAY)) {
      if (len == array.length) {
        // simplified version of ArrayList growth
        array = Arrays.copyOf(array, Math.max(10, array.length + (array.length >> 1)));
      }
      if (nextIfCurrentTokenIs(parser, VALUE_NULL)) {
        array[len++] = null;
      } else if (parser.currentToken() == VALUE_STRING) {
        String string;
        string = parser.getText();
        parser.nextToken();
        if (string.equals("NaN")) {
          array[len++] = Float.NaN;
        } else if (string.equals("Infinity")) {
          array[len++] = Float.POSITIVE_INFINITY;
        } else if (string.equals("-Infinity")) {
          array[len++] = Float.NEGATIVE_INFINITY;
        } else {
          throw new IllegalArgumentException();
        }
      } else if (parser.currentToken().isNumeric()) {
        array[len++] = parser.getFloatValue();
        parser.nextToken();
      } else {
        throw new IOException("Expected number, got " + parser.currentToken() + " at " + parser.getCurrentLocation());
      }
    }
    return Arrays.copyOf(array, len, Float[].class);
  } else {
    throw new IOException("Expected array, got " + parser.currentToken() + " at " + parser.getCurrentLocation());
  }
}
```

It is recommended to build a library of central serializers for primitives, boxed primitives, other scalars like `String`,
common arrays, e.g. `double[]`, etc. This is how you would go about that:

First, define primitive serializers:
```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/PrimitiveScalarsSerde.java#L8-L19

public interface PrimitiveScalarsSerde {
    @JsonOutput
    void writePrimitiveBooleanX(boolean b, JsonGenerator generator) throws Exception;

    @JsonInput
    boolean readPrimitiveBooleanX(JsonParser parser) throws Exception;

    @JsonOutput
    void writePrimitiveByteX(byte b, JsonGenerator generator) throws Exception;

    @JsonInput
    byte readPrimitiveByteX(JsonParser parser) throws Exception;
```
(and so on)

Then define boxed serializers that reuse the primitive serializers:
```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/DelegationSerde.java#L62-L74

@JsonConfig(uses = PrimitiveScalarsSerde.class)
interface BoxedScalarsSerde {
    @JsonOutput
    void writeBoxedBooleanX(Boolean b, JsonGenerator generator) throws Exception;

    @JsonInput
    Boolean readBoxedBooleanX(JsonParser parser) throws Exception;

    @JsonOutput
    void writeBoxedByteX(Byte b, JsonGenerator generator) throws Exception;

    @JsonInput
    Byte readBoxedByteX(JsonParser parser) throws Exception;
```
(and so on)

Finally, define array serializers that reuse both primitive and boxed serializers:
```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/DelegationSerde.java#L126-L138

@JsonConfig(uses = BoxedScalarsSerde.class)
interface ScalarArraysSerde {
    @JsonOutput
    void writeBooleanArrayX(boolean[] input, JsonGenerator generator) throws Exception;

    @JsonInput
    boolean[] readBooleanArrayX(JsonParser parser) throws Exception;

    @JsonOutput
    void writeBoxedBooleanArrayX(Boolean[] input, JsonGenerator generator) throws Exception;

    @JsonInput
    Boolean[] readBoxedBooleanArrayX(JsonParser parser) throws Exception;
```

This approach allows you to build a comprehensive library of reusable serializers while keeping the generated code small and efficient.
Note that `uses` works transitively, 

## Recursive Types

For recursive types, delegation is crucial. Without delegation, the generated code would have to be infinite.
Take the following Type:
```java
// ../jagger-tests/base/src/main/java/org/tillerino/jagger/tests/model/features/DelegationModel.java#L12-L12

record SelfReferencingRecord(String prop, SelfReferencingRecord self) {}
```

This type cannot be used in any other serialization without adding a dedicated serializer:
```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/DelegationSerde.java#L234-L238

@JsonInput
SelfReferencingRecord deserializeRecord(JsonParser input) throws Exception;

@JsonInput
List<SelfReferencingRecord> deserializeList(JsonParser input) throws Exception;
```

This is because when serializing `SelfReferencingRecord`, a recursive call must be made:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/DelegationSerde$SelfReferencingSerdeImpl.java#L66-L69

case "self": {
  self = this.deserializeRecord(input);
  break;
}
```

## Additional arguments

When delegating to a prototype, any parameter that is not the DTO type itself, is filled automatically from the context of the caller.
Any parameter that matches any of caller's parameters, is filled by the caller's parameter.
This includes the parser/formatter and the de/serialization can be passed directly, but can be anything.
You can pass around your own context objects, which you can use to build more elaborate features.

Jagger will also create method references to instantiate functional interfaces (which is used for generics) and
class instances (which is used for generic array deserialization).

It is recommended to keep the signatures of your prototypes as homogenous as possible, since that will allow deep
passing of context objects. Using your [generics](generics.md) interfaces in [templates](templates.md) will make that
quite simple.
