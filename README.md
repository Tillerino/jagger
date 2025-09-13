# Jagger

Jagger is an annotation-processor that generates databind classes to map JSON and similar formats to Java classes and vice versa.
It has several advantages over most traditional libraries:

1) No reflection is required. Works great with AOT, e.g. when working with GraalVM native images.
2) Barely any runtime dependencies. Shaded jars can get very small. With Nanojson, the overhead is just 34 KiB including the parser.
3) Some errors, which are usually only discovered at runtime, are now compiler errors.
   E.g. no accessible constructor, duplicate property names, unsuitable types for factory methods.
   Static code analysis, e.g. nullness checks, can be extended to the serialization code.
   Jagger includes additional compile time checks, see [symmetry](#verification).

Jagger does not include any parsers or formatters and requires external ones.
Various JSON libraries are supported out-of-the-box, including:
- Jackson streaming (`JsonParser` and `JsonGenerator`). This gives you support of many additional input and output formats
  through existing extensions of these classes like YAML, CBOR, Smile, and more.
- Jackson objects (`JsonNode`)
- Gson (`JsonParser` and `JsonWriter`)
- JSON-P
- Fastjson2 (`JSONReader` and `JSONWriter`)
- Nanojson

You can use any backend by implementing the `JaggerReader` and `JaggerWriter` adapter classes and generating code
for the adapters.

See [Backends](docs/backends.md).

## Table of contents

<!-- toc -->

- [Usage](#usage)
- [Features](#features)
  * [Templates](#templates)
  * [Delegators](#delegators)
  * [Converters](#converters)
  * [Generics](#generics)
  * [Polymorphism](#polymorphism)
  * [Default Values](#default-values)
  * [Verification](#verification)
- [Configuration](#configuration)
  * [@JsonConfig Annotation](#jsonconfig-annotation)
  * [Jackson Annotation Compatibility](#jackson-annotation-compatibility)
- [Exotic use cases](#exotic-use-cases)
  * [Custom implementation](#custom-implementation)
- [Alternatives](#alternatives)
- [Compatibility](#compatibility)
  * [Notable exceptions](#notable-exceptions)
  * [Jackson annotations compatibility](#jackson-annotations-compatibility)
- [Roadmap](#roadmap)
  * [Short-term:](#short-term)
  * [Long-term:](#long-term)

<!-- tocstop -->

## Usage

If you have ever used [Mapstruct](https://github.com/mapstruct/mapstruct), you will feel right at home with Jagger.

Include the following in your POM:

```xml
<dependency>
    <groupId>org.tillerino.jagger</groupId>
    <artifactId>jagger-core</artifactId>
    <version>${jagger.version}</version>
</dependency>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <annotationProcessorPath>
                <groupId>org.tillerino.jagger</groupId>
                <artifactId>jagger-processor</artifactId>
                <version>${jagger.version}</version>
            </annotationProcessorPath>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-parameters</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

To generate readers and writers, create an interface and annotate a method with `@JsonInput` or `@JsonOutput`:

```java
interface MyObjectSerde {
    @JsonInput
    MyObject read(JsonParser parser, DeserializationContext context) throws IOException;

    @JsonOutput
    void write(MyObject object, JsonGenerator generator, SerializationContext context) throws IOException;
}
```

The example above is based on Jackson streaming, which provides `JsonParser` for parsing and `JsonGenerator` for writing JSON.
The Jagger annotation processor will generate `MyJsonMapperImpl`, which implements the interface.
The context parameters can be omitted if they are not explicitly needed.

## Features

### Templates

The `@JsonTemplate` annotation allows you to specify prototypes from templates without specifying each as a separate method.

```java
// jagger-tests/jagger-tests-jackson/src/main/java/org/tillerino/jagger/tests/base/features/TemplatesSerde.java#L14-L16

@JsonTemplate(
        templates = {GenericInputSerde.class, GenericOutputSerde.class},
        types = {double.class, AnEnum.class, double[].class, AnEnum[].class})
```

[more](docs/templates.md)

### Delegators

To keep the generated code small, Jagger can split up implementations across multiple, reusable methods.
Take the following example:

```java
interface MyObjectSerde {
  @JsonInput
  List<MyObject> read(JsonParser parser) throws IOException;

  @JsonInput
  MyObject read(JsonParser parser) throws IOException;
}
```

Here, the implementation of the first method will call the second method for each element in the list.
It is recommended to view the generated code and declare further methods to break down large generated methods.
This will work at any level, and you can even declare methods for primitive types.

[more](docs/delegators.md)

### Converters

It is impractical to write actual (de-)serialisers for data types which have a simpler representation like a string.
`@JsonValue` and `@JsonCreator` are supported, but if you cannot (or do not want to) modify the actual types, you can use converters.
Converters are static methods annotated with `@JsonOutputConverter` or `@JsonInputConverter`.
See this example for `OffsetDateTime`:

```java
public class OffsetDateTimeConverters {
  @JsonOutputConverter
  public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
    return offsetDateTime.toString();
  }

  @JsonInputConverter
  public static OffsetDateTime stringToOffsetDateTime(String string) {
    return OffsetDateTime.parse(string);
  }
}
```

Converter methods can be either located in the same class as the `@JsonInput` or `@JsonOutput` method
or in a separate class and referenced with the `@JsonConfig` `uses` value.

Generics are supported for converters, `@JsonValue`, and `@JsonCreator` methods.
For example, you can write a converter for `Optional<T>`:

```java
public class OptionalConverters {
  @JsonOutputConverter
  public static <T> T optionalToNullable(Optional<T> optional) {
    return optional.orElse(null);
  }

  @JsonInputConverter
  public static <T> Optional<T> nullableToOptional(T value) {
    return Optional.ofNullable(value);
  }
}
```

This specific case has already been implemented for reuse
in [OptionalInputConverters](jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalInputConverters.java)
and [OptionalOutputConverters](jagger-core/src/main/java/org/tillerino/jagger/converters/OptionalOutputConverters.java).
See the [converters](jagger-core/src/main/java/org/tillerino/jagger/converters) package for more premade converters.

### Generics

Generics are well-supported. Object properties can be generic, but also collection and array components as well as map values.

See [generics](docs/generics.md) for details.

### Polymorphism

Jagger supports polymorphism through Jackson's `@JsonTypeInfo` and `@JsonSubTypes` annotations, as well as automatic detection for sealed interfaces and classes. It handles various type identification strategies:

1. **Class-based identification** (`JsonTypeInfo.Id.CLASS`): Uses the full class name as the type identifier
2. **Name-based identification** (`JsonTypeInfo.Id.NAME`): Uses custom names defined in `@JsonSubTypes`
3. **Simple name identification** (`JsonTypeInfo.Id.SIMPLE_NAME`): Uses the simple class name or custom names from `@Type`
4. **Minimal class identification** (`JsonTypeInfo.Id.MINIMAL_CLASS`): Uses a minimal class identifier

**Limitations:**
- Does not support `JsonTypeInfo.Id.CUSTOM` or `JsonTypeInfo.Id.DEDUCTION` (throws an exception)
- Always uses `include = PROPERTY` (does not support other inclusion mechanisms like `WRAPPER_OBJECT` or `WRAPPER_ARRAY`)
- `defaultImpl` is ignored
- `visible` is always `false`

Example with explicit subtypes:
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
    @Type(value = RecordOne.class, name = "1"),
    @Type(value = RecordTwo.class, name = "2")
})
interface MyInterface {
    record RecordOne(String s) implements MyInterface {}
    record RecordTwo(int i) implements MyInterface {}
}

interface MySerde {
    @JsonOutput
    void writeMyInterface(MyInterface obj, JsonGenerator generator) throws Exception;
    
    @JsonInput
    MyInterface readMyInterface(JsonParser parser) throws Exception;
}
```

Example with sealed interfaces (no explicit subtypes needed):
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
sealed interface SealedInterface permits RecordOne, RecordTwo {}

record RecordOne(String s) implements SealedInterface {}
record RecordTwo(int i) implements SealedInterface {}

interface SealedSerde {
    @JsonOutput
    void writeSealed(SealedInterface obj, JsonGenerator generator) throws Exception;
    
    @JsonInput
    SealedInterface readSealed(JsonParser parser) throws Exception;
}
```

The generated serializer will include a type discriminator in the JSON output (e.g., `{"@type": "1", "s": "value"}`), and the deserializer will use this discriminator to instantiate the correct subtype.

### Default Values

When deserializing JSON and a property is missing, Jagger can use default values defined with the `@JsonInputDefaultValue` annotation.

```java
interface MySerde {
    @JsonInput
    MyObject read(JsonParser parser) throws IOException;
    
    @JsonInputDefaultValue
    static String defaultString() {
        return "N/A";
    }
}
```

Default value methods can be defined as siblingsor in separate classes referenced with `@JsonConfig(uses = {...})`.

### Verification

The symmetry of serialization and deserialization can be checked at runtime.
The main consideration is: For each serialized type, is the set of written properties equal to the set of read properties?

Consider the following class:
```java
//jagger-tests/jagger-tests-base/src/main/java/org/tillerino/jagger/tests/model/features/VerificationModel.java#L18-L25

class MoreSettersThanGetters {
    @Getter
    @Setter
    String s;

    @Setter
    String t;
}
```

Assuming that both `s` and `t` are properties that are required to reconstruct the object correctly, then this
class is missing a `@Getter` on `t`.
Inversely, having more getters than setters means possibly serializing redundant information.
Once we move away from POJOs and involve creators or inheritance, this symmetry becomes quite hard judge.

Setting `@JsonConfig(verifySymmetry=FAIL)` will verify symmetry of serialization and deserialization at compile time.
In addition to this symmetry of individual properties, it will verify:
- That each object's fields are serialized and deserialized in exactly one place. Not only does this prevent duplicating
  methods for the same type, but prevents code bloat from nested serde.
- That for each reader/writer, the corresponding writer/reader exists in the first place.
- That properties are not duplicated (with `@JsonProperty("name")`, one could define the same property twice).

It is recommended to generate all code with this configuration.

## Configuration

Jagger supports several configuration options through annotations that can be applied at different levels:

### @JsonConfig Annotation

The `@JsonConfig` annotation provides several configuration options:

- `uses`: References other classes containing serializers/deserializers for delegation
- `implement`: Controls whether methods should be implemented (`DO_IMPLEMENT`, `DO_NOT_IMPLEMENT`)
- `delegateTo`: Controls whether methods can be called from other serializers (`DELEGATE_TO`, `DO_NOT_DELEGATE_TO`)
- `unknownProperties`: Controls handling of unknown properties (`THROW`, `IGNORE`)

### Jackson Annotation Compatibility

Jagger supports several Jackson annotations for configuration:

- `@JsonProperty`: Customize property names in JSON
- `@JsonIgnore`: Ignore specific properties during serialization/deserialization
- `@JsonIgnoreProperties`: Ignore multiple properties or control unknown properties handling

## Exotic use cases

Obviously, Jagger is not complete in any sense, and you may reach the limits of the core functionality.
In this section, we show some ways to get your own functionality into jagger.

### Custom implementation

You can always simply implement serializers yourself:

```java
interface CustomizedSerialization {
    @JsonOutput
    void writeMyObj(MyObj o, JsonGenerator generator) throws IOException;

    @JsonOutput
    default void writeOffsetDate(OffsetDateTime timestamp, JsonGenerator generator) throws IOException {       
        generator.writeString(timestamp.toString());
    }

    record MyObj(OffsetDateTime t) { }
}
```

This works for output and input.

## Alternatives

- [jackson-databind](https://github.com/FasterXML/jackson-databind):
  The definitive standard for Java JSON serialization :heart:. Jackson is the anti-Jagger:
  It is entirely based on reflection, and even includes a mechanism to write Java bytecode at runtime to boost performance.
  Jackson is so large that there is a smaller version called [jackson-jr](https://github.com/FasterXML/jackson-jr). 
- https://github.com/ngs-doo/dsl-json

## Compatibility

In general, Jagger tries to be compatible with Jackson's default behaviour.
Some of Jackson's annotations are supported, but not all and not each supported annotation is supported fully.

### Notable exceptions

- With polymorphism, Jackson will always write and require a discriminator, even when explicitly limiting the type to a specific subtype.
  Jagger will not write or require a discriminator when the subtype is known.
- Jackson requires `ParameterNamesModule` and compilation with the `-parameters` flag to support creator-based deserialization without
  @JsonProperty annotations. Jagger does not require this since this information is always present during annotation processing.
- Jagger will assign the default value of the property type to absent properties even when converters are used.
  Jackson will always use the converter and invoke it with its default argument - _I think_.
  An example of this is that Jagger will initialize an absent `Optional<Optional<T>>` property with `Optional.empty()`
  whereas Jackson will instead initialize it with `Optional.of(Optional.empty())`.
  I asked here: https://github.com/FasterXML/jackson-modules-java8/issues/310

### Jackson annotations compatibility

The following is a rough indication of compatibility with Jackson's annotations.
A checkmark indicates _basic_ compatibility, although there can be edge cases where we are not compatible.

- [ ] JacksonInject
- [ ] JsonAlias
- [ ] JsonAnyGetter
- [ ] JsonAnySetter
- [ ] JsonAutoDetect
- [ ] JsonBackReference
- [ ] JsonClassDescription
- [X] JsonCreator
- [ ] JsonEnumDefaultValue
- [ ] JsonFilter
- [ ] JsonFormat
- [ ] JsonGetter
- [X] JsonIdentityInfo (regular generators and property-based IDs (`PropertyGenerator`) are supported. JSOG not supported)
- [ ] JsonIdentityReference
- [X] JsonIgnore
- [X] JsonIgnoreProperties (`value` and `ignoreUnknown` properties)
- [ ] JsonIgnoreType
- [ ] JsonInclude
- [ ] JsonIncludeProperties
- [ ] JsonKey
- [ ] JsonManagedReference
- [ ] JsonMerge
- [X] JsonProperty (`value` and `required`)
- [ ] JsonPropertyDescription
- [ ] JsonPropertyOrder
- [ ] JsonRawValue
- [ ] JsonRootName
- [ ] JsonSetter
- [X] JsonSubTypes (`failOnRepeatedNames` unsupported)
- [ ] JsonTypeId
- [X] JsonTypeInfo (not `use` `CUSTOM` or `DEDUCE`, always `include` `PROPERTY`, `defaultImpl` ignored, `visible` always `false`)
- [ ] JsonTypeName
- [ ] JsonUnwrapped
- [X] JsonValue
- [ ] JsonView

## Roadmap

### Short-term
- Custom converters per property.

### Long-term

- Slowly add support for more Jackson annotations, but on a need-to-have basis.
  There are so many annotations that we cannot support them all.
