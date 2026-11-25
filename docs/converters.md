# Converters

Converters are methods annotated with `@JsonOutputConverter` or `@JsonInputConverter`.
They are used to convert types that Jagger cannot serialize directly, e.g. `OffsetDateTime`:

```java
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/JavaTimeOutputConverters.java#L27-L30

@JsonOutputConverter
public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
    return offsetDateTime.toString();
}
```

```java
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/JavaTimeInputConverters.java#L27-L30

@JsonInputConverter
public static OffsetDateTime stringToOffsetDateTime(String string) {
    return OffsetDateTime.parse(string);
}
```

Converter methods can be either located in the same class as the `@JsonInput` or `@JsonOutput` method
or in a separate class and referenced with the `@JsonConfig` `uses` value.

Converters can be static or non-static. Non-static converters declared in the same class are called on the generated
instance. For converters in a used class, Jagger instantiates the class if it is concrete and has a no-arg constructor,
or passes it to the generated constructor otherwise. If the converter type is passed as a parameter to the prototype,
that parameter is used directly.

The generated code converts the value before writing it and after reading it.
An `OffsetDateTime` is written as a `String`:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/ConvertersSerdeImpl.java#L35-L45

OffsetDateTime timestamp = record.timestamp();
if (timestamp != null) {
  String converted = JavaTimeOutputConverters.offsetDateTimeToString(timestamp);
  if (converted != null) {
    generator.writeStringProperty("timestamp", converted);
  } else {
    generator.writeNullProperty("timestamp");
  }
} else {
  generator.writeNullProperty("timestamp");
}
```

and read back with the reverse converter:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/ConvertersSerdeImpl.java#L70-L82

case "timestamp": {
  final String timestampToConvert;
  if (nextIfCurrentTokenIs(parser, VALUE_NULL)) {
    timestampToConvert = null;
  } else if (parser.currentToken() == VALUE_STRING) {
    timestampToConvert = parser.getText();
    parser.nextToken();
  } else {
    throw new JaggerDatabindException("Expected string");
  }
  timestamp = timestampToConvert != null ? JavaTimeInputConverters.stringToOffsetDateTime(timestampToConvert) : null;
  break;
}
```

Generics are supported for converters, `@JsonValue`, and `@JsonCreator` methods.
For example, you can write a converter for `Optional<T>`:

```java
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalOutputConverters.java#L11-L14

@JsonOutputConverter
public static <T> T optionalToNullable(Optional<T> optional) {
    return optional.orElse(null);
}
```

```java
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalInputConverters.java#L13-L16

@JsonInputConverter
public static <T> Optional<T> nullableToOptional(@Nullable T nullable) {
    return Optional.ofNullable(nullable);
}
```

This specific case has already been implemented for reuse
in [OptionalInputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalInputConverters.java)
and [OptionalOutputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalOutputConverters.java).
See the [converters](../jagger-core/src/main/java/org/tillerino/jagger/converters) package for more premade converters.

Use
[AllInputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/AllInputConverters.java) and
[AllOutputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/AllOutputConverters.java)
for all premade converters.
Add them to `@JsonConfig` to enable all of them at once:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/ConvertersSerde.java#L17-L17

@JsonConfig(uses = {AllInputConverters.class, AllOutputConverters.class})
```

## Automatically instantiated converters

When a generic prototype requires an output converter to `String`, Jagger can instantiate it automatically for boxed
types and enums. This is useful for generic map keys, where the key type varies between prototypes:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L238-L253

interface GenericKeyMapSerde {
    @JsonOutput
    <K, V> void writeGenericMap(
            Map<K, V> map,
            JsonGenerator gen,
            GenericConvertersSerde.OutputConverter<K, String> keyConverter,
            GenericOutputSerde<V> valueWriter)
            throws Exception;

    @JsonInput
    <K, V> Map<K, V> readGenericMap(
            JsonParser parser,
            GenericConvertersSerde.InputConverter<String, K> keyConverter,
            GenericInputSerde<V> valueReader)
            throws Exception;
}
```

The concrete prototype only declares the map. Jagger fills in the key converter:

```java
// ../jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features/GenericsSerde.java#L255-L266

@JsonConfig(
        uses = {
            GenericKeyMapSerde.class,
            StringSerde.class,
        })
interface IntegerStringMapSerde {
    @JsonOutput
    void writeIntegerStringMap(Map<Integer, String> map, JsonGenerator gen) throws Exception;

    @JsonInput
    Map<Integer, String> readIntegerStringMap(JsonParser parser) throws Exception;
}
```

Boxed types are converted with `Objects.toString(value, null)`:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$IntegerStringMapSerdeImpl.java#L19-L22

@Override
public void writeIntegerStringMap(Map<Integer, String> map, JsonGenerator gen) throws Exception {
  genericKeyMapSerde$0$delegate.writeGenericMap(map, gen, from -> Objects.toString(from, null), stringSerde$1$delegate);
}
```

Enums use the same serialization as map keys, including explicit `@JsonProperty` names:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/GenericsSerde$CustomNamedEnumStringMapSerdeImpl.java#L29-L33

@Override
public void writeCustomNamedEnumStringMap(Map<EnumsModel.CustomNames.CustomNamedEnum, String> map,
    JsonGenerator gen) throws Exception {
  genericKeyMapSerde$0$delegate.writeGenericMap(map, gen, from -> customNamedEnum$0$values.get(from), stringSerde$1$delegate);
}
```
