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

A `when` can also require that another rule was already applied: `related R { ... }` for a relation,
`mapped R { ... }` for a mapping. Give a value for each source domain and a variable for the target domain;
the variable is bound to what `R` produced. `related all` / `mapped all` require it for every element of a collection.

```
top relation CircleDisc {
  domain s c:Circle { name == n }
  domain d disc:Disc { label == n }
}
top mapping CircleToLabel {
  domain s c:Circle { name == n }
  domain d l:String := 'disc ' + x.label
  when { related CircleDisc { s := c  d := x } }
}
```

Result: the discs `c` and `d`, and `'disc c'`, `'disc d'`.

If `R` has not been applied yet for those values, it is applied first, so the order of the `top` rules does not matter.

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

The target and the `where` calls are done in the order their variables need, in a mapping as in a relation. A `where` call runs before a target expression that uses the variable it binds (`domain d l:String := x  where { map Up { s := n  d := x } }`), and after the target is created when it uses the target (`map Up { s := disc.label  d := x }`). The `{ ... }` properties of a constructor are set once the variables they use are bound. If the target and a `where` call each need a variable the other binds, that is an error.

A rule is applied at most once for the same variable binding, so a `top` rule that is also called from `where` or `when` gives no duplicate targets.

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

An abstract rule is meant to have one extending rule that matches each source; if more than one matches, see below.

## Extending rules

A rule that extends another rule (`: Name`) replaces it wherever it applies. For each source binding, the most specific extending rule
whose templates match and whose `when` holds is applied instead of the extended rule, and its result also counts as a result of the extended rule
(for `where map Name` and `mapped Name`). The extended rule is applied to every other source. This holds wherever the extended rule is applied:
as a `top` rule, or called from `where` (also `map all`) or `when`. The extending rule does not need to be `top`.
It does not inherit the templates of the rule it extends; it gives its own.

```
top mapping AnyLabel {
  domain s o:Any {}
  domain d l:String := 'other'
}
mapping CircleLabel : AnyLabel {
  domain s c:Circle { name == n }
  domain d l:String := 'circle ' + n
}
```

Result: `'circle c'`, `'other'`, `'circle d'`, `'other'` (the square and the connector are `'other'`), one result per source object.

If more than one extending rule applies to the same source and none of them extends the others, that is an error and none is applied for that source.

## Collection templates

`prop == [ ... ]` matches a collection. The members of the template are separated by `,`.
`...` is a gap: zero or more elements that no member matches. A gap can be anywhere, and takes the place of the `,`.
Each member matches a different element, and each way the members match gives a separate match:

```
top mapping SquarePoint {
  domain s sq:Square { name == n  point == [ ... p:Point { number == x } ... ] }
  domain d l:String := n + ' has x=' + x
}
```

Result: `'s has x=1'` and `'s has x=3'`.

The elements of a `List` (or `OrderedSet`) are matched in order, and other elements are allowed only where a gap is written:

| Template | Matches |
|---|---|
| `[p, q]` | exactly two elements, `p` then `q` |
| `[p ...]` | `p` is the first element |
| `[... p]` | `p` is the last element |
| `[... p ...]` | `p` is any element |
| `[p ... q]` | `p` is the first and `q` the last element |

A `Set` or other collection has no order: the position of a gap does not matter, and without a gap every element must be matched by a member.
In a bag (a collection that can hold the same value twice) each occurrence is a separate element.

A gap can be named, `name:...`: the name is bound to a collection (of the same type) of the elements in the gap,
e.g. `point == [ p:Point { number == x } rest:... ]` binds `rest` to the points after the first.
A template on an unordered collection can name at most one gap.
A member that is only a variable matches any one element, e.g. `[p, x ...]` is "`p` then at least one more element".
The whole collection can be named too, `point == ps:[ ... ]`.

### Differences from QVT Relations

* QVT names the collection type in the template (`Set(T) { a, b ++ rest }`); here the kind of collection comes from the property type.
  So a `List` cannot be matched as a set (members in any order); write two templates, or name a gap and use `when`.
  An explicit kind, e.g. `set[...]`, could be added later.
* QVT has one rest variable, after the members (`++ rest`), and on a sequence the members match the head of the sequence.
  Here gaps can be anywhere, and there can be several.
* QVT's `_` (any element, ignored) is written as a variable that is not used, or as a gap.

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
* `#name : value` sets a transform option (the option syntax of all agl DSLs). No options are defined yet.
* Tests of a transform are written in their own language, see [m2m-transform-test](m2m-transform-test.md).
