# Style

A style definition gives colours and fonts to parts of a sentence, for syntax highlighting in an editor.
AGL parses and checks the styles; the [AGL editor](https://github.com/dhakehurst/net.akehurst.language.editor) applies them.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Style.kt`.

## Example

For the [Shapes language](../GETTING_STARTED.md#the-example-language):

```agl
namespace example
styles Shapes {
    $nostyle {
        foreground: black;
    }
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
| `$nostyle` | everything that no other rule styles (the default style) |
| `$keyword` | every word-like literal, the same as `$$ "'[a-zA-Z_][a-zA-Z0-9_-]*'"` |

A meta rule `$$ "regex" { ... }` styles every terminal whose text **as written in the grammar** matches the regex,
quotes included: `$$ "'[^a-zA-Z]+'"` styles all punctuation literals such as `'{'` and `','`.
A meta rule that matches no terminal gives a warning.

When several rules match a token, they combine like CSS: start from `$nostyle`, then apply each matching rule
in the order written; a later rule overrides only the declarations it sets.
So `$keyword { font-weight: bold; }` then `'circle' { foreground: red; }` makes `circle` bold and red.

The declarations are CSS-like `name: value;` pairs; the editor decides which names it supports.
A value that is not a single word (e.g. `font-family: 'Courier New';`) is written in single quotes.
A name declared twice in one rule gives a warning; the last value is used.

`styles B : A { }` extends style set `A`: `B` has the rules of `A`, then its own.
`A` can be in the same style file or in the context (e.g. the style sets of other languages).
A simple name is found in the namespace of `B`, then in the namespaces imported with `import`, in order
(`namespace example import other.styles` then `styles B : A { }` can extend `other.styles.A`).
A qualified name can always be used. A style set must not extend itself, directly or indirectly.
A style set is used for the grammar with the same qualified name (`namespace example styles Shapes` for `namespace example grammar Shapes`).
A style set named after no grammar of the language gives a warning, unless another style set extends it.

## Semantic selectors

A selector can also name a type of the language, using what semantic analysis finds out about the sentence:

| Selector | Matches |
|---|---|
| `type T` | every token of an element whose type is `T` or conforms to `T` |
| `identify T` | the identifier that names an element of type `T` (or a subtype), as given by `identify T by ...` in the [references](references.md) |
| `refers-to T` | an identifier that refers to an element of type `T` (or a subtype), i.e. a resolved reference |

For the Shapes language with its [references](references.md#example):

```agl
namespace example
styles Shapes {
    identify Shape {
        font-weight: bold;
    }
    refers-to Circle {
        foreground: red;
    }
    type Connection {
        font-style: italic;
    }
}
```

In `connect c to s` the `c` is red (it refers to a circle), every token of the connection is italic,
and the names in `circle c` and `square s` are bold.

A simple type name is found in the types namespace with the qualified name of the style set
(`example.Shapes` for `namespace example styles Shapes`, where the types of `namespace example grammar Shapes` are),
then in the namespaces imported with `import-types`, in order. A qualified name can always be used.
A type that is not found is an error.

```agl
namespace other
styles X {
    import-types example.Shapes
    refers-to Circle { foreground: red; }
}
```

`type`, `identify` and `refers-to` on their own (no type name after them) are still rule names.
The editor styles these tokens once semantic analysis of the sentence has finished.
