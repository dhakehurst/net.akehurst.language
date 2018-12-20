# net.akehurst.language

AGL: A Grammar Language (or maybe Akehurst Grammar Language!)

There are many parser technologies, frameworks, generators, libraries
that already exist. When I first started this one in about 2007 there was none that
met all the requirements that I have. It has taken me several years (too many) to finally
reach the stage where I am happy to publish it. (mostly because it was a home project and not
worked on very frequently until the last couple of years, 2017/2018).

My Requirements:

 - Simple grammar for defining a language with no restrictions
 -- i.e. should support left & right recursive rules,
 - Useable in Java and Javascript (other platforms/languages a bonus but not essential)
 - Scanner-less, i.e. no need to worry about keyword/identifier clashes
 - Interpreted at runtime, i.e. no generate parser step.
 - Supports families of languages, i.e. grammar composition/extension
 - 