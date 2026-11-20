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

## Literals and expressions

```agl
namespace example
completion Shapes {
    in square '{' -> add [ CompletionItem() { label := 'points' text := '{ <point> }' } ]
    'r' -> decorate { label := 'radius' description := 'the radius of the ' + $rules.first }
}
```

After `square s ` the items are `{ <point> }` (labelled `points`) and `{`.
After `circle c ` the `r` item has the label `radius` and the description `the radius of the circle`.

## Syntax

The rules of every `completion` block apply (the name after `completion` is only a label). Each rule is `[in <context rule>] <rule> -> <action>`.
With `in`, the rule applies only when `<rule>` is expected inside `<context rule>`.
A choice rule is never a context (it does not enclose a position), use its alternatives instead.
Instead of a rule name, `<rule>` can be a quoted literal, e.g. `'{'`: the rule then applies to every expected literal with that text.

| Action | Meaning |
|---|---|
| `hide` | do not offer this item |
| `add [ items ]` | offer these items as well |
| `replace [ items ]` | offer these items instead |
| `decorate { label := '..' description := '..' }` | change the label or description of the item |
| `depth N` | expand this rule N levels deep |

An item is `CompletionItem() { text := .. label := .. description := .. kind := '..' }`.
`text` is required, `label` defaults to `text`, `kind` defaults to `SEGMENT`
(other kinds: `LITERAL`, `PATTERN`, `REFERRED`).

The values of `text`, `label` and `description` (in an item or in `decorate`) are [expressions](expressions.md) that give a String.
They are evaluated each time the items are offered. In them

| Name | Value |
|---|---|
| `$expected` | the rule name (or the literal text) that the completion rule is for |
| `$rules` | the names of the rules that enclose the position, innermost first |
| `$self` | in `decorate`: the offered item, with `kind`, `label`, `text`, `description` |

An item whose `text` does not give a String is not offered; a `decorate` value that does not give a String leaves the value as it was.

When an editor asks for an offered item to be expanded further (the `path` option), the rules apply to the items of that expansion in the same way.
There, `add` and `replace` add their items when an item of the expansion is for their rule.
