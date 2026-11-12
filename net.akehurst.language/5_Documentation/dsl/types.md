# Types

The types DSL describes the types of a model: the ASM built by syntax analysis, or any model used by
[format](format.md), [expressions](expressions.md) or [m2m-transform](m2m-transform.md).
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Types.kt`.

## Types generated from a grammar

A processor generates its types from the grammar, see
[What the default ASM looks like](../GETTING_STARTED.md#what-the-default-asm-looks-like).
`processor.typesDomain` gives them; `processor.typesDomain.asString()` prints them in this DSL.

## Giving a processor your own types

Pass a types string as `typeStr`. A namespace named after the grammar's qualified name (`example.Shapes`)
is used by the default transform: the properties you declare are kept, the missing ones are generated.

```kotlin
val types = """
    namespace example.Shapes
      data Circle { name: String  colour: String? }
"""
val processor = Agl.processorFromStringSimple(GrammarString(grammar), typeStr = TypesString(types)).processor!!
// Circle has the properties name, colour and (generated) radius
```

## Example

```agl
namespace shapes.model
  data Model { cmp items: List<Item> }
  interface Item
  data Circle : Item { name: String  radius: Integer? }
  data Square : Item { name: String }
  data Link : Item { ref from: Item  ref to: Item }
  enum Colour { RED, GREEN }
  value Size(val n: Integer)
  union Number { Integer | Real }
  data Pair2<A,B> { a: A  b: B }
```

```kotlin
val typesDomain = Agl.registry.agl.types.processor!!.process(types).asm!!
```

## Syntax

| Definition | Meaning |
|---|---|
| `primitive Name` | a primitive type |
| `singleton Name` | a type with one instance |
| `enum Name { A, B }` | an enumeration |
| `value Name(val p: T)` | a value type with one constructor parameter |
| `collection Name<E>` | a collection type |
| `data Name<T> : Super { constructor(...) properties }` | a class with properties |
| `interface Name : Super { properties }` | an interface |
| `union Name { A \| B }` | one of the listed types |
| `association { end a: A  end b: B }` | a two-way link between two types |

A property is `characteristics name: Type`, where `Type?` is nullable and `List<T>` is a list.

| Characteristic | Meaning |
|---|---|
| `cmp` / `ref` | composite (the object owns the value) / reference |
| `val` / `var` | read only / read and write |
| `str` / `drv` | stored / derived (stored is the default) |
| `idn` | part of the identity |

The standard types (`String`, `Integer`, `Real`, `Boolean`, `Any`, `List`, `Set`, `Map`, ...) are in the namespace `std`.
A name that is not an identifier is written in backticks, for example `` `$value` ``.
