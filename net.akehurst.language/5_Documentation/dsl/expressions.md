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
| `set.size`, `.isEmpty`, `.map(f)`, `.filter(f)` | properties and methods of any collection (`List`, `Set`, `OrderedSet`, `Map`); an `OrderedSet` also has `.first` and `.last` |
| `list.foldRight(initial, { x, acc -> expr })` | accumulates a value from the last element to the first, as Kotlin `foldRight`; e.g. nested objects from a list: `l.front.foldRight(l.last, { x, acc -> N() { v := x  n := acc } })` |
| `+ - * / %`, `== != < <= > >=`, `and or xor` | infix operators, Kotlin precedence (tightest first): `* / %`, `+ -`, `< <= > >=`, `== !=`, `and`, `xor`, `or`; all left associative. Precedence is applied when the expression is evaluated or typed; the ASM keeps the chain as written |
| `c ? a : b` | `a` if `c` is true, else `b` |
| `when { c1 -> e1 ... else -> e }` | first true condition wins |
| `{ x -> expr }` | lambda, e.g. for `map` and `filter` |
| `{ x, acc -> expr }` | lambda with two parameters, for `foldRight` |
| `with(e) expr` | evaluate `expr` with `e` as the current object |
| `e as T`, `e is T` | cast, type test |
| `T() { p := e ... }` | create an object of type `T` and set its properties |
| `tuple { a := 1 b := 'x' }` | create a tuple |
| `{ x := e ... expr }` | block with local variables |
| `$nothing` | no value |
| `e.$type` | the runtime type of `e`, see [Reflection](#reflection) |

A property that is not set reads as `$nothing` if it is nullable, and as an empty collection if its type is a collection
(`List`, `Set`, `OrderedSet`, `Map`), e.g. a metamodel property of multiplicity 0..* that the sentence gives no elements for
(see `test_ReferenceResolverSimple_unsetCollection`); reading any other unset property is an error.

Special names: `$self` (current object), `$nothing`, `.$type` (of every value); in [asm-transform](asm-transform.md) also `child`, `children`,
`$matchedText`, `$alternative`; in [references](references.md) also `$container`, `$owner`, `$root`, `$previous`; in [format](format.md) `$EOL`.

## Reflection

`e.$type` is the runtime type of the value of `e`: the type of the object itself, not the type it is declared or cast as
(for a `Circle` in a `List<Item>`, `items[0].$type.name` and `(items[0] as Item).$type.name` are both `'Circle'`).
It is a value of the std interface `Type`, whose properties are values of the std interface `Property`
(both are given by `.$type`, they cannot be created):

```
namespace std
  interface Type {
    val drv name: String
    val drv qualifiedName: String
    val drv isNullable: Boolean
    val drv typeArgument: List<Type>
    val drv supertype: List<Type>                   // direct supertypes
    val drv allSupertype: List<Type>                // all (transitive) supertypes
    val drv property: List<Property>                // own and inherited
    val drv isSpecial: Boolean                      // Any, Nothing
    val drv isSingleton: Boolean
    val drv isPrimitive: Boolean
    val drv isEnum: Boolean
    val drv isValue: Boolean
    val drv isUnion: Boolean
    val drv isTuple: Boolean
    val drv isCollection: Boolean
    val drv isData: Boolean
    val drv isInterface: Boolean
    fun findPropertyByName(name: String): Property?  // $nothing if there is none
    fun conformsTo(other: Type): Boolean
  }
  interface Property {
    val drv name: String
    val drv type: Type
    val drv owner: Type                             // the type that declares it (without type arguments)
    val drv isComposite: Boolean
    val drv isReference: Boolean
    val drv isReadOnly: Boolean
    val drv isDerived: Boolean
    fun getValue(obj: Any): Any?                    // the value of this property of obj, $nothing if obj has none
  }
```

For example `$self.$type.property.map { p -> p.name }`, `x.$type.findPropertyByName('radius').getValue(x)`,
`when { p.type.isCollection -> ... }`. `$nothing.$type` is `$nothing` (with a warning).
The same works in [m2m transforms](m2m-transform.md), on source and target objects.
A type name is found in the language's own namespaces before `std`, so a language with its own `Type` (e.g. KerML)
writes `std.Type` for the reflection type.
The examples are tested in `agl-processor/src/commonTest/kotlin/expressions/test_ExpressionsInterpreter_reflection.kt`.

## References

A property that refers to another element (see [references](references.md)) holds a reference.
Reading it gives the element it refers to: `l.target` is the referred element, and `l.target.id` its `id`.
Put `&` in front of the property name to get the reference itself: `l.&target` (or `&target` for a property of the current object).
In a chain, `&` applies to the one property it is written in front of: `a.&b.c` reads `b` as a reference and then navigates to `c` of the element it refers to.
Reading an unresolved reference without `&` gives `$nothing`; with `&` it gives the (unresolved) reference.
A list that holds references (e.g. one created by a transform) gives the referred elements, without the unresolved ones; with `&` it gives the list of references.

The [references](references.md) and [format](format.md) processors work on the referring side, so they read the last property of their expressions with `&`
(a reference is formatted as the text that refers to the element).
The examples are tested in `agl-processor/src/commonTest/kotlin/expressions/test_ExpressionsInterpreter_asmReference.kt`.

A file can define functions: `fun name(p: Type): Type = expression`.
