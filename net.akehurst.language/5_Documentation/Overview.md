# AGL - Akehurst Grammar Language

**AGL** is a comprehensive, multi-platform language processing library built with Kotlin Multiplatform.
It provides powerful tools for creating, parsing, and processing domain-specific languages (DSLs).

## Overview

AGL started out from the tag line "When a RegEx is not enough".
It was designed to allow me to write grammars quickly and easily and parse a string with the minimum
number of lines of code. As close to the experience of using a Regular Expression function as possible.

Over the years, I found it so useful, more anf more features were added until, now,

AGL is a complete language development framework that enables you to do many things related to DSL and model processing.
The API is designed such that simple things are simple, defaults are provided (convention over configuration),
with everything being configurable if you want to customise it. AGL provides features to: 

- **Define grammars** in a simple, expressive grammar language, to specify your language's syntax, with features for grammar composition.
- **Parse text** according to the grammar definitions
- **Default/Custom Abstract Syntax Graph** defult is based on the grammar and can be augmented with grammar-rule to type mappings, or a custom syntax analyser can be provided
- **Create syntax graphs** (Syntax Analysis) convert a parse tree into an abstract syntax graph (tree/model)
- **Validate and process** (Semantic Analysis) analyse the abstract syntax adding semantic content and issues
- **Style** the langauge, defining color for parts of a sentence based on scanning or parsing results (future work to be based on abstract syntax)
- **Code Completion** Can ask for the next expected items at any point in the text, a default is provided based on the grammar, or a custom proivder can be written
- **Format** (model-to-text, M2T) specify declarative rules that will generate text from an abstract syntax graph (inverse of parsing!)
- **Handle cross-references** there is a DSL for specifying cross-references,  
- **Transform** (model-to-model, M2M) models (object graphs) such as the abstract syntax graph into other data structures.
- **Support multiple target platforms** (JVM, JS, WASM, Native)
- **Language-Agnostic** via kotlin multi-platform, build parsers for any language or DSL - from simple configuration files to complex programming languages.

## Key Features

### 🚀 Multi-Platform Support
- **JVM** - Full Java compatibility
- **JavaScript** - Browser and Node.js support
- **WebAssembly** - WASM for high-performance browser execution
- **Native** - macOS, Linux, Windows (Kotlin/Native)

### 📦 Zero Configuration
Use sensible defaults and get started immediately, or customize every aspect for advanced use cases.

## Quick Start

See [Getting Started](./GETTING_STARTED.md) for a step-by-step guide to:
1. Define your first grammar
2. Create a language processor
3. Parse and process text
4. Extract semantic information

## Core Concepts

- **Grammar** - Defines the syntax rules of your language
- **Parser** - Analyzes text according to grammar rules
- **SPPT** (Shared Packed Parse Tree) - Efficient parse tree representation
- **ASG** (Abstract Syntax Graph) - Target representation of parsed content
- **Types Domain** - Type system for semantic analysis
- **ASM Transform** - Rules for transforming parse trees to semantic models

For detailed explanations, see [Core Concepts](./CONCEPTS.md).

## Modules

| Module | Purpose |
|--------|---------|
| `agl-regex` | Regular expression support for lexical analysis |
| `agl-parser` | Core parsing engine and parse tree structures |
| `agl-processor` | Complete language processor with all features |
| `agl-generators` | Code generators for various target languages |
| `agl-neural` | Machine learning support (experimental) |
| `collections` | Utility collections and data structures |

## Documentation

- **[Getting Started](./GETTING_STARTED.md)** - Quick introduction and first example
- **[Core Concepts](./CONCEPTS.md)** - Understand the fundamental concepts
- **[Grammar Definition](./GRAMMAR_GUIDE.md)** - Complete grammar syntax reference
- **[API Reference](./API_REFERENCE.md)** - Detailed API documentation
- **[Examples](./EXAMPLES.md)** - Real-world example use cases
- **[Troubleshooting](./TROUBLESHOOTING.md)** - Common issues and solutions

## Project Information

- **Version**: 4.2.2.21
- **Language**: Kotlin 2.2.21
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
    <version>4.2.2.21</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
implementation("net.akehurst.language:agl-processor:4.2.2.21")
```

## Support

- Report issues on [GitHub](https://github.com/dhakehurst/net.akehurst.language/issues)
- Check [Troubleshooting](./TROUBLESHOOTING.md) for common solutions
- Review [Examples](./EXAMPLES.md) for implementation patterns

## License

Licensed under the Apache Licence, Version 2.0. See LICENCE file for details.

---

**Start building language processors today!** → [Getting Started](./GETTING_STARTED.md)

