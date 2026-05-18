# Plugins

Jagger is designed to be extended with plugins.
This document explains the mechanisms that are available to plugins and how they can be applied for different use cases.

<!-- toc -->

## Plugin discovery

Plugins are discovered via the standard Java `ServiceLoader` mechanism.
To register your plugin, create a file `META-INF/services/org.tillerino.jagger.processor.ext.JaggerPlugin` containing your
plugin's fully qualified class name.

The easiest way to do this is with Google's `@AutoService` annotation, which generates the service file at compile time:

```java
// ../jagger-tests/custom-plugins/src/main/java/org/tillerino/jagger/tests/plugins/DeepClonePlugin.java#L38-L39

@Override
public void configure(JaggerContext ctx) {
```

Add the annotation and its processor to your build:

```xml
<!-- ../jagger-tests/custom-plugins/pom.xml#L15-L19 -->

<dependency>
    <groupId>com.google.auto.service</groupId>
    <artifactId>auto-service-annotations</artifactId>
    <scope>provided</scope>
</dependency>
```

`auto-service` itself must be on the annotation processor path (not the regular compile path):

```xml
<!-- ../jagger-tests/custom-plugins/pom.xml#L55-L59 -->

<annotationProcessorPath>
    <groupId>com.google.auto.service</groupId>
    <artifactId>auto-service</artifactId>
    <version>${auto-service.version}</version>
</annotationProcessorPath>
```

## New configuration annotations

Jagger, by default, only supports a handful of annotations.
You can add support for new annotations by mapping configuration properties to the annotation properties.
The following example sets up the connection between the `PROPERTY_NAME` configuration property and
Jackson's `@JsonProperty` annotation:

```java
// ../jagger-processor/src/main/java/org/tillerino/jagger/processor/config/JacksonAnnotationsPlugin.java#L36-L39

ctx.configProperties.addConfigAnnotation(
        PropertyName.PROPERTY_NAME,
        CFJA + ".JsonProperty",
        ann -> ann.method("value", true).map(AnnotationValueWrapper::asString));
```

Support for Jackson annotations is part of the core processor, but internally, it is designed as a plugin.
You can use this as a template to write your own plugin: [JacksonAnnotationsPlugin.java](../jagger-processor/src/main/java/org/tillerino/jagger/processor/config/JacksonAnnotationsPlugin.java)

## Custom code generators

You can add code generators that respond to custom annotations in a couple of steps:

- Write a plugin that declares the supported annotations and registers a `PrototypeDetector` in `configure()`
- The detector validates the method signature and returns a `PrototypeKind`
- The `PrototypeKind` generates the body of the method on which the annotation was detected.

Jagger will take care of the rest:
- Naming and creating the generated classes
- Writing the corresponding method signatures
- Generics
- Templating (Requires `TemplatablePrototypeKind`)
- A configuration system
- A delegation system

[The custom plugin test project](../jagger-tests/custom-plugins/src/main/java/org/tillerino/jagger/tests/plugins)
contains some simple plugins that can serve as a starting reference.

Internally, Jagger is split into plugins. These can serve as good references:
- [DatabindPlugin.java](../jagger-processor/src/main/java/org/tillerino/jagger/processor/config/DatabindPlugin.java)
- [JdbcPlugin.java](../jagger-processor/src/main/java/org/tillerino/jagger/processor/config/JdbcPlugin.java)
- [Templates.java](../jagger-processor/src/main/java/org/tillerino/jagger/processor/features/Templates.java)
