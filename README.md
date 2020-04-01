[![Build Status](https://travis-ci.org/dhakehurst/net.akehurst.language.svg?branch=master)](https://travisci.org/dhakehurst/net.akehurst.language)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.akehurst.language/agl-processor/badge.svg)](https://maven-badges.herokuapp.com/maven-central/language/agl-processor)
[ ![Bintray](https://api.bintray.com/packages/dhakehurst/maven/net.akehurst.language/images/download.svg) ](https://bintray.com/dhakehurst/maven/net.akehurst.language/_latestVersion)

# AGL: A Grammar Language (or maybe Akehurst Grammar Language!)
### net.akehurst.language

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


# TODO

lots:
 - performance improvements
 - PT is not always what one expects
 
