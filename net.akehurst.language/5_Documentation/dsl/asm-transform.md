# ASM transform

An asm-transform says how syntax analysis turns each grammar rule's parse node into an object of the ASM.
Without one, AGL generates it from the grammar, see
[What the default ASM looks like](../GETTING_STARTED.md#what-the-default-asm-looks-like).
`processor.transformDomain.asString()` prints the generated transform in this DSL, which is a good starting point.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_AsmTransform.kt`.

## Example

For the [Shapes language](../GETTING_STARTED.md#the-example-language), make a connection a `Link` with `from` and `to`,
and make a radius a plain `String`:

```agl
#override-default-transform
#create-missing-types
namespace example
asm-transform Shapes {
    connection: Link() {
        from := child[1]
        to := child[3]
    }
    radius: child[1] as String
}
```

```kotlin
val processor = Agl.processorFromStringSimple(GrammarString(grammar), transformStr = AsmTransformString(transform)).processor!!
processor.process("circle c r 5 connect c to c").asm
```

```
:Unit {
  item = [
    :Circle {
      name = 'c'
      radius = '5'
    }
    :Link {
      from = 'c'
      to = 'c'
    }
  ]
}
```

The namespace and name are those of the grammar (`example` and `Shapes`). Each rule is `grammarRuleName: expression`.

## Rules

| Rule | Meaning |
|---|---|
| `r: Type() { p := expr ... }` | create a `Type` and set its properties |
| `r: tuple { p := expr ... }` | create a tuple |
| `r: expr as Type` | the value of `expr`, with type `Type` |
| `r: Type` | keep the default rule, give the type of `r` the new name `Type` (needs `#override-default-transform`), see below |
| `r: { Type -> p := expr ... }` | modify the object of the first item (`child[0]`) by setting its properties, the result is that object, `Type` is the type of `r` |

### Change only the type

`r: Type` renames the type of `r` to `Type`, everywhere the default transform uses it, and keeps the default rule.
For `circle: Disc` a circle gives a `Disc` with the default properties.
For a choice of rules, e.g. `item: Thing` for `item = circle | square ;`, `Thing` is the supertype (or the union) of `Circle` and `Square`.
`Type` must be a simple name, and not the type of another rule.
It is an error for a rule whose default type is not its own, e.g. a leaf or a choice of literals gives a `String`:
use `child[0] as Type` or `Type() { ... }` instead.

### Modify the object of the first item

A modify rule lets the first item of a rule create the object (for example through a choice),
and the following items add properties to it:

```agl
#override-default-transform
#create-missing-types
namespace example
asm-transform Shapes {
    coloured: { Shape -> colour := child[1] }
}
```

For the grammar rules `coloured = shape colour ;  shape = circle | square ;`,
`circle c red` gives a `:Circle { id = 'c'  colour = 'red' }`.
`Shape` (the type of `coloured`) and its property `colour` must exist, or be created with `#create-missing-types`.

The expressions are written in the [expressions](expressions.md) language. In a rule, these names refer to the parse node:

| Name | Meaning |
|---|---|
| `child[i]` | the value of the i-th item of the rule (counting literals, from 0) |
| `children` | the values of all items, e.g. of a list |
| `$matchedText` | the text the rule matched |
| `$alternative` | which alternative of a choice matched |

## Options

Options go before `namespace` (for the whole file) or at the start of an `asm-transform` block.

| Option | Meaning |
|---|---|
| `#override-default-transform` | start from the transform generated from the grammar: your rules replace those rules, all other rules stay as generated. Without it, the transform has only your rules, so every non-leaf, non-skip grammar rule needs one (a missing one is an error). |
| `#create-missing-types` | create the types (and their properties) that your rules name. Without it, every type must already exist (generated, or from [types](types.md)). |

`asm-transform B : A { }` extends transform `A`. `import-types ns` makes the types of namespace `ns` available.
