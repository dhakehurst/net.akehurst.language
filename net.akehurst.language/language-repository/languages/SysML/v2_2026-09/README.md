# SysML v2, release 2026-09

The SysML v2 textual notation from the [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release)
2026-09.1 (SysML v2.1 Beta 2, KerML v1.1 Beta 2).

## grammars

- `standard` - converted from the specification BNF (`bnf/SysML-textual-bnf.kebnf` and `bnf/KerML-textual-bnf.kebnf`).
  Rule names and structure are as in the specification. Changes to the specification BNF are marked `// AGL:` in the grammar.
- `agl` - tailored for AGL: split into modular grammars that extend each other, wrapper rules inlined,
  separated lists, and operator precedence given by a preference rule.
  It also has `references.agl` (identify, scope and references rules).

## KerML

SysML is built on KerML ([../../KerML/v2_2026-09](../../KerML/v2_2026-09)). To resolve references, the SysML processor
uses the types of the KerML processor (grammar `agl`) as its base types, and the KerML references are merged into the SysML references.
The KerML model libraries (`.kerml`) are processed by the KerML processor, the SysML model libraries (`valid/library`) by the SysML processor,
into the same context (see the test `test_SysML_v2_2026_09_agl_references`).

## valid

The models in `valid/` are copied unchanged from the SysML v2 Release, tag `2026-09.1`
(commit 9546486f49f704d1df092e3f252f4fcfbce534dd):

- `valid/examples`, `valid/training` and `valid/validation` from the folders `sysml/src/examples`, `sysml/src/training` and `sysml/src/validation`
- `valid/library` from the folder `sysml.library` (the `.sysml` files of the SysML model libraries; the `.kerml` ones are in the KerML language)

They are licensed under the Eclipse Public License v2.0, see [LICENSE-valid-EPL-2.0.txt](LICENSE-valid-EPL-2.0.txt).
The license and this README are not in `valid/`, because every file in `valid/` is parsed as a SysML sentence by the tests.

The other files in this folder are licensed under the Apache License, Version 2.0, as the rest of this repository.
