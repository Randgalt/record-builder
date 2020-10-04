[![Build Status](https://travis-ci.org/Randgalt/record-builder.svg?branch=master)](https://travis-ci.org/Randgalt/record-builder)
[![Maven Central](https://img.shields.io/maven-central/v/io.soabase.record-builder/record-builder.svg)](https://search.maven.org/search?q=g:io.soabase.record-builder%20a:record-builder)

# RecordBuilder - Early Access

## What is RecordBuilder

Java 15 introduced [Records](https://cr.openjdk.java.net/~briangoetz/amber/datum.html) as a preview feature. Since Java 9, 
features in Java are being released in stages. While the Java 15 version of records is fantastic, it's currently missing important features 
for data classes: a builder and "with"ers. This project is an annotation processor that creates:
 
- a companion builder class for Java records
- an interface that adds "with" copy methods
- an annotation that generates a Java record from an Interface template

_Details:_

- [RecordBuilder Details](#RecordBuilder-Example)
- [Wither Details](#Wither-Example)
- [RecordBuilder Full Definition](#Builder-Class-Definition)
- [Record From Interface Details](#RecordInterface-Example)

## RecordBuilder Example

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

## Wither Example

```java
@RecordBuilder
public record NameAndAge(String name, int age) implements NameAndAgeBuilder.With {}
```

In addition to creating a builder, your record is enhanced by "wither" methods ala:

```java
var r1 = new NameAndAge("foo", 123);
var r2 = r1.withName("bar");
var r3 = r2.withAge(456);

// access the builder as well
var r4 = r3.with().age(101).name("baz").build();

// alternate method of accessing the builder (note: no need to call "build()")
var r5 = r4.with(b -> b.age(200).name("whatever"));
```

_Hat tip to [Benji Weber](https://benjiweber.co.uk/blog/2020/09/19/fun-with-java-records/) for the Withers idea._

## Builder Class Definition

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

    /**
     * Add withers to {@code NameAndAge}
     */
    public interface With {
        /**
         * Return a new record builder using the current values
         */
        default NameAndAgeBuilder with() {
            var r = (NameAndAge)(Object)this;
            return NameAndAgeBuilder.builder(r);
        }

        /**
         * Return a new record built from the builder passed to the given consumer
         */
        default NameAndAge with(Consumer<NameAndAgeBuilder> consumer) {
            var r = (NameAndAge)(Object)this;
            NameAndAgeBuilder builder = NameAndAgeBuilder.builder(r);
            consumer.accept(builder);
            return builder.build();
        }

        /**
         * Return a new instance of {@code NameAndAge} with a new value for {@code name}
         */
        default NameAndAge withName(String name) {
            var r = (NameAndAge)(Object)this;
            return new NameAndAge(name, r.age());
        }

        /**
         * Return a new instance of {@code NameAndAge} with a new value for {@code age}
         */
        default NameAndAge withAge(int age) {
            var r = (NameAndAge)(Object)this;
            return new NameAndAge(r.name(), age);
        }
    }
}
```

## RecordInterface Example

```java
@RecordInterface
public interface NameAndAge {
    String name(); 
    int age();
}
```

This will generate a record ala:

```java
@RecordBuilder
public record NameAndAgeRecord(String name, int age) implements NameAndAge {}
```

Note that the generated record is annotated with `@RecordBuilder` so a record
builder is generated for the new record as well.

Notes:

- Non static methods in the interface...
  - ...cannot have arguments
  - ...must return a value
  - ...cannot have type parameters
- Methods with default implementations are used in the generation unless they are annotated with `@IgnoreDefaultMethod`
- If you do not want a record builder generated, annotate your interface as `@RecordInterface(addRecordBuilder = false)`

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
        <release>15</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>

        ... any other options here ...
    </configuration>
</plugin>
```

3\. Enable Preview for Maven

Create a file in your project's root named `.mvn/jvm.config`. The file should have 1 line with the value: `--enable-preview`. (see: https://stackoverflow.com/questions/58023240)

### Gradle

Add the following to your build.gradle file:

```
dependencies {
    annotationProcessor 'io.soabase.record-builder:record-builder-processor:$version-goes-here'
    implementation 'io.soabase.record-builder:record-builder-core:$version-goes-here'
}

tasks.withType(JavaCompile) {
    options.fork = true
    options.forkOptions.jvmArgs += '--enable-preview'
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

- Install and make active Java 15 or later
- Make sure your development tool is using Java 15 or later and is configured to enable preview features (for Maven I've documented how to do this here: [https://stackoverflow.com/a/59363152/2048051](https://stackoverflow.com/a/59363152/2048051))
- Bear in mind that this is not yet meant for production and there are numerous bugs in the tools and JDKs.

Note: I've seen some very odd compilation bugs with the current Java 15 and Maven. If you get internal Javac errors I suggest rebuilding with `mvn clean package` and/or `mvn clean install`.

## Customizing

The names of the generated methods, etc. are determined by [RecordBuilderMetaData](https://github.com/Randgalt/record-builder/blob/master/record-builder-core/src/main/java/io/soabase/recordbuilder/core/RecordBuilderMetaData.java). If you want to use your own meta data instance:

- Create a class that implements RecordBuilderMetaData
- When compiling, make sure that the compiled class is in the processor path
- Add a "metaDataClass" compiler option with the class name. E.g. `javac ... -AmetaDataClass=foo.bar.MyMetaData`

Alternatively, you can provide values for each individual meta data (or combinations):

- `javac ... -Asuffix=foo`
- `javac ... -AinterfaceSuffix=foo`
- `javac ... -AcopyMethodName=foo`
- `javac ... -AbuilderMethodName=foo`
- `javac ... -AbuildMethodName=foo`
- `javac ... -AcomponentsMethodName=foo`
- `javac ... -AwithClassName=foo`
- `javac ... -AwithClassMethodPrefix=foo`
- `javac ... -AfileComment=foo`
- `javac ... -AfileIndent=foo`
- `javac ... -AprefixEnclosingClassNames=foo`
