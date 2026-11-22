# Converters

Converters are static methods annotated with `@JsonOutputConverter` or `@JsonInputConverter`.
They are used to convert types that Jagger cannot serialize directly, e.g. `OffsetDateTime`:

```java
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/JavaTimeInputConverters.java#L8-L16

@JsonOutputConverter
public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
    return offsetDateTime.toString();
}

@JsonInputConverter
public static OffsetDateTime stringToOffsetDateTime(String string) {
    return OffsetDateTime.parse(string);
}
```

Converter methods can be either located in the same class as the `@JsonInput` or `@JsonOutput` method
or in a separate class and referenced with the `@JsonConfig` `uses` value.

The generated code converts the value before writing it and after reading it.
An `OffsetDateTime` is written as a `String`:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/ConvertersSerdeImpl.java#L35-L45

OffsetDateTime timestamp = record.timestamp();
if (timestamp != null) {
  String converted = JavaTimeInputConverters.offsetDateTimeToString(timestamp);
  if (converted != null) {
    generator.writeStringField("timestamp", converted);
  } else {
    generator.writeNullField("timestamp");
  }
} else {
  generator.writeNullField("timestamp");
}
```

and read back with the reverse converter:

```java
// ../jagger-tests/jackson/target/generated-sources/annotations/org/tillerino/jagger/tests/base/features/ConvertersSerdeImpl.java#L69-L81

case "timestamp": {
  final String timestampToConvert;
  if (nextIfCurrentTokenIs(parser, VALUE_NULL)) {
    timestampToConvert = null;
  } else if (parser.currentToken() == VALUE_STRING) {
    timestampToConvert = parser.getText();
    parser.nextToken();
  } else {
    throw new IOException("Expected string, got " + parser.currentToken() + " at " + parser.getCurrentLocation());
  }
  timestamp = JavaTimeInputConverters.stringToOffsetDateTime(timestampToConvert);
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
// ../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalInputConverters.java#L12-L15

@JsonInputConverter
public static <T> Optional<T> nullableToOptional(T nullable) {
    return Optional.ofNullable(nullable);
}
```

This specific case has already been implemented for reuse
in [OptionalInputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalInputConverters.java)
and [OptionalOutputConverters](../jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalOutputConverters.java).
See the [converters](../jagger-core/src/main/java/org/tillerino/jagger/converters) package for more premade converters.
