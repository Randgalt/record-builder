[◀︎ RecordBuilder](README.md) • Customizing RecordBuilder

# RecordBuilderFull

Note: `@RecordBuilderFull` has most optional features enabled. It's an alternate
form of `@RecordBuilder` that uses the [templating mechanism](#create-a-custom-annotation) to
enable optional features.

# Customizing RecordBuilder

RecordBuilder can be customized in a number of ways. The types of customizations will change over time. See
[RecordBuilder Options](options.md)
for the current set of customizations and their default values. For example, the `useImmutableCollections` option
adds special handling for record components of type `java.util.List`, `java.util.Set`, `java.util.Map` and `java.util.Collection`. When the record is built, any components of these types are passed through an added shim method that uses the corresponding immutable collection (e.g. `List.copyOf(o)`) or an empty immutable collection if the component is `null`.

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

To customize a single record, add `@RecordBuilder.Options` ([options details](options.md)) in addition to
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

Note: the template mechanism also supports `@RecordInterface` templates via the `asRecordInterface` attribute.
When it is set a `@RecordInterface` template is created instead.
