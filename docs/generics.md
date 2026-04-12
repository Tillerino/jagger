# Generics

<!-- toc -->

- [Basics](#basics)
- [Filling delegator parameters](#filling-delegator-parameters)
- [Arrays](#arrays)

<!-- tocstop -->

## Basics

To start with generics, we need to define functional interfaces for generic serialization and/or deserialization.

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L17-L21

@JsonConfig(implement = JsonConfig.ImplementationMode.DO_NOT_IMPLEMENT)
interface GenericInputSerde<V> {
    @JsonInput
    V readOnGenericInterface(JsonParser parser) throws Exception;
}
```

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L24-L28

@JsonConfig(implement = JsonConfig.ImplementationMode.DO_NOT_IMPLEMENT)
interface GenericOutputSerde<U> {
    @JsonOutput
    void writeOnGenericInterface(U obj, JsonGenerator gen) throws Exception;
}
```

`DO_NOT_IMPLEMENT` is important to prevent implementation of these prototypes.
Note that these can have additional arguments like all prototypes, see [delegation](delegators.md#additional-arguments).
These generic interfaces are well-suited for [templates](templates.md).

Now say you have the following generic class:

```java
// ../jagger-tests/base/src/main/java/org/tillerino/jagger/tests/model/features/GenericsModel.java#L4-L4

record GenericRecord<F>(F f) {}
```

When you define a prototype for this, add a parameter of the matching functional interface:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L30-L37

interface GenericRecordSerde {
    @JsonInput
    <T> GenericRecord<T> readGenericRecord(JsonParser parser, GenericInputSerde<T> fieldSerde) throws Exception;

    @JsonOutput
    <T> void writeGenericRecord(GenericRecord<T> obj, JsonGenerator gen, GenericOutputSerde<T> fieldSerde)
            throws Exception;
}
```

The code generated for these generic prototypes will then delegate to the prototypes passed as a parameter:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$GenericRecordSerdeImpl.java#L55-L56

gen.writeFieldName("f");
fieldSerde.writeOnGenericInterface(obj.f(), gen);
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$GenericRecordSerdeImpl.java#L31-L34

case "f": {
  f = fieldSerde.readOnGenericInterface(parser);
  break;
}
```

## Filling delegator parameters

When Jagger delegates to a generic prototype, the generic delegator parameter is filled automatically from the available
prototypes, if possible. Say you define prototypes for a concrete generic record:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L39-L51

@JsonConfig(
        uses = {
            BoxedScalarsSerde.class,
            GenericRecordSerde.class,
        })
interface IntegerRecordSerde {
    @JsonInput
    GenericRecord<Integer> readIntegerRecord(JsonParser parser) throws Exception;

    @JsonOutput
    void writeIntegerRecord(GenericRecord<Integer> obj, JsonGenerator gen) throws Exception;
}

```

From `@JsonConfig`, the prototypes for `GenericRecord<F>` and prototypes for `Integer` are available. Jagger will then
implement these concrete prototypes like so:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$IntegerRecordSerdeImpl.java#L12-L31

public class GenericsSerde$IntegerRecordSerdeImpl implements GenericsSerde.IntegerRecordSerde {
  GenericsSerde.GenericRecordSerde genericRecordSerde$0$delegate = new GenericsSerde$GenericRecordSerdeImpl();

  DelegationSerde.BoxedScalarsSerde boxedScalarsSerde$1$delegate = new DelegationSerde$BoxedScalarsSerdeImpl();

  @Override
  public GenericsModel.GenericRecord<Integer> readIntegerRecord(JsonParser parser) throws
      Exception {
    if (!parser.hasCurrentToken()) {
      parser.nextToken();
    }
    return genericRecordSerde$0$delegate.readGenericRecord(parser, boxedScalarsSerde$1$delegate::readBoxedIntX);
  }

  @Override
  public void writeIntegerRecord(GenericsModel.GenericRecord<Integer> obj, JsonGenerator gen) throws
      Exception {
    genericRecordSerde$0$delegate.writeGenericRecord(obj, gen, boxedScalarsSerde$1$delegate::writeBoxedIntX);
  }
}
```

Note that the signatures of the functional interfaces and the prototypes to delegate to in the generic prototype
must match exactly so that method signatures can be used. Jagger cannot currently create more complex lambdas.
It is recommended to use your generic functional interfaces as [templates](templates.md). That way everything will
always match by construction.

It is not necessary to define methods for all concrete occurrences of your generic types. The generic prototype will
be called whenever necessary. For this record, which has a property of the fully instantiated generic record, 

```java
// ../jagger-tests/base/src/main/java/org/tillerino/jagger/tests/model/features/GenericsModel.java#L8-L8

record UsesGenericRecord(GenericRecord<Integer> gi) {}
```

the prototype

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L52-L63

@JsonConfig(
        uses = {
            BoxedScalarsSerde.class,
            GenericRecordSerde.class,
        })
interface UsesGenericRecordSerde {
    @JsonOutput
    void writeUsesGenericRecord(UsesGenericRecord usesGenericRecord, JsonGenerator gen) throws Exception;

    @JsonInput
    UsesGenericRecord readUsesGenericRecord(JsonParser parser) throws Exception;
}
```

automatically calls the generic prototype:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$UsesGenericRecordSerdeImpl.java#L27-L28

gen.writeFieldName("gi");
genericRecordSerde$0$delegate.writeGenericRecord(usesGenericRecord.gi(), gen, boxedScalarsSerde$1$delegate::writeBoxedIntX);
```

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$UsesGenericRecordSerdeImpl.java#L50-L53

case "gi": {
  gi = genericRecordSerde$0$delegate.readGenericRecord(parser, boxedScalarsSerde$1$delegate::readBoxedIntX);
  break;
}
```

## Arrays

While writing generic arrays works just like any generic type, reading arrays requires us to instantiate an array of
the component type. This means that it has to be known at runtime.

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L116-L118

@JsonInput
<T> T[] readGenericArray(JsonParser parser, GenericInputSerde<T> componentReader, Class<T[]> arrayClass)
        throws Exception;
```

Jagger will instantiate this class parameter automatically when necessary:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$ConcreteContainerSerdeImpl.java#L56-L62

@Override
public Double[] readDoubleArray(JsonParser parser) throws Exception {
  if (!parser.hasCurrentToken()) {
    parser.nextToken();
  }
  return genericContainersSerde$0$delegate.readGenericArray(parser, boxedScalarsSerde$1$delegate::readBoxedDoubleX, Double[].class);
}
```
