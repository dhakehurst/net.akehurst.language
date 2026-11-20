# KerML v1.1 (Beta 2, 2026-09): standard grammar onto the metamodel types

The `standard` grammar (generated from the release BNF), with an asm-transform whose ASM is made of the
KerML v1.1 / SysML v2.1 metamodel types (the official 2.1 metamodel, from the Pilot Implementation `org.omg.sysml/model/SysML_xmi.uml` at release tag 2026-09.1).

- `grammar.agl-grm`, `style.agl-sty`: copies of `../standard`.
- `types.agl-typ`: the agl types domain of the metamodel, converted from `SysML_xmi.uml` (OMG publishes only the 2.0 XMI) by `uml2agltypes.py` (converter); derived properties are not stored.
- `transform.agl-trf`: generated from the abstract syntax of the release BNF (`p = X`, `p += X`, `p ?= 'kw'`, `[QualifiedName]`, `{ p = v }`)
  by `gen_transform.py` (converter), without the 2.0 BNF fixes. Do not edit by hand; regenerate. Corrections of the BNF are listed in
  [../../../../SysML/v2.1_2026-09/SPEC_ISSUES.md](../../../../SysML/v2.1_2026-09/SPEC_ISSUES.md).

Mapping choices:
- a name (`[QualifiedName]`) is the list of its unquoted segments, set into the reference property;
- a reference property is never set to an owned object (e.g. a feature chain): only the owning property (`ownedRelatedElement`) holds it;
- an assignment to a derived property of the metamodel (`ownedMemberFeature`, `ownedMemberParameter`, `ownedMemberElement`,
  `FeatureValue.value`, `ElementFilterMembership.condition`) adds the value to `ownedRelatedElement`; `ownedTypeFeaturing` to `ownedRelationship`;
- an unassigned call to a rule whose type is a supertype of the caller's type sets properties of the caller's object.

`references.agl-ref`: the references (name resolution of KerML 8.2.3.5) written against the metaclasses: identification by
`declaredName` / `declaredShortName` (and the effective name of a redefining feature), the scopes of namespaces (imports) and types
(inherited memberships, also of the implicit specializations of the Semantic Library), and the local namespace of each kind of
relationship. What is not (yet) as the spec says is marked `OPEN` in the file.
