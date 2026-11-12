# M2M transform

An m2m-transform turns objects of one model (a *domain*) into objects of another.
Each domain is described by a [types](types.md) domain. A rule matches objects in one domain with a
*template*, binds variables, and creates or matches objects in the other domain.
Expressions in rules use the [expressions](expressions.md) DSL.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_M2mTransform.kt`.

The examples transform the ASM of the [Shapes language](../GETTING_STARTED.md#the-example-language),
for the sentence `circle c r 5  square s { 1 2, 3 4 }  circle d r 50  connect c to s`,
into this *Drawing* types domain:

```
namespace drawing
  data Drawing { cmp discs: List<Disc> }
  data Disc { label: String  size: String? }
```

## A first transform

```
namespace example
transform ShapesToDrawing(s:Shapes, d:Drawing) {
  top mapping CircleToDisc {
    domain s c:Circle { name == n  radius == Radius { number == r } }
    domain d disc:Disc := Disc() { label := n  size := r }
  }
}
```

* `transform Name(s:Shapes, d:Drawing)` names the transform and its domains: `s` uses the types domain `Shapes`, `d` uses `Drawing`.
* A `top` rule is applied to every input object. Other rules run only when another rule calls them (see [Calling rules](#calling-rules)).
* `domain s c:Circle { ... }` is a template. It matches an object of type `Circle`, and calls it `c`.
* `name == n` matches the property `name`. If `n` is not yet bound, it is bound to the value of `name`; otherwise the value must equal `n`.
* `radius == Radius { ... }` matches a nested object; if `radius` has no value, the template does not match. `prop == 'text'` matches a literal value.
* Variables are bound separately for each match.
* `domain d disc:Disc := Disc() { label := n }` (a *mapping* only) creates the target object and sets its properties with `:=`.
* Any expression works on the right, e.g. `domain d l:String := 'circle ' + n`.

Result, one `Disc` per `Circle`:

```
:Disc { label = 'c'  size = '5' }
:Disc { label = 'd'  size = '50' }
```

## Running a transform

```kotlin
val processor = Agl.processorFromStringSimple(GrammarString(grammar)).processor!!
val shapes = processor.typesDomain
val drawing = TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(drawingTypes)).asm!!
val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
val ogS = objectGraphSimpleAsm(shapes, null, issues, LocationMapDefault())
val ogD = objectGraphSimpleAsm(drawing, null, issues, LocationMapDefault())

val unit = processor.process(sentence).asm!!.root.first() as AsmStructure
val input = (unit.getProperty(PropertyValueName("item")) as List<Any>).map { ogS.toTypedObject(it, StdLibDefault.AnyType) }

val result = Agl.transform(
    M2mTransformString(transform),
    typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
    accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),   // how to read and create objects
    domains = mapOf(DomainReference("s") to input),                      // the input objects
    ruleArguments = emptyMap(),
    targetDomainReference = DomainReference("d")                         // the domain to create
)
result.targets   // the created objects
result.issues
```

`Agl.transformSuspend` does the same, and uses the suspending functions of the accessors (`getPropertySuspend`, ...).

## Mapping and relation

A `mapping` goes one way: it creates the target with `:=`.
A `relation` has a template on every side and works in either direction; the target is chosen when you run it.

```
top relation CircleDisc {
  domain s c:Circle { name == n }
  domain d disc:Disc { label == n }
}
```

With target `d` it creates a `Disc` for each `Circle`; with target `s` (and the discs as input) it creates a `Circle` for each `Disc`.

## when

A `when` condition must hold, or the rule is not applied (an INFORMATION issue says so):

```
top mapping CircleToDisc {
  domain s c:Circle { name == n }
  domain d disc:Disc := Disc() { label := n }
  when { n == 'c' }
}
```

Result: only `:Disc { label = 'c' }`.

## table

A `table` is a rule given as a list of values, one column per domain. Call it from `where`:

```
top mapping CircleToDisc {
  domain s c:Circle { name == n  radius == Radius { number == r } }
  domain d disc:Disc := Disc() { label := n  size := sz }
  where { map SizeName { s := r  d := sz } }
}
table SizeName {
  domain s :String   domain d :String
  values '5'    to   'small'
  values '50'   to   'big'
}
```

Result: `:Disc { label = 'c'  size = 'small' }` and `:Disc { label = 'd'  size = 'big' }`.
A `table` can be `top`, and is used like a relation, in both directions.

## Calling rules

`where { ... }` calls other rules after the templates have matched:

* `map Rule { s := x  d := y }` calls a mapping (or table), `relate Rule { ... }` calls a relation.
* `map all Rule { s := xs  d := ys }` calls it for every element of the collection `xs`, and `ys` is the list of results.
* A rule can have parameters, `mapping R(prefix:String) { ... }`, passed as `map R(prefix := 'disc-') { ... }`.

```
top mapping UnitToDrawing {
  domain s u:Unit { item == items }
  domain d dr:Drawing := Drawing() { discs := ds }
  where { map all CircleToDisc(prefix := 'disc-') { s := items  d := ds } }
}
mapping CircleToDisc(prefix:String) {
  domain s c:Circle { name == n }
  domain d disc:Disc := Disc() { label := prefix + n }
}
```

Result: one `Drawing` with the discs `disc-c` and `disc-d`. Items that are not a `Circle` are skipped.

## Abstract rules

An `abstract` rule gives only the domain signatures. Rules that extend it (`: Name`) give the templates.
An abstract `top` rule is applied through whichever of its extending rules match:

```
abstract top rule ShapeToLabel {
  domain s sh:Shape
  domain d l:String
}
mapping CircleLabel : ShapeToLabel {
  domain s sh:Circle { name == n }
  domain d l:String := 'circle ' + n
}
mapping SquareLabel : ShapeToLabel {
  domain s sh:Square { name == n }
  domain d l:String := 'square ' + n
}
```

Result: `'circle c'`, `'circle d'`, `'square s'`.

## Collection templates

`prop == [ ... ]` matches a collection. `...` means "and other elements"; each element that matches the
template gives a separate match:

```
top mapping SquarePoint {
  domain s sq:Square { name == n  point == [ ... p:Point { number == x } ] }
  domain d l:String := n + ' has x=' + x
}
```

Result: `'s has x=1'` and `'s has x=3'`. Without `...`, the collection must match exactly.
A collection can be named, `point == ps:[ ... ]`.

## Other parts

* `pivot n:String` (in a relation) declares a variable that is matched in every domain, e.g. the name that links a `Circle` and its `Disc`:

  ```
  top relation CircleDisc {
    pivot n:String
    domain s c:Circle { name == n }
    domain d disc:Disc { label == n }
  }
  ```
* `import-types ns` (first in the transform, after any options) imports the types used in the transform; `import ns` (in the namespace) imports other transforms.
* `option name = expression` sets a transform option. No options are defined yet.
* `transform-test` writes tests for a transform: each `test-case` gives an object for each domain.
  AGL parses them but does not run them yet.

  ```
  namespace example
  transform-test ShapesToDrawingTest(s:Shapes, d:Drawing) {
    test-case OneCircle {
      domain s := Circle() { name := 'c' }
      domain d := Disc() { label := 'c' }
    }
  }
  ```
