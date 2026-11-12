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
| `r: Type` | keep the default properties, change only the type (needs `#override-default-transform`) |

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
| `#override-default-transform` | start from the transform generated from the grammar: your rules replace those rules, all other rules stay as generated. Without it, the transform has only your rules. |
| `#create-missing-types` | create the types (and their properties) that your rules name. Without it, every type must already exist (generated, or from [types](types.md)). |

`asm-transform B : A { }` extends transform `A`. `import-types ns` makes the types of namespace `ns` available.
