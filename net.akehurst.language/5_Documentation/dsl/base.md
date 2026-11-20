# Base: what all AGL DSL files share

Every AGL DSL (grammar, types, asm-transform, references, style, format, completion, expressions, m2m-transform)
extends the same base syntax.

```agl
// a single line comment
/* a multi-line
   comment */
#option-for-the-whole-file
namespace example.shapes
  import example.common
  ... definitions ...
```

| Syntax | Meaning |
|---|---|
| `namespace a.b.c` | starts a namespace; the definitions that follow belong to it. A file can have several. |
| `import a.b` | definitions of namespace `a.b` can be used by their simple name |
| `#name` or `#name: value` | an option, at the start of the file, of a namespace, or of a definition (for example a grammar) |
| `//`, `/* */` | comments |

Names are letters, digits, `_` and `-`, starting with a letter or `_`. Qualified names use `.`.
Option values are names, `true`/`false`, numbers, or `'strings'`.

The options of each DSL are listed on its page, for example [grammar options](grammar.md#options).
