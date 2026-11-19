# SysML v2.1 (Beta 2, 2026-09): standard grammar onto the metamodel types

The `standard` grammar (generated from the release BNF), with an asm-transform whose ASM is made of the
KerML v1.1 / SysML v2.1 metamodel types (the official 2.1 metamodel, from the Pilot Implementation `org.omg.sysml/model/SysML_xmi.uml` at release tag 2026-09.1).

- `grammar.agl-grm`, `style.agl-sty`: copies of `../standard`.
- `types.agl-typ`: the agl types domain of the metamodel, converted from `SysML_xmi.uml` (OMG publishes only the 2.0 XMI) by `uml2agltypes.py` (converter); derived properties are not stored.
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
