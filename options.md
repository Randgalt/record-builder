[◀︎ RecordBuilder](README.md) • RecordBuilder Options

# RecordBuilder Options

RecordBuilder supports many options that change the behavior of generated builders. These options are added to over
time and the project is open to suggestions/submissions from users.

Options are specified by adding `@RecordBuilder.Options` annotations on source records. The project's test
files have many examples. 

## Naming

The names used for generated methods, classes, etc. can be changed via the following options:

| option                                                      | details                                                                                                                                                                          |
|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(suffix = "foo")`                    | The builder class name will be the name of the record (prefixed with any enclosing class) plus this suffix.                                                                      |
| `@RecordBuilder.Options(interfaceSuffix = "Foo")`           | Used by {@code RecordInterface}. The generated record will have the same name as the annotated interface plus this suffix.                                                       |
| `@RecordBuilder.Options(copyMethodName = "foo")`            | The name to use for the copy builder.                                                                                                                                            |
| `@RecordBuilder.Options(builderMethodName = "foo")`         | The name to use for the builder.                                                                                                                                                 |
| `@RecordBuilder.Options(buildMethodName = "foo")`           | The name to use for the build method.                                                                                                                                            |
| `@RecordBuilder.Options(fromMethodName = "foo")`            | The name to use for the from-to-wither method.                                                                                                                                   |
| `@RecordBuilder.Options(componentsMethodName = "foo")`      | The name to use for the method that returns the record components as a stream.                                                                                                   |
| `@RecordBuilder.Options(withClassName = "Foo")`             | The name to use for the nested With class.                                                                                                                                       |
| `@RecordBuilder.Options(withClassMethodPrefix = "foo")`     | The prefix to use for the methods in the With class.                                                                                                                             |
| `@RecordBuilder.Options(singleItemBuilderPrefix = "foo")`   | The prefix for adder methods when `addSingleItemCollectionBuilders()` is enabled.                                                                                                |
| `@RecordBuilder.Options(fromWithClassName = "Foo")`         | The `fromMethodName` method instantiates an internal private class. This is the name of that class.                                                                              |
| `@RecordBuilder.Options(mutableListClassName = "Foo")`      | If `addSingleItemCollectionBuilders()` and `useImmutableCollections()` are enabled the builder uses an internal class to track changes to lists. This is the name of that class. |
| `@RecordBuilder.Options(mutableSetClassName = "Foo")`       | If `addSingleItemCollectionBuilders()` and `useImmutableCollections()` are enabled the builder uses an internal class to track changes to sets. This is the name of that class.  |
| `@RecordBuilder.Options(mutableMapClassName = "Foo")`       | If `addSingleItemCollectionBuilders()` and `useImmutableCollections()` are enabled the builder uses an internal class to track changes to maps. This is the name of that class.  |
| `@RecordBuilder.Options(stagedBuilderMethodName = "foo")`   | The name to use for the staged builder if present.                                                                                                                               |
| `@RecordBuilder.Options(stagedBuilderMethodSuffix = "Foo")` | The suffix to use for the staged builder interfaces if present.                                                                                                                  |
| `@RecordBuilder.Options(enableGetters = true)`              | If true, getters will be generated for the Builder class.                                                                                                                        |
| `@RecordBuilder.Options(setterPrefix = "foo")`              | If set, all builder setter methods will be prefixed with this string.                                                                                                            |
| `@RecordBuilder.Options(getterPrefix = "foo")`              | If set, all builder getter methods will be prefixed with this string.                                                                                                            |
| `@RecordBuilder.Options(booleanPrefix = "foo")`             | If set, all boolean builder getter methods will be prefixed with this string.                                                                                                    |
| `@RecordBuilder.Options(onceOnlyAssignmentName = "foo")`    | `onceOnlyAssignment` method instantiates an internal private boolean array. This is the name of that array.                                                                      |

## Withers

| option                                                            | details                                                                                                                            |
|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(enableWither = true/false)`               | The builder class name will be the name of the record (prefixed with any enclosing class) plus this suffix. The default is `true`. |
| `@RecordBuilder.Options(withClassName = "Foo")`                   | The name to use for the nested With class.                                                                                         |
| `@RecordBuilder.Options(withClassMethodPrefix = "foo")`           | The prefix to use for the methods in the With class.                                                                               |
| `@RecordBuilder.Options(addFunctionalMethodsToWith = true/false)` | When enabled, adds functional methods to the nested "With" class. The default is `false`.                                          |
| `@RecordBuilder.Options(fromWithClassName = "Foo")`               | The `fromMethodName` method instantiates an internal private class. This is the name of that class.                                |

## File/Class Generation

