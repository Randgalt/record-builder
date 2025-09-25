[◀︎ RecordBuilder](README.md) • Deconstructors

# Deconstructors

Deconstructors have been proposed for a future JDK. RecordBuilder's deconstructor support is based on this proposal. See: https://github.com/openjdk/amber-docs/blob/master/eg-drafts/deconstruction-patterns-records-and-classes.md#deconstructors-for-classes.
Quoting the proposal:

> A deconstruction pattern can be thought of as the dual of a constructor; a constructor takes N state components and 
> aggregates them into an object, and a deconstruction pattern takes an object and decomposes it into N state components. 

JDK `record`s have implicit deconstructors that are used throughout the JDK for pattern matching, etc. However, standard
Java classes do not have deconstructors. RecordBuilder can generate records and builders from deconstructor-style methods in any Java class.

A deconstructor is either: 

1. a method in any class or interface that:
    - returns `void`
    - is not `static`
    - has one or more parameters of type:
      - `Consumer<T>`
      - `IntConsumer`
      - `LongConsumer`
      - `DoubleConsumer`
    - is annotated with `@RecordBuilder.Deconstructor`
2. a class or interface that is:
   - annotated with `@RecordBuilder.Deconstructor`
   - has one or more accessor methods annotated with `@RecordBuilder.DeconstructorAccessor`

When RecordBuilder encounters a deconstructor it generates a `record` that matches the deconstructor's parameters
and, optionally, creates a record builder for it.
The record's components are the same as the implied types of the deconstructor method's parameters or the return types
of the deconstructor accessors. The `record` will have a static method named `from` (you can change this) that
takes an instance of the class containing the deconstructor and returns an instance of the generated `record`.
In other words, it deconstructs the class instance into a data access object that can be used in pattern matching,
etc.

## Example

### Deconstructor method

Given this POJO:

```java
public class MyClass {
    private final int qty;
    private final String name;
    
    public MyClass(int qty, String name) {
        this.qty = qty;
        this.name = name;
    }

    @Deconstructor
    public void deconstructor(IntConsumer qty, Consumer<String> name) {
        qty.accept(this.qty);
        name.accept(this.name);
    }
}
```

RecordBuilder will generate a `record` (and record builder) similar to:

```java
public record MyClassDao(int qty, String name) {
    public static MyClassDao from(MyClass rhs) {
        MyClassDaoBuilder builder = MyClassDaoBuilder.builder();
        rhs.deconstructor(builder::qty, builder::name);
        return builder.build();
    }
}
```

### Deconstructor accessor

Given this POJO:

```java
@Deconstructor
public class MyClass {
    private final int qty;
    private final String name;
    
    private MyClass(int qty, String name) {
        this.qty = qty;
        this.name = name;
    }

    @DeconstructorAccessor
    public int getQty() {
        return qty;
    }

    @DeconstructorAccessor
    public String getName() {
        return name;
    }
    
    // etc.
}
```

RecordBuilder will generate a `record` (and record builder) similar to:

```java
public record MyClassDao(int qty, String name) {
    public static MyClassDao from(MyClass rhs) {
        MyClassDaoBuilder builder = MyClassDaoBuilder.builder();
        builder.qty(rhs.getQty()).name(rhs.getName());
        return builder.build();
    }
}
```


### Usage

You can use it in switch statements, etc.:

```java
MyClass myClass = ...
        
switch (MyClassDao.from(myClass)) {
    case MyClassDao(var qty, var name) when qty > 0 -> ...
    case MyClassDao(var qty, var name) when qty == 0 -> ...
    case MyClassDao(var qty, var name) when name.isEmpty() -> ...
    ... etc. ...
}
```

## Options

The `@RecordBuilder.Deconstructor` annotation has several options that control how the record is generated.
See [the definition](record-builder-core/src/main/java/io/soabase/recordbuilder/core/RecordBuilder.java) for details.

When `addRecordBuilder()` is `true` a RecordBuilder is created for the generated record. You can add
a `@RecordBuilder.Options` annotation to the deconstructor method to control how the record builder is generated.
You can also create a `@DeconstructorTemplate` to build a custom deconstructor annotation. It is created
in the same manner as a custom `@Template` annotation. See [Customizing](customizing.md#create-a-custom-annotation) for details.

A pre-built template, `@DeconstructorFull` can be used which is an analog to `@RecordBuilderFull`.
