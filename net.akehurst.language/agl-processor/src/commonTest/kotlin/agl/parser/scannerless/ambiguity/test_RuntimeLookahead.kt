/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_RuntimeLookahead : test_ScanOnDemandParserAbstract() {

    // Abstraction of the issue from the Java8 grammar
    // compilationUnit = packageDeclaration? importDeclaration* typeDeclaration*
    // packageDeclaration = annotation* PACKAGE qualifiedName ';'
    // importDeclaration  = IMPORT STATIC? qualifiedName ('.' '*')? ';'
    // typeDeclaration   = classOrInterfaceModifier* (classDeclaration | enumDeclaration | interfaceDeclaration | annotationTypeDeclaration)
    // classOrInterfaceModifier = annotation | PUBLIC
    // annotation   = '@' qualifiedName ('(' ( elementValuePairs | elementValue )? ')')?
    /*
        S = P? T?      // compilationUnit = packageDeclaration? typeDeclaration?
        P = A? p       // packageDeclaration = annotation? 'package'
        T = A? t       // typeDeclaration = annotation? 'type'
        A = a n G?     // annotation = '@' 'qualifiedName' arguments
        G = s A?
     */
    /*
       S = oP oT
       oP = P?
       oT = T?
       P = oA p
       T = oA t
       oA = A?
       A = a n oG
       oG = G?
       G = s oA
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("oP"); ref("oT") }
            optional("oP", "P")
            optional("oT", "T")
            concatenation("P") { ref("oA"); literal("p") }
            concatenation("T") { ref("oA"); literal("t") }
            optional("oA", "A")
            concatenation("A") { literal("a"); literal("n"); ref("oG") }
            optional("oG", "G")
            concatenation("G") { literal("s"); ref("oA") }
            preferenceFor("<EMPTY>") {
                leftOption("oA", 1, setOf("'t'"))
                leftOption("oP", 1, setOf("'t'"))
            }
        }

        val goal = "S"
    }

    @Test
    fun p() {
        val sentence = "p"

        val expected = """
            S {
              oP { P {
                  oA { §empty }
                  'p'
                } }
              oT { §empty }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun t() {
        val sentence = "t"

        val expected = """
            S {
              oP { §empty }
              oT { T {
                  oA { §empty }
                  't'
                } }
            }
        """

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun anp() {
        val sentence = "anp"

        val expected = """
            S {
              oP { P {
                  oA { A { 'a'  'n'
                      oG { §empty }
                    } }
                  'p'
                } }
              oT { §empty }
            }
        """

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ansant() {
        val sentence = "ansant"

        val expected = """
            S {
              oP { §empty }
              oT { T {
                  oA { A {
                      'a'
                      'n'
                      oG { G {
                          's'
                          oA { A {
                              'a'
                              'n'
                              oG { §empty }
                            } }
                        } }
                    } }
                  't'
                } }
            }
        """

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ansansant() {
        val sentence = "ansansant"

        val expected = """
            S {
              oP { §empty }
              oT { T {
                  oA { A {
                      'a'
                      'n'
                      oG { G {
                          's'
                          oA { A {
                              'a'
                              'n'
                              oG { G {
                                  's'
                                  oA { A {
                                      'a'
                                      'n'
                                      oG { §empty }
                                    } }
                                } }
                            } }
                        } }
                    } }
                  't'
                } }
            }
        """

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }
}