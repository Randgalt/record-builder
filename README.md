[![Build Status](https://travis-ci.org/Randgalt/record-builder.svg?branch=master)](https://travis-ci.org/Randgalt/record-builder)
[![Maven Central](https://img.shields.io/maven-central/v/io.soabase.record-builder/record-builder.svg)](https://search.maven.org/search?q=g:io.soabase.record-builder%20a:record-builder)

# RecordBuilder - Early Access

## What is RecordBuilder

Java 14 is introducing [Records](https://cr.openjdk.java.net/~briangoetz/amber/datum.html) as a preview feature. Since Java 9, features in Java are being released in stages. While the Java 14 version of records is fantastic, it's currently missing an important feature for data classes: a builder. This project is an annotation processor that creates companion builder classes for Java records.

## Example

```java
@RecordBuilder
public record NameAndAge(String name, int age){}
```

This will generate a builder class that can be used ala:

```java
// build from components
var n1 = NameAndAgeBuilder.builder().name(aName).age(anAge).build();

// generate a copy with a changed value
var n2 = NameAndAgeBuilder.builder(n1).age(newAge).build(); // name is the same as the name in n1

// pass to other methods to set components
var builder = new NameAndAgeBuilder();
setName(builder);
setAge(builder);
var n3 = builder.build();
```

The full builder class is defined as:

```java
public class NameAndAgeBuilder {
    private String name;

    private int age;

    private NameAndAgeBuilder() {
    }

    private NameAndAgeBuilder(String name, int age) {
        this.name = name;
        this.age = age;
    }

    /**
     * Return a new builder with all fields set to default Java values
     */
    public static NameAndAgeBuilder builder() {
        return new NameAndAgeBuilder();
    }

    /**
     * Return a new builder with all fields set to the values taken from the given record instance
     */
    public static NameAndAgeBuilder builder(NameAndAge from) {
        return new NameAndAgeBuilder(from.name(), from.age());
    }

    /**
     * Return a new record instance with all fields set to the current values in this builder
     */
    public NameAndAge build() {
        return new NameAndAge(name, age);
    }

    /**
     * Set a new value for the {@code name} record component in the builder
     */
    public NameAndAgeBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Return the current value for the {@code name} record component in the builder
     */
    public String name() {
        return name;
    }

    /**
     * Set a new value for the {@code age} record component in the builder
     */
    public NameAndAgeBuilder age(int age) {
        this.age = age;
        return this;
    }

    /**
     * Return the current value for the {@code age} record component in the builder
     */
    public int age() {
        return age;
    }

    /**
     * Return a stream of the record components as map entries keyed with the component name and the value as the component value
     */
    public static Stream<Map.Entry<String, Object>> stream(NameAndAge record) {
        return Stream.of(new AbstractMap.SimpleEntry<>("name", record.name()),
                 new AbstractMap.SimpleEntry<>("age", record.age()));
    }

    @Override
    public String toString() {
        return "NameAndAgeBuilder[name=" + name + ", age=" + age + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || ((o instanceof NameAndAgeBuilder b)
                && Objects.equals(name, b.name)
                && (age == b.age));
    }
}
```

## Usage

### Maven

1\. Add the dependency that contains the `@RecordBuilder` annotation.

```
<dependency>
    <groupId>io.soabase.record-builder</groupId>
    <artifactId>record-builder-core</artifactId>
    <version>set-version-here</version>
</dependency>

```

2\. Enable the annotation processing for the Maven Compiler Plugin:

```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>set-version-here</version>
    <configuration>
        <annotationProcessorPaths>
            <annotationProcessorPath>
                <groupId>io.soabase.record-builder</groupId>
                <artifactId>record-builder-processor</artifactId>
                <version>set-version-here</version>
            </annotationProcessorPath>
        </annotationProcessorPaths>
        <annotationProcessors>
            <annotationProcessor>io.soabase.recordbuilder.processor.RecordBuilderProcessor</annotationProcessor>
        </annotationProcessors>

        
        <!-- "release" and "enable-preview" are required while records are preview features -->
        <release>14</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>

        ... any other options here ...
    </configuration>
</plugin>
```

### Gradle

*Note: Gradle builds are not currently working. Please track https://github.com/gradle/gradle/issues/12680 for a fix*

Add the following to your build.gradle file:

```
dependencies {
    annotationProcessor 'io.soabase.record-builder:record-builder-processor:$version-goes-here'
    implementation 'io.soabase.record-builder:record-builder-core:$version-goes-here'
}

tasks.withType(JavaCompile) {
    options.compilerArgs += '--enable-preview'
}
tasks.withType(Test) {
    jvmArgs += "--enable-preview"
}
```

### IDE

Depending on your IDE you are likely to need to enable Annotation Processing in your IDE settings.

## Enable Preview

Note: records are a preview feature only. You'll need take a number of steps in order to try RecordBuilder:

- Install and make active Java 14 or later
- Make sure your development tool is using Java 14 or later and is configured to enable preview features (for Maven I've documented how to do this here: [https://stackoverflow.com/a/59363152/2048051](https://stackoverflow.com/a/59363152/2048051))
- Bear in mind that this is not yet meant for production and there are numerous bugs in the tools and JDKs.

Note: I've seen some very odd compilation bugs with the current Java 14 and Maven. If you get internal Javac errors I suggest rebuilding with `mvn clean package` and/or `mvn clean install`.

## Customizing

The names of the generated methods, etc. are determined by [RecordBuilderMetaData](https://github.com/Randgalt/record-builder/blob/master/record-builder-core/src/main/java/io/soabase/recordbuilder/core/RecordBuilderMetaData.java). If you want to use your own meta data instance:

- Create a class that implements RecordBuilderMetaData
- When compiling, make sure that the compiled class is in the processor path
- Add a "metaDataClass" compiler option with the class name. E.g. `javac ... -AmetaDataClass=foo.bar.MyMetaData`

Alternatively, you can provide values for each individual meta data (or combinations):

- `javac ... -AcopyMethodName=foo`
- `javac ... -AbuilderMethodName=foo`
- `javac ... -AbuildMethodName=foo`
- `javac ... -AcomponentsMethodName=foo`
- `javac ... -AfileComment=foo`
- `javac ... -AfileIndent=foo`
- `javac ... -AprefixEnclosingClassNames=foo`

## TODOs

- Document how to integrate with Gradle
- Keep up with changes
- Testing
- Etc.
