# How AGL compares to other parser frameworks

> **Keep this page up to date.** When a feature named here changes (parser algorithm, ambiguity handling,
> error recovery, incremental parsing, platforms, IDE/LSP support, performance), update this page in the same commit.
>
> Last reviewed: 2026-11-13, against branch `2.4.0-inwork` at `fa239a2a`.

AGL sits between the parser generators (ANTLR, tree-sitter) and the full language workbenches
(Xtext, Langium, Spoofax, JetBrains MPS). It combines a general parser (like the GLR/GLL tools),
grammars interpreted at runtime, and Kotlin Multiplatform, so it also runs in the browser.
Where it is behind is raw speed, error recovery and community size.

## Strengths

1. **Unrestricted grammars.** The left-corner, GLR-style parser (`agl-parser`, `LeftCornerParser`) accepts
   left recursion and ambiguous grammars and produces a shared packed parse tree (SPPT).
   ANTLR (ALL(*)) and Langium/Xtext (Chevrotain/ANTLR based) reject or need rewrites for some grammars;
   ANTLR only supports direct left recursion. The real peers here are Spoofax (SGLR), Iguana (GLL) and tree-sitter (GLR).
2. **Declarative ambiguity resolution.** Preference rules, with "last choice option wins" as the default, are similar
   to SDF priorities. ANTLR has only ordered alternatives and predicates.
3. **Runtime grammars, no code generation step.** A grammar can be loaded or changed while the program runs.
   ANTLR, tree-sitter and Xtext all generate code. Only parser combinators and Spoofax's interpreted SGLR are similar.
4. **Grammar composition.** `extends`, `override` and `embedded` grammars work well because the parser is general.
   Embedding one language in another is hard in ANTLR.
5. **Multiplatform from one codebase.** JVM, JS (with TypeScript definitions) and wasmJs.
   Xtext and MPS are JVM only, Langium is TypeScript only, ANTLR needs a separate generated target per language.
6. **The whole pipeline in one library.** Grammar, typed ASM derived from the grammar (or a custom mapping),
   cross-references and scopes, semantic analysis, style, completion, formatting (M2T), M2M transforms and
   expressions, each as a small DSL. Langium and Xtext are the closest, but they are tied to their IDE/LSP setup.
   Name resolution (enclosing scopes, imports, inherited members, visibility, aliases) is declared in the reference DSL;
   in Xtext and Langium the same needs scope-provider code. Each DSL has a reference page in [dsl/](dsl/).
7. **Completion from the grammar with no extra work.** Tree-sitter has none; ANTLR needs a library such as antlr4-c3.

## Weaknesses

1. **Speed.** A general parser costs more than ANTLR or tree-sitter on unambiguous input; performance is still a TODO
   in the root README. The `net.akehurst.language.comparisons` project benchmarks AGL against ANTLR 3/4 and Iguana,
   but its results are from 2021.
2. **No incremental parsing.** Tree-sitter's main strength, and important for editors.
3. **No error recovery.** A failed parse reports the expected items, not a partial tree.
   ANTLR, tree-sitter and Langium recover and continue.
4. **Editor is a separate package.** There is an AGL editor, in a separate repository
   ([net.akehurst.language.editor](https://github.com/dhakehurst/net.akehurst.language.editor)).
   Xtext, Langium, Spoofax and MPS ship editor generation as part of the framework.
   (This page does not yet compare the editor's features with theirs.)
5. **Maturity.** A one-person project with a small community. Known open issues include
   parse trees that are not always what one expects and a number of failing parser tests.
   The documentation is newer and smaller than that of ANTLR, Xtext or Langium: a [Getting Started](GETTING_STARTED.md)
   guide and one reference page per DSL (see [Overview](Overview.md)), but no tutorials or worked examples yet.
6. **Kotlin/Native not enabled.** Only JVM, JS and wasmJs are built; `macosArm64()` is commented out in
   `0_build-logic/src/main/kotlin/project-conventions.gradle.kts`.
7. **No projectional editing.** That is MPS's niche, so it is a different category rather than a gap.

## Summary per framework

| Framework | Compared with AGL |
|---|---|
| ANTLR | AGL is more expressive and interpreted; ANTLR is faster, more mature and recovers from errors. |
| tree-sitter | AGL has a semantic pipeline and runtime grammars; tree-sitter is incremental and faster. |
| Xtext / Langium | AGL is lighter, multiplatform and has a general parser; they generate an IDE/LSP as part of the framework (AGL's editor is a separate repository). |
| Spoofax / SDF | Same ideas on ambiguity and composition; AGL is a plain embeddable library, not a workbench. |
| JetBrains MPS | Projectional rather than textual; a different category. |
| GLR/GLL tools (e.g. Iguana) | Similar parsing power; AGL adds the processing pipeline and multiplatform. |
| Parser combinators | Both are runtime; AGL's grammar is declarative and general (no left-recursion limits). |
