<!--
[![Build Status](https://travis-ci.org/dhakehurst/net.akehurst.language.svg?branch=master)](https://travisci.org/dhakehurst/net.akehurst.language)
-->
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.akehurst.language/agl-processor/badge.svg?gav=true)](https://maven-badges.herokuapp.com/maven-central/net.akehurst.language/agl-processor)
![npm](https://img.shields.io/npm/v/net.akehurst.language-agl-processor)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Generic badge](https://img.shields.io/badge/Kotlin-2.1.21-green)](https://kotlinlang.org/)

# AGL: A Grammar Language (or maybe Akehurst Grammar Language!)

Generic Language (DSL) support for kotlin multiplatform (parser, syntax-analyser, formatter, processor, etc)

There are many parser technologies, frameworks, generators, libraries
that already exist. When I first started this one in about 2007 there was none that
met all the requirements that I have. It has taken me several years (too many) to finally
reach the stage where I am happy to publish it. (mostly because it was a home project and not
worked on very frequently until the last couple of years, 2017/2018).

My Requirements:

 - Simple grammar for defining a language with no restrictions
 -- i.e. should support left & right recursive rules,
 - Useable in Java and Javascript (other platforms/languages a bonus but not essential)
 - No need to worry about keyword/identifier clashes
 - Interpreted at runtime, i.e. no generate parser step.
 - Supports families of languages, i.e. grammar composition/extension
 - 
 
# Other
 - [Editor Integration and Demo](https://github.com/dhakehurst/net.akehurst.language.editor)

# Articles

 - [Creating a custom Domain Specific Language for text input validation](https://medium.com/javascript-in-plain-english/agl-your-dsl-in-the-web-c9f54595691b)
 - [Documentation](https://medium.com/@dr.david.h.akehurst/a-kotlin-multi-platform-parser-usable-from-a-jvm-or-javascript-59e870832a79)

# TODO

lots:
 - performance improvements
 - PT is not always what one expects
 
# Build
There are currently a number of failing tests, these indicate things to be fixed, and desired performance improvements.
To build locally the latest version, use `assemble` or `publishToMavenLocal`.
```
> cd net.akehurst.language
> ./gradlew clean assemble
```
