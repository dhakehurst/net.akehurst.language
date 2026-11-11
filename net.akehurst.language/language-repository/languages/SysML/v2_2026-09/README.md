# SysML v2, release 2026-09

The SysML v2 textual notation from the [SysML v2 Release](https://github.com/Systems-Modeling/SysML-v2-Release)
2026-09.1 (SysML v2.1 Beta 2, KerML v1.1 Beta 2).

## grammars

- `standard` - converted from the specification BNF (`bnf/SysML-textual-bnf.kebnf` and `bnf/KerML-textual-bnf.kebnf`).
  Rule names and structure are as in the specification. Changes to the specification BNF are marked `// AGL:` in the grammar.
- `agl` - tailored for AGL: split into modular grammars that extend each other, wrapper rules inlined,
  separated lists, and operator precedence given by a preference rule.

## valid

The example models in `valid/` are copied unchanged from the folders `sysml/src/examples`, `sysml/src/training`
and `sysml/src/validation` of the SysML v2 Release, tag `2026-09.1`
(commit 9546486f49f704d1df092e3f252f4fcfbce534dd).

They are licensed under the Eclipse Public License v2.0, see [LICENSE-valid-EPL-2.0.txt](LICENSE-valid-EPL-2.0.txt).
The license and this README are not in `valid/`, because every file in `valid/` is parsed as a SysML sentence by the tests.

The other files in this folder are licensed under the Apache License, Version 2.0, as the rest of this repository.
