[◀︎ RecordBuilder](../../README.md) • [◀︎ Features](../features.md) • Standard Usage

# Standard Usage

This is an example of the standard usage of RecordBuilder. Annotate a record with
`@RecordBuilder` and a builder will be generated.

```java
@RecordBuilder
public record SimpleRecord(char c, int i, String s) {
}
```

Use the builder:

```java
SimpleRecord simpleRecord = SimpleRecordBuilder.builder()
        .c('a')
        .i(10)
        .s("whatever")
        .build();
```

A "Wither" is also generated which can be added to the record:

```java
@RecordBuilder
public record SimpleRecord(int i, String s) implements SimpleRecordBuilder.With {
}
```

The wither provides several ways of modifying one or more record components:

```java
SimpleRecord simpleRecord = ...;

// change one value
SimpleRecord altered = simpleRecord.withS("newValue");

// change multiple values
SimpleRecord altered = simpleRecord.with(builder -> builder.s("new s").i(101));
```

Note: you can disable Wither generation via the `enableWither` option.
