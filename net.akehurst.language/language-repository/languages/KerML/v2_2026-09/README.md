# KerML, release 2026-09

The KerML textual notation from the [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release)
2026-09.1 (KerML v1.1 Beta 2).

## grammars

- `standard` - converted from the specification BNF (`bnf/KerML-textual-bnf.kebnf`).
  Rule names and structure are as in the specification. Changes to the specification BNF are marked `// AGL:` in the grammar.
- `agl` - tailored for AGL: split into modular grammars that extend each other, wrapper rules inlined,
  separated lists, and operator precedence given by a preference rule.
  It also has `references.agl` (identify, scope and references rules).

Both grammars set `#scanner: Classic`, because KerML reserves its keywords (KerML 8.2.2.6).

## valid

The models in `valid/` are copied unchanged from the SysML v2 Release, tag `2026-09.1`
(commit 9546486f49f704d1df092e3f252f4fcfbce534dd):

- `valid/examples` from the folder `kerml/src/examples`
- `valid/library` from the folder `sysml.library/Kernel Libraries` (the `.kerml` files of the KerML model libraries)

They are licensed under the Eclipse Public License v2.0, see [LICENSE-valid-EPL-2.0.txt](LICENSE-valid-EPL-2.0.txt).
The license and this README are not in `valid/`, because every file in `valid/` is parsed as a KerML sentence by the tests.

The other files in this folder are licensed under the Apache License, Version 2.0, as the rest of this repository.
