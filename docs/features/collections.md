[◀︎ RecordBuilder](../../README.md) • [◀︎ Features](../features.md) • Collections

# Collections

RecordBuilder's default behavior does not treat Java collection components specially. Use the following
options to add specialized behavior.

## useImmutableCollections

Adds special handling for record components of type `java.util.List`, `java.util.Set`, 
`java.util.Map` and `java.util.Collection`. When the record is built, any components of these 
types are passed through an added shim method that uses the corresponding immutable 
collection (e.g. `List.copyOf(o)`) or an empty immutable collection if the component is `null`.

Example:

```java
@RecordBuilder
@RecordBuilder.Options(useImmutableCollections = true)
public record MyRecord<T, X extends Point>(List<T> l, Set<T> s, Map<T, X> m, Collection<X> c) {}
```

The build method of the generated builder is:

```java
public MyRecord<T, X> build() {
    l = __list(l);
    s = __set(s);
    m = __map(m);
    c = __collection(c);
    return new MyRecord<T, X>(l, s, m, c);
}

// __list et al are defined in the builder as:
private static <T> List<T> __list(Collection<? extends T> o) {
    return (o != null) ? List.copyOf(o) : List.of();
}
```

## useUnmodifiableCollections

Adds special handling for record components of type: `java.util.List`, `java.util.Set`, 
`java.util.Map` and `java.util.Collection`. When the record is built, any components of 
these types are passed through an added shim method that uses the corresponding unmodifiable 
collection (e.g. `Collections.unmodifiableList(o)`) or an empty immutable collection if the component is `null`.
For backward compatibility, when [useImmutableCollections](#useimmutablecollections) returns `true`, this 
property is ignored.

Example:

```java
@RecordBuilder
@RecordBuilder.Options(useUnmodifiableCollections = true)
public record MyRecord(List<Integer> aList, Set<String> orderedSet, Map<String, Integer> orderedMap,
        Collection<String> aCollection) {}
```

The build method of the generated builder is:

```java
public MyRecord build() {
    aList = __list(aList);
    orderedSet = __set(orderedSet);
    orderedMap = __map(orderedMap);
    aCollection = __collection(aCollection);
    return new MyRecord(aList, orderedSet, orderedMap, aCollection);
}

// __list et al are defined in the builder as:
private static <T> List<T> __list(Collection<? extends T> o) {
    return (o != null) ? Collections.<T>unmodifiableList((List<T>) o) : Collections.<T>emptyList();
}
```

## addSingleItemCollectionBuilders

When enabled, collection types (`List`, `Set` and `Map`) are handled specially. The setters for these 
types now create an internal collection and items are added to that collection. Additionally, 
"adder" methods prefixed with singleItemBuilderPrefix() are created to add single items to these collections.

Note: you should also enable [useImmutableCollections](#useimmutablecollections) 

Example:

```java
@RecordBuilder
@RecordBuilder.Options(addSingleItemCollectionBuilders = true, useImmutableCollections = true)
public record MyRecord<T>(List<? extends String> strings, Set<? extends List<? extends T>> sets,
        Map<? extends Instant, ? extends T> map, Collection<? extends T> collection) {}
```

The setters and build method of the generated builder is:
   
```java     
// setters in the builder are defined as:
public MyRecordBuilder<T> strings(List<? extends String> strings) {
    this.strings = __list(strings);
    return this;
}

// build in the builder is defined as:
public MyRecord<T> build() {
    strings = __list(strings);
    sets = __set(sets);
    map = __map(map);
    collection = __collection(collection);
    return new MyRecord<T>(strings, sets, map, collection);
}

// __list et al are defined in the builder as:
private static <T> List<T> __list(Collection<? extends T> o) {
    return (o != null) ? List.copyOf(o) : List.of();
}
```
