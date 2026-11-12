# Format

A format definition prints an ASM as text: the opposite of parsing (model-to-text).
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Format.kt`.

## Example

For the [Shapes language](../GETTING_STARTED.md#the-example-language):

```agl
namespace example
format Shapes {
    Unit -> "$[item sep $EOL]"
    Circle -> "circle $name${radius}"
    Radius -> " r $number"
    Square -> "square $name { $[point sep ', '] }"
    Point -> "$number $number2"
    Connection -> "connect $source to $target"
}
```

```kotlin
val processor = Agl.processorFromStringSimple(GrammarString(grammar), formatterStr = FormatString(format)).processor!!
processor.format("circle   c r 5 circle d square s{1 2,3 4}  connect c to s").sentence
```

```
circle c r 5
circle d
square s { 1 2, 3 4 }
connect c to s
```

`processor.formatAsm(asm)` formats an ASM you already have.
The processor uses the format set whose qualified name is the target grammar's (`example.Shapes` here).

## Syntax

Each rule is `TypeName -> expression`. The expression is usually a template string.

| In a template `"..."` | Meaning |
|---|---|
| text | copied as it is (`\"` for a quote) |
| `$prop` | the property `prop` of the object, formatted by its own rule if it is an object |
| `${expr}` | any [expression](expressions.md); an absent (null) value prints nothing |
| `$[list sep ', ']` | each element of `list`, with the separator between them |
| `$EOL` | a new line |
| `${expr via F}`, `$[list sep ',' via F]` | format with the rules of format set `F` |

A rule can also be any expression, for example `when { cond -> "..." else -> "..." }`.
`format B : A { }` extends format set `A`.
