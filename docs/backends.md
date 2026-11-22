# Backends

Jagger supports multiple backends for reading and writing JSON.
You can choose the backend that best fits your requirements and dependencies.

You can get a clearer idea of each backend in practice by looking at the modules in the `jagger-tests` directory.
We run the entire test suite for each backend with some exceptions.
The tests are written for the `jackson-core` backend and copied/adapted to the other backends in the `generate-sources` phase.

The jar of each test module is shaded with the `minimizeJar` flag for each test module to estimate the overhead of each backend.
In many cases, this can be optimized further, but we provide this number as a baseline.

If you have trouble picking a backend, here are some ideas:
- Fastjson2 if you want speed more than anything.
- Nanojson if you want a small footprint.
- Jackson Core if you do not want to worry about anything.

## Jackson Core (streaming)

`jackson-core` provides `JsonParser` and `JsonGenerator` for reading and writing JSON.
We consider this the default backend and use it for most examples.
The required dependency is:

```xml
<dependency>
  <groupId>tools.jackson.core</groupId>
  <artifactId>jackson-core</artifactId>
  <version>${jackson.version}</version>
</dependency>
```

Overhead: 507kiB

## Jackson Databind (objects)

`jackson-databind` provides `JsonNode` for reading and writing JSON.
You would only use this instead of `jackson-core` if you have some special requirements, e.g.
you cannot guarantee that the order of fields in JSON is stable and require polymorphism.
To correctly handle visibility of discriminators and unknown properties, we need adapters for `JsonNode`.
Use `JacksonJsonNodeReaderAdapter` and `JacksonJsonNodeWriterAdapter` to write your `@JsonInput` and `@JsonOutput` methods.
The required dependency is:

```xml
<dependency>
  <groupId>tools.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>${jackson.version}</version>
</dependency>
```

Overhead: 2111kiB

## Jackson 2 Core (streaming)

`com.fasterxml.jackson.core:jackson-core` provides Jackson 2 `JsonParser` and `JsonGenerator` for reading and writing
JSON. Jagger generates code for these types directly, just like for Jackson 3. You can also use the
`Jackson2JsonParserAdapter` and `Jackson2JsonGeneratorAdapter` if you want to pass Jackson 2 parsers and generators
through the `JaggerReader` and `JaggerWriter` abstractions.
The required dependency is:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-core</artifactId>
  <version>${jackson2.version}</version>
</dependency>
```

## Jackson 2 Databind (objects)

`com.fasterxml.jackson.core:jackson-databind` provides Jackson 2 `JsonNode` for reading and writing JSON.
Use `Jackson2JsonNodeReaderAdapter` and `Jackson2JsonNodeWriterAdapter` to write your `@JsonInput` and `@JsonOutput`
methods, for example when you cannot guarantee that the order of fields in JSON is stable and require polymorphism.
The required dependency is:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>${jackson2.version}</version>
</dependency>
```

## Gson

`gson` provides `JsonParser` and `JsonWriter` for reading and writing JSON.
The required dependency is:

```xml
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>${gson.version}</version>
</dependency>
```

Overhead: 298kiB

## fastjson2

`fastjson2` provides `JSONReader` and `JSONWriter` for reading and writing JSON.
At the time of writing it is the fastest JSON library for Java
according to [some benchmarks](https://github.com/fabienrenaud/java-json-benchmark).
The required dependency is:

```xml
<dependency>
  <groupId>com.alibaba.fastjson2</groupId>
  <artifactId>fastjson2</artifactId>
  <version>${fastjson2.version}</version>
</dependency>
```

Overhead: 1945kiB

## Jakarta JSON-P

`jakarta.json-api` is an API definition which provides `JsonParser` and `JsonGenerator` for reading and writing JSON.
`JsonParser#currentToken` is fairly new and not supported by all implementations.
Additionally, `JsonParser` does not allow us to properly save the state of _the input has ended_.
This is why we wrap `JsonParserWrapper` around it.

The required dependency is:

```xml
<dependency>
  <groupId>jakarta.json</groupId>
  <artifactId>jakarta.json-api</artifactId>
  <version>${jakarta.json.version}</version>
</dependency>
```

In addition to the API, you need to include an implementation. There are several available.

```xml
<dependency>
  <groupId>org.apache.johnzon</groupId>
  <artifactId>johnzon-core</artifactId>
  <version>${johnzon.version}</version>
</dependency>
```

Johnzon and the API have an overhead of 183kiB.

```xml
<dependency>
  <groupId>org.glassfish</groupId>
  <artifactId>jakarta.json</artifactId>
  <version>${glassfish.json.version}</version>
</dependency>
```

The Glassfish implementation has an overhead of 137kiB.

## Nanojson

`nanojson` is a small JSON parser and writer.
Its `JsonParser` class just misses what we need. To read JSON, you need to create a `NanojsonReaderAdapter` instance
from an `InputStream` or `Reader`.
We use `JsonAppendableWriter` for writing JSON, which can be obtained from the `JsonWriter` factory.

The required dependency is:

```xml
<dependency>
  <groupId>com.grack</groupId>
  <artifactId>nanojson</artifactId>
  <version>${nanojson.version}</version>
</dependency>
```

Overhead: 34kiB