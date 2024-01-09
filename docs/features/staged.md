[◀︎ RecordBuilder](../../README.md) • [◀︎ Features](../features.md) • Staged Builder

# Staged Builder

RecordBuilder's staged builder option generates a builder that requires setting each record component
individually. The way that the builder is generated ensures that each component in the record is always
set with a value as you cannot call the final build method until all components are set. Additionally,
the order of components in the builder is consistent and matches the record definition.

```java
@RecordBuilder
@RecordBuilder.Options(builderMode = RecordBuilder.BuilderMode.STAGED)
public record StagedRecord(char c, int i, String s) {
}
```

To use the staged builder:

```java
StagedRecord stagedRecord = StagedRecordBuilder.builder()
        .c('c') // "c" must be set first, there will be no other methods in the builder
        .i(1) // "i" must be set second, there will be no other methods in the builder
        .s("something") // "s" must be set last, there will be no other methods in the builder
        .build();   // the build method is only available after all components are set 
```

## Additional Features

In addition to the `build()` method, there is a `builder()` method that returns a builder instead
of a built record:

```java
StagedRecordBuilder builder = StagedRecordBuilder.builder()
        .c('c')
        .i(1)
        .s("something")
        .builder(); 
```

You can have the both a staged and standard builder as well:

```java
@RecordBuilder
@RecordBuilder.Options(builderMode = RecordBuilder.BuilderMode.STANDARD_AND_STAGED)
public record StagedRecord(char c, int i, String s) {
}

StagedRecordBuilder.builder()           // standard builder
StagedRecordBuilder.stagedBuilder()     // staged builder
```

