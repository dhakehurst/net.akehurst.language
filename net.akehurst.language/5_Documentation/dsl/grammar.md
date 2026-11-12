# Grammar

A grammar defines the syntax of a language. AGL reads it at runtime; there is no code generation step.
The parser accepts any context-free grammar, including left recursion and ambiguity.

Pass it to `Agl.processorFromStringSimple(GrammarString(...))`, see [Getting started](../GETTING_STARTED.md).
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_Grammar.kt`.
The file layout (namespace, import, options, comments) is described in [base](base.md).

## Minimal example

```agl
namespace example
grammar Hello {
    skip leaf WS = "\s+" ;
    greeting = 'hello' NAME ;
    leaf NAME = "[a-z]+" ;
}
```

A namespace can hold several grammars. The last one is the target grammar, used by the processor.
The first non-skip rule of the target grammar is the default goal rule (the rule a sentence must match).

## Rules

A rule is `name = right-hand-side ;`.

| Syntax | Meaning |
|---|---|
| `'if'` | literal text |
| `"[a-z]+"` | regular expression (Kotlin `Regex` syntax) |
| `a b c` | concatenation: `a` then `b` then `c` |
| `a \| b` | choice, see below |
| `a \|\| b` | ambiguous choice: keep every alternative that matches |
| `( a b )` | group |
| `a?` | optional |
| `a*`, `a+` | list: zero or more, one or more |
| `a2+`, `a2..5`, `a{2..5}` | list with a minimum, or a minimum and maximum |
| `[ a / ',' ]*` | separated list: `a` items with `,` between them (any multiplicity) |
| `Other::rule` | embedded rule from grammar `Other`, see below |
| `r = ;` | empty rule (matches nothing) |
| `// ...`, `/* ... */` | comments |

## skip and leaf

- `skip` rules (typically white space and comments) may appear between any two tokens.
  They are in the parse tree but not in the ASM.
- `leaf` rules are tokens: their matched text is the value, and their inside is not in the parse tree.
  Use `leaf` for names, numbers and other terminals.

When a skip rule exists, it is required between two tokens when the second token would otherwise
run into the first. With `skip leaf WS = "\s+"`, `S = 'if' ID` does not match `ifx`, but does match `if x`.
The option `#skipRequiredBetweenTokens: false` turns this off.

## Choice

`a | b` takes the longest match. When the alternatives match the same text, the last one wins:

```agl
S = a | b ;
a = 'x' ;
b = 'x' ;
```

`x` parses as `S { b { 'x' } }`. With `a || b` the parse tree keeps both: `S|0 { a { 'x' } }` and `S|1 { b { 'x' } }`.

## Preference rules

A preference rule chooses between alternatives that overlap, for example operator precedence.
Later lines win over earlier ones, so here `/` binds tighter than `+`:

```agl
E = v | add | div ;
add = E '+' E ;
div = E '/' E ;
leaf v = "[a-z]+" ;

preference E {
    add on '+' left
    div on '/' left
}
```

`a+b/c` parses as `add(a, div(b, c))`. Each line is `<alternative> on <terminals> left|right` (associativity).
For an alternative that is a separated list (`add = [E / '+']2+`), write `add ITEM on '+' left`.

## Embedded grammars

`Other::rule` uses a rule from another grammar, with that grammar's own skip rules:

```agl
namespace example
grammar Numbers {
    skip leaf WS = "\s+" ;
    list = [NUM / ',']+ ;
    leaf NUM = "[0-9]+" ;
}
grammar Doc {
    skip leaf WS = "\s+" ;
    doc = 'nums' Numbers::list ';' ;
}
```

## Extending a grammar

`grammar Ext : Base` inherits all rules of `Base` (several bases are allowed: `: A, B`).

| Syntax | Meaning |
|---|---|
| `override item = 'b' ;` | replace the inherited rule |
| `override item +=\| 'b' ;` | add an alternative to the inherited rule |
| `override item == Y.item ;` | the rule is inherited via two bases (a diamond): use the one from base `Y` |

The default goal rule is the first rule written in the target grammar, so after an override it is often
necessary to set it with `#defaultGoalRule`:

```agl
grammar Ext : Base {
    #defaultGoalRule: S
    override item +=| 'b' ;
}
```

## Options

Options are written `#name: value` at the start of a grammar.

| Option | Meaning |
|---|---|
| `#defaultGoalRule: S` | the goal rule when none is given (default: first non-skip rule) |
| `#skipRequiredBetweenTokens: false` | skips are always optional (default `true`, see above) |
| `#scanner: Classic` | scanner kind: `OnDemand` (default, scans while parsing) or `Classic` (scans the whole text first) |
| `#regexEngine: AGL` | regex engine for patterns: `PLATFORM` (default) or `AGL` |

A different goal can be given for one parse: `processor.parse(text, Agl.parseOptions { goalRuleName("one") })`.
