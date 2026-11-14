# Getting started

This page takes you from a grammar to an Abstract Syntax Model (ASM) in a few lines of Kotlin.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_GettingStarted.kt`.

## Add the dependency

```kotlin
// build.gradle.kts, for a Kotlin Multiplatform or JVM project
dependencies {
    implementation("net.akehurst.language:agl-processor:<version>")
}
```

The version is shown on the Maven Central badge in the [README](../../README.md).
For JavaScript, use the npm package `net.akehurst.language-agl-processor`.

## The example language

The DSL pages all use this small language. It has circles, squares and connections between them.

```agl
namespace example
grammar Shapes {
    skip leaf WS = "\s+" ;
    skip leaf COMMENT = "//[^\n]*" ;

    unit = item* ;
    item = shape | connection ;
    shape = circle | square ;
    circle = 'circle' NAME radius? ;
    radius = 'r' NUMBER ;
    square = 'square' NAME '{' [point / ',']* '}' ;
    point = NUMBER NUMBER ;
    connection = 'connect' source 'to' target ;

    leaf source = NAME ;
    leaf target = NAME ;
    leaf NAME = "[a-z]+" ;
    leaf NUMBER = "[0-9]+" ;
}
```

A sentence in it:

```
circle c r 5
square s { 1 2, 3 4 }  // a comment
connect c to s
```

## Create a processor

```kotlin
val result = Agl.processorFromStringSimple(GrammarString(grammar))
check(result.issues.errors.isEmpty()) { result.issues.toString() }
val processor = result.processor!!
```

`processorFromStringSimple` also takes the other DSLs as optional arguments
(`typeStr`, `transformStr`, `referenceStr`, `styleStr`, `formatterStr`, `completionStr`).
A processor is reusable. Build it once, then use it for many sentences.

## Parse

`parse` checks the sentence and gives a parse tree (`sppt`, a Shared Packed Parse Tree).

```kotlin
val parseResult = processor.parse(sentence)
```

## Process

`process` parses, builds the ASM (syntax analysis), then runs semantic analysis (for example resolving references).

```kotlin
val processResult = processor.process(sentence)
println(processResult.asm!!.asString())
```

```
:Unit {
  item = [
    :Circle {
      name = 'c'
      radius = :Radius {
        number = '5'
      }
    }
    :Square {
      name = 's'
      point = [
        :Point {
          number = '1'
          number2 = '2'
        }
        :Point {
          number = '3'
          number2 = '4'
        }
      ]
    }
    :Connection {
      source = 'c'
      target = 's'
    }
  ]
}
```

## Errors

Every result has `issues` (`allIssues` for `process`). Each issue has a kind, a phase, a message and a location.

```kotlin
val error = processor.parse("circle 5").issues.errors.first()
error.message          // Failed to match {<GOAL>} at: circle ^5
error.location?.line   // 1
error.location?.column // 8
```

## What the default ASM looks like

Without an [asm-transform](dsl/asm-transform.md), AGL makes the types from the grammar:

- Each non-leaf rule becomes a type, named in UpperCamelCase (`circle` becomes `Circle`).
- Each item in a rule becomes a property, named after the rule it refers to (`NAME` becomes `name`).
  When a name is used twice, the second gets a number (`number`, `number2`).
- Literals (`'circle'`) are not stored.
- A leaf becomes a `String`, a list becomes a `List`, an optional item becomes nullable.
- A rule that is a choice of rules (`shape = circle | square`) becomes a supertype of its alternatives.

`processor.typesDomain.asString()` shows the generated types,
and `processor.transformDomain.asString()` shows the generated asm-transform.

## Next

- The [DSL pages](Overview.md#the-agl-dsls) describe each part of a language definition.
- `processor.expectedItemsAt(sentence, position)` gives code completion, see [completion](dsl/completion.md).
- `processor.format(sentence)` prints a sentence from its ASM, see [format](dsl/format.md).
