# References (cross-references and scopes)

The references DSL says which ASM properties are names that refer to other objects, and where to look them up.
Semantic analysis then resolves each reference, or reports an error.
The examples are tested in `agl-processor/src/commonTest/kotlin/documentation/test_Doc_References.kt`.

## Example

For the [Shapes language](../GETTING_STARTED.md#the-example-language), a connection refers to shapes by name:

```agl
namespace example.Shapes
    identify Circle by name
    identify Square by name
    references {
        in Connection {
            property source refers-to Circle|Square
            property target refers-to Circle|Square
        }
    }
```

The namespace is the namespace of the types (for types generated from a grammar: `<grammar namespace>.<grammar name>`).

```kotlin
val processor = Agl.processorFromStringSimple(
    grammarDefinitionStr = GrammarString(grammar),
    referenceStr = CrossReferenceString(references)
).processor!!

val options = Agl.options<Asm, SentenceContext> { semanticAnalysis { sentenceContext(contextAsmSimple()) } }
val result = processor.process(sentence, options)
```

A sentence context is needed, otherwise references are not resolved (`process` gives an information issue).
In the ASM, a resolved reference prints as `source = &{'c' : Circle}`.
`circle c connect c to x` gives the error `Reference 'x' not resolved, to type(s) [Circle, Square] in scope '/'`.

## Syntax

| Syntax | Meaning |
|---|---|
| `identify T by expr` | objects of type `T` are found by the value of `expr` (usually a property, e.g. `name`) |
| `scope T { identify U by expr ... }` | an object of type `T` is a scope; the `U` objects inside it are identified there, not in the root scope |
| `references { in T { ... } }` | the reference properties of type `T` |
| `property p refers-to A\|B` | property `p` is a reference to an `A` or a `B` |
| `forall list of-type T { ... }` | apply the inner lines to each element of `list` (optionally only those of type `T`) |
| `... from expr` | look the name up in the scope given by `expr` instead of the current scope |
| `... when expr` | use this line only when `expr` is true; the first line that holds wins |

Names are looked up in the current scope, then in the enclosing scopes.
In `from` and `when` expressions, `$self` is the object, `$scope` is the current scope, `$root` is the root scope.
Expressions are written in the [expressions](expressions.md) language.

## Imports, inheritance, visibility, aliases

These are written inside a `scope` block, or on an `identify` line:

| Syntax | Meaning |
|---|---|
| `import forall nav members-of ref` | for each `nav`, the members of the scope that `ref` refers to are visible here |
| `import forall nav element ref` | for each `nav`, only the object that `ref` refers to is visible here |
| `... recursive expr` | also import what the imported scope imports, while `expr` holds |
| `inherit forall nav members-of ref` | inherit the members of the scope that `ref` refers to (for example a supertype) |
| `identify T by expr visibility v` | `v` gives the visibility: `'private'`, `'protected'`, anything else is public |
| `identify T by expr alias-for ref` | the name is another name for the object that `ref` refers to |

Private items are visible only inside their scope, protected items also in scopes that inherit it,
and only public items are visible through an import.
Lookup order is: owned items, then inherited, then imported.
In a `from` expression, `$scope.owner` is the enclosing scope and `$scope.owner.inherited` adds its inherited scopes.

A complete worked example (packages, imports, specialization, redefinition, aliases, global names) is the test
`agl-processor/src/commonTest/kotlin/agl/simple/test_ReferenceResolverSimple_imports.kt`.
