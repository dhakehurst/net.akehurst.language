# AGL - Akehurst Grammar Language

(This documentation is AI generated, my apologies for any mistakes!)

**AGL** is a comprehensive, multi-platform language processing library built with Kotlin Multiplatform.
It provides powerful tools for creating, parsing, and processing domain-specific languages (DSLs).

## Overview

AGL is a complete language development framework that enables you to:

- **Define grammars** in a simple, expressive grammar language
- **Parse text** according to your grammar definitions
- **Analyze syntax trees** and transform them into semantic models
- **Validate and process** language content
- **Format and style** parsed content
- **Handle cross-references** and semantic analysis
- **transform** models (i.e. the AST) into other data structures.
- **Support multiple target platforms** (JVM, JS, WASM, Native)

## Key Features

### 🎯 Language-Agnostic
Build parsers for any language or DSL - from simple configuration files to complex programming languages.

### 🚀 Multi-Platform Support
- **JVM** - Full Java compatibility
- **JavaScript** - Browser and Node.js support
- **WebAssembly** - WASM for high-performance browser execution
- **Native** - macOS, Linux, Windows (Kotlin/Native)

### 🏗️ Modular Architecture
- **Grammar Definition** - Express your language's syntax
- **Parsing** - Efficient parsing with left-corner parsing algorithm
- **Type System** - Optional type checking and semantic analysis
- **ASM Transform** - Transform parsed trees into domain models
- **Cross-References** - Resolve references between elements
- **Formatting** - Format and style parsed content
- **Completion** - Provide IDE-like completion suggestions
- **M2M Transform** - Transform one data-structure into another.

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
- **ASM** (Abstract Syntax Model) - Target representation of parsed content
- **Types Domain** - Type system for semantic analysis
- **ASM Transform** - Rules for transforming parse trees to semantic models

For detailed explanations, see [Core Concepts](./CONCEPTS.md).

## Modules

| Module | Purpose |
|--------|---------|
| `agl-parser` | Core parsing engine and parse tree structures |
| `agl-processor` | Complete language processor with all features |
| `agl-regex` | Regular expression support for lexical analysis |
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

- **Version**: 4.2.2
- **Language**: Kotlin 2.2+
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

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

---

**Start building language processors today!** → [Getting Started](./GETTING_STARTED.md)

