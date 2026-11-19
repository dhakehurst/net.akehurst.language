# SysML v2.0 (2026-04): standard grammar onto the metamodel types

The `standard` grammar (generated from the release BNF), with an asm-transform whose ASM is made of the
KerML v1.0 / SysML v2.0 metamodel types (the metamodel of github `dhakehurst/net.akehurst.omg`).

- `grammar.agl-grm`, `style.agl-sty`: copies of `../standard`.
- `types.agl-typ`: the agl types domain of the metamodel (`SysMLTypesDomain` of net.akehurst.omg, branch 2.4.0-inwork 7a5a2b2, as `asString`).
- `transform.agl-trf`: generated from the abstract syntax of the release BNF (`p = X`, `p += X`, `p ?= 'kw'`, `[QualifiedName]`, `{ p = v }`)
  by `gen_transform_sysml.sh` (converter; the grammar file has two grammars, `KerML` and `SysMLv2_0 : KerML`, so the transform has two asm-transforms). Do not edit by hand; regenerate. Corrections of the BNF are listed in
  [../../SPEC_ISSUES.md](../../SPEC_ISSUES.md).

Mapping choices:
- a name (`[QualifiedName]`) is the list of its unquoted segments, set into the reference property;
- a reference property is never set to an owned object (e.g. a feature chain): only the owning property (`ownedRelatedElement`) holds it;
- an assignment to a derived property of the metamodel (`ownedMemberFeature`, `ownedMemberParameter`, `ownedMemberElement`,
  `FeatureValue.value`, `ElementFilterMembership.condition`) adds the value to `ownedRelatedElement`; `ownedTypeFeaturing` to `ownedRelationship`;
- an unassigned call to a rule whose type is a supertype of the caller's type sets properties of the caller's object.

There are no references for these types yet.
