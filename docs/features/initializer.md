[◀︎ RecordBuilder](../../README.md) • [◀︎ Features](../features.md) • Initialization / Default Values

# Initialization / Default Values

RecordBuilder's default behavior is to use the Java language default value for record components which is usually
`null` (note that RecordBuilder has options for controlling `null`. See the [Nulls](nulls.md) doc for details).
You can use the `@RecordBuilder.Initializer` annotation to provide custom initializers for record components in the
generated builder.

Annotate any record component with `@RecordBuilder.Initializer(...)`. The annotation attribute is the name
of either a public static final field or a public static method in the record of the same type as the record
component. Optionally, `@RecordBuilder.Initializer(source = Foo.class, value = "name")` can be used to specify
a custom class/interface instead of the target record.

## Example 1

```java
@RecordBuilder
public record MyRecord(char c, @RecordBuilder.Initializer("DEFAULT_INT") int i, String s) {
    public static final int DEFAULT_INT = 1234;
}
```

```java
MyRecord myRecord = MyRecordBuilder.builder().build();

// this will print: 1234
System.out.println(myRecord.i());
```

## Example 2

```java
@RecordBuilder
public record MyOtherRecord(char c, 
                       @RecordBuilder.Initializer("DEFAULT_INT") int i,
                       @RecordBuilder.Initializer(value = "defaultName", source = MyDefaults.class) String s) {
    public static final int DEFAULT_INT = 1234;
}

public interface MyDefaults {
    static String defaultName() {
        return "unknown"; 
    }
} 
```

```java
MyOtherRecord myOtherRecord = MyOtherRecordBuilder.builder().build();

// this will print: 1234
System.out.println(myOtherRecord.i());

// this will print: unknown
System.out.println(myOtherRecord.s());
```

