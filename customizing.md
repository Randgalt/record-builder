[◀︎ RecordBuilder](README.md) • Customizing RecordBuilder

# Customizing RecordBuilder

RecordBuilder can be customized in a number of ways. The types of customizations will change over time. See
[@RecordBuilder.Options](record-builder-core/src/main/java/io/soabase/recordbuilder/core/RecordBuilder.java)
for the current set of customizations and their default values. 

You can:

- [Customize an entire build](#customize-an-entire-build) - all uses of `@RecordBuilder` in your project
- [Customize a single record](#customize-a-single-record) annotated with `@RecordBuilder`
- [Create a custom annotation](#create-a-custom-annotation) that specifies your options and use that instead of `@RecordBuilder`

## Customize an entire build

To customize an entire build, use javac's annotation processor options via `-A` on the command line.
The options available are the same as the attributes in [@RecordBuilder.Options](record-builder-core/src/main/java/io/soabase/recordbuilder/core/RecordBuilder.java).
i.e. to disable "prefixing enclosing class names", compile with:

```shell
javac -AprefixEnclosingClassNames=false ...
```

_Note: use a separate `-A` for each option._

#### Maven

If you are using Maven, specify the options in the compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>${maven-compiler-plugin-version}</version>
    <configuration>
        <compilerArgs>
            <arg>-AprefixEnclosingClassNames=false</arg>
            <arg>-AfileComment="something different"</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

#### Gradle

For Gradle, specify the options:

```groovy
compilerArgs.addAll(['-AprefixEnclosingClassNames=false', '-AfileComment="something different"'])
```

## Customize a single record

To customize a single record, add `@RecordBuilder.Options` in addition to
`@RecordBuilder`.

E.g.

```java
@RecordBuilder.Options(withClassName = "Wither")
@RecordBuilder
public record MyRecord(String s){}
```

## Create a custom annotation

Using `@RecordBuilder.Template` you can create your own RecordBuilder annotation
that uses the set of options you want. E.g. to create a custom annotation that
uses an alternate file comment and an alternate With classname:

```java
@RecordBuilder.Template(options = @RecordBuilder.Options(
        fileComment = "MyCo license",
        withClassName = "Wither"
))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface MyCoRecordBuilder {
}
```

Now, you can use `@MyCoRecordBuilder` instead of `@RecordBuilder` and the record
will be built with options as specified.
