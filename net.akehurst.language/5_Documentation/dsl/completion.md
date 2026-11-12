# Completion

`processor.expectedItemsAt(sentence, position)` gives the items that may come next at `position` (code completion).
By default the items come from the grammar. A completion definition changes them.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Completion.kt`.

## Without a completion definition

For the [Shapes language](../GETTING_STARTED.md#the-example-language), after `circle c `:

```kotlin
processor.expectedItemsAt("circle c ", 9).items
```

| kind | label | text |
|---|---|---|
| SEGMENT | item | `<item>` |
| SEGMENT | radius | `<radius>` |
| LITERAL | 'circle' | `circle` |
| LITERAL | 'connect' | `connect` |
| LITERAL | 'r' | `r` |
| LITERAL | 'square' | `square` |

`text` is what to insert; `<name>` is a placeholder for a rule.
The `depth` option expands rules into their content: with `Agl.options { completionProvider { depth(1) } }`
the `radius` item has the text `r <NUMBER>`.
`expectedTerminalsAt` gives only the terminals.

## Example

```agl
namespace example
completion Shapes {
    item -> hide
    radius -> decorate { label := 'radius' description := 'the radius of the circle' }
    in circle NAME -> add [ CompletionItem() { text := 'c1' description := 'a circle name' } ]
}
```

```kotlin
val processor = Agl.processorFromStringSimple(GrammarString(grammar), completionStr = CompletionString(completion)).processor!!
```

After `circle c ` the `item` entry is gone and `radius` has a description.
After `circle ` the items are `c1` and `<NAME>`.

## Syntax

The rules of every `completion` block apply (the name after `completion` is only a label). Each rule is `[in <context rule>] <rule> -> <action>`.
With `in`, the rule applies only when `<rule>` is expected inside `<context rule>`.

| Action | Meaning |
|---|---|
| `hide` | do not offer this item |
| `add [ items ]` | offer these items as well |
| `replace [ items ]` | offer these items instead |
| `decorate { label := '..' description := '..' }` | change the label or description of the item |
| `depth N` | expand this rule N levels deep |

An item is `CompletionItem() { text := '..' label := '..' description := '..' kind := '..' }`.
`text` is required, `label` defaults to `text`, `kind` defaults to `SEGMENT`
(other kinds: `LITERAL`, `PATTERN`, `REFERRED`).
