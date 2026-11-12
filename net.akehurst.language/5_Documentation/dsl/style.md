# Style

A style definition gives colours and fonts to parts of a sentence, for syntax highlighting in an editor.
AGL parses and checks the styles; the [AGL editor](https://github.com/dhakehurst/net.akehurst.language.editor) applies them.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Style.kt`.

## Example

For the [Shapes language](../GETTING_STARTED.md#the-example-language):

```agl
namespace example
styles Shapes {
    'circle', 'square', 'connect' {
        foreground: blue;
        font-weight: bold;
    }
    NAME, NUMBER {
        foreground: darkred;
    }
    COMMENT {
        foreground: gray;
        font-style: italic;
    }
}
```

```kotlin
val definition = Agl.languageDefinitionFromStringSimple(
    LanguageIdentity("example.Shapes"),
    GrammarString(grammar),
    styleStr = StyleString(style)
)
definition.styleDomain   // the parsed styles
```

## Syntax

A rule is `selectors { name: value; ... }`. Several selectors are separated by `,`.

| Selector | Matches |
|---|---|
| `'if'` | that literal |
| `"[0-9]+"` | that pattern, as written in the grammar |
| `NAME` | the rule `NAME` |

The declarations are CSS-like `name: value;` pairs; the editor decides which names it supports.
`styles B : A { }` extends style set `A`.
