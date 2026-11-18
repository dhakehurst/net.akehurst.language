# M2M transform test

A transform test gives, for an [m2m-transform](m2m-transform.md), test cases: each test case gives an object for each
domain of the transform. Transform tests are written in their own files, with the extension `.agl-mtt`
(`M2mTransformTest.FILE_EXTENSION`). The values are written with the [expressions](expressions.md) DSL.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_M2mTransformTest.kt`.

```
namespace example.test
  import example
transform-test ShapesToDrawingTest for ShapesToDrawing {
  test-case OneCircle {
    domain s := Circle() { name := 'c' }
    domain d := Disc() { label := 'c' }
  }
}
```

* `transform-test Name for Transform { ... }` names the test and the transform it tests. A qualified name is used as
  it is; a simple name is looked for in the namespace of the test, then in each imported namespace, in order.
* `test-case Name { ... }` gives at least two domains; `domain s := expression` gives the object of domain `s`.
  Each domain must be a domain of the transform, and is given at most once.
* Names of transform tests in a namespace, and of test cases in a transform test, must be unique.

## Processing transform tests

The semantic analysis needs a sentence context that holds the transforms that are tested:

```kotlin
val m2m = Agl.registry.agl.m2mTransform.processor!!.process(transformText).asm!!
val result = Agl.registry.agl.m2mTransformTest.processor!!.process(
    testText,
    Agl.options { semanticAnalysis { sentenceContext(contextFromM2mTransformDomain(m2m)) } }
)
val test = result.asm!!.allTransformTest.first()
test.transform.resolved   // the transform under test
test.domainParameters     // its domains
```

AGL does not run the test cases yet.