| option                                                           | details                                                                                                                                                  |
|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(fileComment = "foo")`                    | Return the comment to place at the top of generated files. Return null or an empty string for no comment.                                                |
| `@RecordBuilder.Options(fileIndent = "    ")`                    | Return the file indent to use.                                                                                                                           |
| `@RecordBuilder.Options(prefixEnclosingClassNames = true/false)` | If the record is declared inside another class, the outer class's name will be prefixed to the builder name if this returns true. The default is `true`. |

## Miscellaneous

| option                                                                     | details                                                                                                                                                          |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(inheritComponentAnnotations = true/false)`         | If true, any annotations (if applicable) on record components are copied to the builder methods. The default is `true`.                                          |
| `@RecordBuilder.Options(publicBuilderConstructors = true/false)`           | Makes the generated builder's constructors public. The default is `false`.                                                                                       |
| `@RecordBuilder.Options(builderClassModifiers = {}})`                      | Any additional `javax.lang.model.element.Modifier` you wish to apply to the builder.                                                                             |
| `@RecordBuilder.Options(beanClassName = "Foo")`                            | If set, the Builder will contain an internal interface with this name.                                                                                           |
| `@RecordBuilder.Options(addClassRetainedGenerated = true/false)`           | If true, generated classes are annotated with `RecordBuilderGenerated`. The default is `false`.                                                                  |
| `@RecordBuilder.Options(addStaticBuilder = true/false)`                    | If true, a functional-style builder is added so that record instances can be instantiated without `new()`. The default is `true`.                                |
| `@RecordBuilder.Options(inheritComponentAnnotations = true/false)`         | If true, any annotations (if applicable) on record components are copied to the builder methods. The default is `true`.                                          |
| `@RecordBuilder.Options(addConcreteSettersForOptional = <mode>)`           | Add non-optional setter methods for optional record components. The default is `ConcreteSettersForOptionalMode.DISABLED`.                                        |
| `@RecordBuilder.Options(nullableAnnotationClass = "com.foo.Nullable")`     | Nullability annotation to use when RecordBuilder needs to add one.                                                                                               |
| `@RecordBuilder.Options(useValidationApi = true/false)`                    | Pass built records through the Java Validation API if it's available in the classpath. The default is `false`.                                                   |
| `@RecordBuilder.Options(builderMode = BuilderMode.XXX)`                    | Whether to add standard builder, staged builder or both. The default is `BuilderMode.STANDARD`.                                                                  |
| `@RecordBuilder.Options(onceOnlyAssignment = true/false)`                  | If true, attributes can be set/assigned only 1 time. Attempts to reassign/reset attributes will throw `java.lang.IllegalStateException`. The default is `false`. |

### Staged Builders

Use `@RecordBuilder.Options(builderMode = BuilderMode.STAGED)` or `@RecordBuilder.Options(builderMode = BuilderMode.STANDARD_AND_STAGED)` to create staged
builders. Staged builders require that each record component is built in order and that each component is specified. The generated builder ensures
this via individual staged builders. See [TestStagedBuilder](record-builder-test/src/test/java/io/soabase/recordbuilder/test/staged/TestStagedBuilder.java) for examples.

A variant of staged builders is available that only stages required record components. Use `BuilderMode.STAGED_REQUIRED_ONLY` or `BuilderMode.STANDARD_AND_STAGED_REQUIRED_ONLY`. 
The following are not staged and are added to the final stage:
- optional components (when `addConcreteSettersForOptional` is enabled)  
- Any collections matching enabled [Collection options](#collections)  
- Any components with initializers (annotated with `@RecordBuilder.Initializer(...)`) when `skipStagingForInitializedComponents` is enabled

## Default Values / Initializers

| option                                                                     | details                                                                                                                                                                                                                                          |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(emptyDefaultForOptional = true/false)`             | Set the default value of `Optional` record components to `Optional.empty()`. The default is `true`.                                                                                                                                              |
| `@RecordBuilder.Options(skipStagingForInitializedComponents = true/false)` | If true, components with initializers (`@RecordBuilder.Initializer`) are excluded from staging when `builderMode` is `STAGED_REQUIRED_ONLY` or `STANDARD_AND_STAGED_REQUIRED_ONLY` and are only included in the final stage. Default is `false`. |

### `@RecordBuilder.Initializer`

You can annotate record components with `@RecordBuilder.Initializer(...)` to specify initializers for the corresponding
field in the generated builder. See [Initialized.java](record-builder-test/src/main/java/io/soabase/recordbuilder/test/Initialized.java)
for an example.

## Null Handling

| option                                                          | details                                                                                                                      |
|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(interpretNotNulls = true/false)`        | Add not-null checks for record components annotated with any null-pattern annotation. The default is `false`.                |
| `@RecordBuilder.Options(interpretNotNullsPattern = "regex")`    | The regex pattern used to determine if an annotation name means non-null.                                                    |
| `@RecordBuilder.Options(allowNullableCollections = true/false)` | Adds special null handling for record collectioncomponents. The default is `false`.                                          |
| `@RecordBuilder.Options(nullablePattern = "regex")`             | Regex pattern to use for `BuilderMode.STAGED_REQUIRED_ONLY` and `BuilderMode.STANDARD_AND_STAGED_REQUIRED_ONLY`.             |
| `@RecordBuilder.Options(defaultNotNull = true/false)`           | Add not-null checks for all record components unless annotated with any non-null-pattern annotation. The default is `false`. |

## Collections

Special handling for collections. See the project test classes for usage.

| option                                                                 | details                                                                             |
|------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| `@RecordBuilder.Options(useImmutableCollections = true/false)`         | Adds special handling for collection record components. The default is `false`.     |
| `@RecordBuilder.Options(useUnmodifiableCollections = true/false)`      | Adds special handling for collection record components. The default is `false`.     |
| `@RecordBuilder.Options(allowNullableCollections = true/false)`        | Adds special null handling for record collectioncomponents. The default is `false`. |
| `@RecordBuilder.Options(addSingleItemCollectionBuilders = true/false)` | Adds special handling for record collectioncomponents. The default is `false`.      |
