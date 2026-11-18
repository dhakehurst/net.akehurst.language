# Expressions

The expression language is used inside the other DSLs (asm-transform, references, format, completion, m2m-transform).
It navigates and builds objects (ASM or Kotlin objects), similar to Kotlin or OCL.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Expressions.kt`.

## Example

On the ASM of the [Shapes sentence](../GETTING_STARTED.md#the-example-language), with `$self` being the `Unit`:

| Expression | Result |
|---|---|
| `item.size` | `3` |
| `item[0].name` | `'c'` |
| `item[0].radius.number` | `'5'` |
| `item[0] is Circle` | `true` |
| `item[2].source + '->' + item[2].target` | `'c->s'` |
| `1 + 2 * 3` | `7` |
| `when { item.size == 3 -> 'three' else -> 'other' }` | `'three'` |
| `with(item[1]) name` | `'s'` |
| `{ x := 2 x * 3 }` | `6` |
| `item.first.name`, `item.last.target` | `'c'`, `'s'` |
| `item.get(1).name` | `'s'` |
| `item[1].point.map({ p -> p.number })` | `['1', '3']` |
| `item.filter({ i -> i is Shape }).size` | `2` |
| `item.size > 2 ? 'many' : 'few'` | `'many'` |
| `item.size > 2 and true` | `true` |
| `'abc'.length` | `3` |

An expression can be evaluated directly from Kotlin:

```kotlin
val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
val objectGraph = objectGraphSimpleAsm(processor.typesDomain, null, issues, LocationMapDefault())
Agl.executeExpression(objectGraph, asm.root[0], "item[0].name").untyped   // "c"
```

For Kotlin objects use `ObjectGraphAccessorMutatorByReflection(typesDomain, issues, locationMap)` instead.

## Syntax

| Syntax | Meaning |
|---|---|
| `'text'`, `12`, `1.5`, `true` | literals |
| `name`, `$self` | a property of the current object, the current object |
| `a.b`, `a.m(x)`, `a[i]` | property, method call, index |
| `a.&b`, `&b` | property `b` read as a reference, see [References](#references) |
| `list.size`, `.isEmpty`, `.first`, `.last`, `.get(i)`, `.map(f)`, `.filter(f)` | list properties and methods |
| `+ - * / %`, `== != < <= > >=`, `and or xor` | infix operators, Kotlin precedence (tightest first): `* / %`, `+ -`, `< <= > >=`, `== !=`, `and`, `xor`, `or` |
| `c ? a : b` | `a` if `c` is true, else `b` |
| `when { c1 -> e1 ... else -> e }` | first true condition wins |
| `{ x -> expr }` | lambda, e.g. for `map` and `filter` |
| `with(e) expr` | evaluate `expr` with `e` as the current object |
| `e as T`, `e is T` | cast, type test |
| `T() { p := e ... }` | create an object of type `T` and set its properties |
| `tuple { a := 1 b := 'x' }` | create a tuple |
| `{ x := e ... expr }` | block with local variables |
| `$nothing` | no value |

Special names: `$self` (current object), `$nothing`; in [asm-transform](asm-transform.md) also `child`, `children`,
`$matchedText`, `$alternative`; in [references](references.md) also `$container`, `$owner`, `$root`, `$previous`; in [format](format.md) `$EOL`.

## References

A property that refers to another element (see [references](references.md)) holds a reference.
Reading it gives the element it refers to: `l.target` is the referred element, and `l.target.id` its `id`.
Put `&` in front of the property name to get the reference itself: `l.&target` (or `&target` for a property of the current object).
In a chain, `&` applies to the one property it is written in front of: `a.&b.c` reads `b` as a reference and then navigates to `c` of the element it refers to.
Reading an unresolved reference without `&` gives `$nothing`; with `&` it gives the (unresolved) reference.

The [references](references.md) and [format](format.md) processors work on the referring side, so they read the last property of their expressions with `&`
(a reference is formatted as the text that refers to the element).
The examples are tested in `agl-processor/src/commonTest/kotlin/expressions/test_ExpressionsInterpreter_asmReference.kt`.

A file can define functions: `fun name(p: Type): Type = expression`.
