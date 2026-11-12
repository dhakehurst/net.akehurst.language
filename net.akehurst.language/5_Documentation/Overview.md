# AGL - Akehurst Grammar Language

**AGL** is a comprehensive, multi-platform language processing library built with Kotlin Multiplatform.
It provides powerful tools for creating, parsing, and processing domain-specific languages (DSLs).

## Overview

AGL started out from the tag line "When a RegEx is not enough".
It was designed to allow me to write grammars quickly and easily and parse a string with the minimum
number of lines of code. As close to the experience of using a Regular Expression function as possible.

Over the years, I found it so useful, more and more features were added until, now,

AGL is a complete language development framework that enables you to do many things related to DSL and model processing.
The API is designed such that simple things are simple, defaults are provided (convention over configuration),
with everything being configurable if you want to customise it. AGL provides features to: 

- **Define grammars** in a simple, expressive grammar language, to specify your language's syntax, with features for grammar composition.
- **Parse text** according to the grammar definitions
- **Default/Custom Abstract Syntax Graph** default is based on the grammar and can be augmented with grammar-rule to type mappings, or a custom syntax analyser can be provided
- **Create syntax graphs** (Syntax Analysis) convert a parse tree into an abstract syntax graph (tree/model)
- **Validate and process** (Semantic Analysis) analyse the abstract syntax adding semantic content and issues
- **Style** the language, defining color for parts of a sentence based on scanning or parsing results (future work to be based on abstract syntax)
- **Code Completion** Can ask for the next expected items at any point in the text, a default is provided based on the grammar, or a custom provider can be written
- **Format** (model-to-text, M2T) specify declarative rules that will generate text from an abstract syntax graph (inverse of parsing!)
- **Handle cross-references** there is a DSL for specifying cross-references,  
- **Transform** (model-to-model, M2M) models (object graphs) such as the abstract syntax graph into other data structures.
- **Support multiple target platforms** (JVM, JS, WASM)
- **Language-Agnostic** via kotlin multi-platform, build parsers for any language or DSL - from simple configuration files to complex programming languages.

## Key Features

### 🚀 Multi-Platform Support
- **JVM** - Full Java compatibility
- **JavaScript** - Browser and Node.js support
- **WebAssembly** - WASM (wasmJs) for the browser

### 📦 Zero Configuration
Use sensible defaults and get started immediately, or customize every aspect for advanced use cases.

## Quick Start

See [Getting Started](./GETTING_STARTED.md) for a step-by-step guide to:
1. Define your first grammar
2. Create a language processor
3. Parse and process text
4. Extract semantic information

## How a sentence is processed

`processor.process(sentence)` runs these steps; each can also be called on its own.

1. **Scan and parse**: the text is matched against the [grammar](dsl/grammar.md), giving a Shared Packed Parse Tree (SPPT).
2. **Syntax analysis**: the parse tree is turned into an Abstract Syntax Model (ASM), using the [types](dsl/types.md)
   and the [asm-transform](dsl/asm-transform.md). Both are generated from the grammar unless you give your own.
   With the option `syntaxAnalysis { streaming(true) }` the ASM is built during the parse, without keeping a parse tree.
3. **Semantic analysis**: [references](dsl/references.md) are resolved and checked.

Other uses of a language definition: [style](dsl/style.md) (syntax highlighting),
[completion](dsl/completion.md) (`expectedItemsAt`) and [format](dsl/format.md) (ASM back to text).
[m2m-transform](dsl/m2m-transform.md) transforms one model into another.

## The AGL DSLs

Each part of a language definition is written in a small DSL, and each DSL is itself defined with AGL.

| DSL | Page | Used for |
|---|---|---|
| base | [base.md](dsl/base.md) | namespaces, imports, options and comments shared by all DSLs |
| grammar | [grammar.md](dsl/grammar.md) | the syntax of a language |
| types | [types.md](dsl/types.md) | the types of the ASM |
| asm-transform | [asm-transform.md](dsl/asm-transform.md) | how the parse tree becomes the ASM |
| references | [references.md](dsl/references.md) | scopes and cross-references |
| style | [style.md](dsl/style.md) | syntax highlighting |
| completion | [completion.md](dsl/completion.md) | code completion items |
| format | [format.md](dsl/format.md) | ASM to text |
| expressions | [expressions.md](dsl/expressions.md) | expressions used inside the other DSLs |
| m2m-transform | [m2m-transform.md](dsl/m2m-transform.md) | model to model transformation |

Patterns in grammars use Kotlin `Regex` syntax, see [agl-regex](../agl-regex/README.md).

## Modules

| Module | Purpose |
|---|---|
| `agl-regex` | regular expression engine used by the scanner |
| `agl-parser` | scanner, parser and parse tree |
| `agl-processor` | language processor, all the DSLs (depends on the other two) |
| `agl-generators` | code generators |
| `agl-neural` | machine learning support (experimental) |

Most users only need `agl-processor`.

## Documentation

- **[Getting Started](./GETTING_STARTED.md)** - first grammar, parse and process
- **[The AGL DSLs](#the-agl-dsls)** - one page per DSL
- **[Comparison](./COMPARISON.md)** - how AGL compares to other parser frameworks

## Project Information

- **Version**: see the Maven Central badge in the [README](../../README.md)
- **License**: Apache License 2.0
- **Author**: Dr. David H. Akehurst
- **Repository**: https://github.com/dhakehurst/net.akehurst.language

## Building from Source

```bash
./gradlew build
```

For detailed build instructions, see the project's GitHub repository.

## Installation

### Maven
```xml
<dependency>
    <groupId>net.akehurst.language</groupId>
    <artifactId>agl-processor</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
implementation("net.akehurst.language:agl-processor:VERSION")
```

## Support

- Report issues on [GitHub](https://github.com/dhakehurst/net.akehurst.language/issues)

## License

Licensed under the Apache Licence, Version 2.0. See LICENCE file for details.

---

**Start building language processors today!** → [Getting Started](./GETTING_STARTED.md)

