# KerML v1.0, release 2026-04

The KerML textual notation from the [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release)
2026-04 (KerML v1.0), the last release of KerML v1.0.

## languages

Two variants of the language, each in `languages/<variant>` with its grammar and other definition files:

- `standard` - converted from the specification BNF (`bnf/KerML-textual-bnf.kebnf`).
  Rule names and structure are as in the specification. Changes to the specification BNF are marked `// AGL:` in the grammar.
- `agl` - tailored for AGL: split into modular grammars that extend each other, wrapper rules inlined,
  separated lists, and operator precedence given by a preference rule.
  It also has `references.agl-ref` (identify, scope and references rules).

Both grammars set `#scanner: Classic`, because KerML reserves its keywords (KerML 8.2.2.6).

## valid

The models in `valid/` are copied unchanged from the SysML v2 Release, tag `2026-04`
(commit 9baca5908ca28b53da085de69336fde48420ea8f):

- `valid/examples` from the folder `kerml/src/examples`
- `valid/library` from the folder `sysml.library/Kernel Libraries` (the `.kerml` files of the KerML model libraries)

They are licensed under the Eclipse Public License v2.0, see [LICENSE-valid-EPL-2.0.txt](LICENSE-valid-EPL-2.0.txt).
The license and this README are not in `valid/`, because every file in `valid/` is parsed as a KerML sentence by the tests.

The other files in this folder are licensed under the Apache License, Version 2.0, as the rest of this repository.

## Specification issues

Issues found in the KerML specification are recorded, together with the SysML ones, in [../../SysML/v2.0_2026-04/SPEC_ISSUES.md](../../SysML/v2.0_2026-04/SPEC_ISSUES.md).
