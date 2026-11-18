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
| `identify T by a \| b` | alternatives: the value of the first alternative that has a value (not `$nothing`) is the name of the object, the values of the others are more names for it (the same value is used once). There is no warning when an alternative is `$nothing` |
| `scope T { identify U by expr ... }` | an object of type `T` is a scope; the `U` objects inside it are identified there, not in the root scope. In the block, the `identify` lines come first, then the `import`/`inherit` lines |
| `references { in T { ... } }` | the reference properties of type `T` |
| `property p refers-to A\|B` | property `p` is a reference to an `A` or a `B` |
| `type-group N = A\|B` | a name for a list of type names, used as `refers-to N` (also with other names: `refers-to N\|C`); a group may use other groups. It is not a type: where it is used it stands for its list. Declared with the top-level `identify` lines (before the `scope` blocks), visible in that namespace only |
| `property p.m(...) refers-to A` | the navigation may end with method calls: the value of the whole expression is looked up, and property `p` becomes the reference (e.g. `property name.removeSurrounding('\'') refers-to A`, where `'a'` and `a` are the same name; for a list of names `name.map({ n -> n.removeSurrounding('\'') })`) |
| `forall list of-type T when c { ... }` | apply the inner lines to each element of `list` (optionally only those of type `T`, and only those for which `c` holds) |
| `... from E` | start the lookup in the scope of element `E` (or of the element that reference `E` refers to) instead of the current scope |
| `... from $previous.p else E` | in a `forall`, the first element has no previous element: it is looked up in the scope of what reference `E` (evaluated on the object of the `in` block) refers to. Without `else`, the first element uses the normal lookup. `else` is only allowed with `from $previous...` (otherwise it is an error) |
| `... from inherited E` | look the name up starting in each scope that element `E` inherits in turn (in the order the inheriting elements are in the sentence), going outwards from that scope; the first that gives a result is used. If none does, outwards from `E`, but never in `E`'s own members (e.g. a redefined feature: not the redefining feature itself) |
| `property p ... when expr` | use this line only when `expr` is true |
| `property p when { c1 -> refers-to A from E1  c2 -> refers-to B  else -> refers-to C }` | property `p` refers to whatever the first option whose condition holds says; `else` (optional, must be last) applies when none holds |

There is at most one `scope T { }` block and one `in T { }` block per type in a namespace,
and a property appears at most once in a block (use `property p when { ... }` to choose between targets); otherwise it is an error.

Open point (RV8): `when` is written differently in the references DSL (`... when c`, `property p when { c -> ... }`),
in expressions (`when { c -> e  else -> e }`) and in m2m (`when { }`). Whether to make these consistent is still to be decided.

The value of the referring navigation is the name that is looked up.
A String value is split on '.' into the parts of a qualified name (`a.b.c` is `c` in `b` in `a`).
A List of Strings is used as the parts as it is (no split), so a language whose names may contain '.'
can use a List property, or end the navigation with method calls that give a List.
Names are looked up in the current scope, then in the enclosing scopes.
A reference never resolves to an object whose name is computed from that same reference (that would be circular),
e.g. with `identify Perform by target.name` and `in Perform { property target.name refers-to Action }`, in `perform tp` the reference `tp` skips the perform itself
and the lookup goes on outwards (see `test_ReferenceResolverSimple_namedByReference`).
Expressions are written in the [expressions](expressions.md) language. Their special values are all elements:

| Special value | Meaning |
|---|---|
| `$self` | the object the expression is evaluated on |
| `$container` | the object that contains `$self` in the ASM (`$nothing` for the root) |
| `$owner` | the object that owns the scope `$self` is in (the namespace owner, not the ASM container) |
| `$root` | the root object of the sentence of `$self`; `from $root` is the root scope |
| `$previous` | in a `forall`, the element before this one (`from $previous.p`: the members of what `p` of the previous element refers to) |

`.$container` and `.$owner` are also properties of every object, e.g. `$owner.$owner`.

## Imports, inheritance, visibility, aliases

These are written inside a `scope` block, on an `identify` line, or (`import-into-scope`) at the top level:

| Syntax | Meaning |
|---|---|
| `import forall nav members-of ref` | for each `nav`, the members of the scope that `ref` refers to are visible here |
| `import forall nav element ref` | for each `nav`, only the object that `ref` refers to is visible here |
| `import forall nav of-type T ...`, `inherit forall nav of-type T ...` | as above, only for the elements of `nav` that are of type `T` (or a subtype) |
| `import members-of ref`, `inherit members-of ref` | without `forall`: the same as `forall $self` (`ref` is evaluated on the object that owns the scope) |
| `import-into-scope T members-of ref` | (top level) each object of type `T` is an import of the scope it is in: the members of the scope that `ref` (evaluated on that object) refers to are visible there. For a language whose imports can be in many kinds of body (e.g. SysML imports in packages, part and action bodies), one line instead of an `import forall` in each scope |
| `import-into-scope T element ref` | (top level) as above, only the object that `ref` refers to is visible |
| `... recursive expr` | also import what the imported scope imports, while `expr` holds |
| `inherit forall nav members-of ref` | inherit the members of the scope that `ref` refers to (for example a supertype) |
| `inherit named a.b.c` | inherit the members of the element with qualified name `a.b.c`, looked up from the root scopes of the context (for example a library element every element of the type specializes); a name that is not an identifier is quoted: `'my lib'.b` |
| `inherit by-position when expr` | the owner of the scope (X) inherits the members of the element at the same position as X in each scope that the owner of X inherits (in the order of `from inherited`). The items counted are those of a scope for which `expr` holds (evaluated on each item, of any type), in sentence order; X takes part only if `expr` holds for X. For example SysML/KerML parameters: a parameter redefines the parameter at the same position of each general of its owner |
| `... when expr` | (on any `import` or `inherit`) use it only when `expr` holds; for `forall` it is evaluated on each element, for `named` on the object that owns the scope |
| `identify T by expr visibility v` | `v` gives the visibility: `'private'`, `'protected'`, anything else is public |
| `identify T by expr alias-for ref` | the name is another name for the object that `ref` refers to |

A `scope T { }` block also applies to the subtypes of `T`. An element gets the `import` and `inherit` lines of the blocks
for its type and for each of its supertypes, nearest type first; a nearer block does not replace the lines of a farther one.

Private items are visible only inside their scope, protected items also in scopes that inherit it,
and only public items are visible through an import.
Lookup order is: owned items, then inherited (`inherit forall`, then `inherit by-position`, then `inherit named`), then imported.
If the element of an `inherit named` is not in the context (e.g. the library is not loaded), nothing is inherited from it
and there is one warning for that rule.

A complete worked example (packages, imports, specialization, redefinition, aliases, global names) is the test
`agl-processor/src/commonTest/kotlin/agl/simple/test_ReferenceResolverSimple_imports.kt`;
`inherit named`, `when` on imports and inheritance, and `$owner` are in `test_ReferenceResolverSimple_inheritNamed.kt`; `inherit by-position` in `test_ReferenceResolverSimple_inheritByPosition.kt`;
`import-into-scope` is in `test_ReferenceResolverSimple_importIntoScope.kt`; method calls at the end of a referring navigation are in `test_ReferenceResolverSimple_referringValue.kt`.
