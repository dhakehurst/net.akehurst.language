/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.m2mTransformTest.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.format.builder.formatDomain
import net.akehurst.language.agl.processor.contextFromLanguageObject
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.completion.api.AglCompletionDomain
import net.akehurst.language.completion.builder.completionDomain
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestDomain
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

/**
 * tests of m2m transforms ('transform-test Name for Transform { ... }'), see 5_Documentation/dsl/m2m-transform-test.md
 */
object M2mTransformTest : LanguageObjectAbstract<M2mTransformTestDomain, SentenceContext>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "M2mTransformTest"
    const val goalRuleName = "unit"

    /** extension of a file that holds transform tests */
    const val FILE_EXTENSION = "agl-mtt"

    /** type name under which an M2mTransformRuleSet is put in a sentence context, see [contextFromM2mTransformDomain] */
    val transformRuleSetItemTypeName = QualifiedName("net.akehurst.language.m2mTransform.api.M2mTransformRuleSet")

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) } //TODO: should be AglExpressions, as for AglCompletion

    override val grammarString: String = """
namespace $NAMESPACE_NAME
  grammar $NAME : Expressions {
    override definition = transformTest ;
    transformTest = 'transform-test' IDENTIFIER 'for' possiblyQualifiedName '{' testCase* '}' ;
    testCase = 'test-case' IDENTIFIER '{' testDomain{2+} '}' ;
    testDomain = 'domain' domainReference ':=' expression ;
    leaf domainReference = IDENTIFIER ;
  }
    """.trimIndent()

    override val typesString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val kompositeString: String = """
        namespace net.akehurst.language.m2mTransformTest.api
          // TODO
    """.trimIndent()

    override val asmTransformString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val styleString: String = """
        namespace ${NAMESPACE_NAME}
          styles ${NAME} : ${AglBase.NAME} {

          }
      """

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val completionString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val grammarDomain by lazy {
        grammarDomain(name = NAME, registry = Agl.registry) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglExpressions.defaultTargetGrammar.selfReference)
                    concatenation("definition", overrideKind = OverrideKind.REPLACE) {
                        ref("transformTest")
                    }
                    concatenation("transformTest") {
                        lit("transform-test"); ref("IDENTIFIER"); lit("for"); ref("possiblyQualifiedName"); lit("{")
                        lst(0, -1) { ref("testCase") }
                        lit("}")
                    }
                    concatenation("testCase") {
                        lit("test-case"); ref("IDENTIFIER"); lit("{")
                        lst(2, -1) { ref("testDomain") }
                        lit("}")
                    }
                    concatenation("testDomain") {
                        lit("domain"); ref("domainReference"); lit(":="); ref("expression")
                    }
                    concatenation("domainReference", isLeaf = true) { ref("IDENTIFIER") }
                }
            }
        }
    }

    override val typesDomain: TypesDomain by lazy {
        typesDomain(NAME, true, AglBase.typesDomain.namespace) {
            //TODO: the types of the M2mTransformTest domain
            namespace("net.akehurst.language.m2mTransformTest.api", listOf("std", "net.akehurst.language.base.api")) {
                interface_("M2mTransformTestDomain") {}
            }
        }
    }

    override val asmTransformDomain: AsmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typesDomain = typesDomain,
            createTypes = false
        ) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                ruleSet(NAME) {
                    //TODO: custom SyntaxAnalyser, the rules are not used
                    importTypes("net.akehurst.language.m2mTransformTest.api")
                    createObject("unit", "M2mTransformTestDomain") {
                    }
                }
            }
        }
    }

    override val crossReferenceDomain: CrossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {
            //TODO
        }
    }

    override val styleDomain by lazy {
        styleDomain(NAME, sentenceContext = contextFromGrammar(AglStyle.grammarDomain).union(contextFromLanguageObject(listOf(AglBase)))) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    extends(AglBase.NAME)
                }
            }
        }
    }

    override val formatDomain by lazy {
        formatDomain(NAME) {
        }
    }

    override val completionDomain: AglCompletionDomain by lazy {
        completionDomain(NAME) {
        }
    }

    override val defaultTargetGrammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = goalRuleName

    override val syntaxAnalyser: SyntaxAnalyser<M2mTransformTestDomain>? by lazy { M2mTransformTestSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<M2mTransformTestDomain, SentenceContext>? by lazy { M2mTransformTestSemanticAnalyser() }
    override val completionProvider: CompletionProvider<M2mTransformTestDomain, SentenceContext>? by lazy { M2mTransformTestCompletionProvider() }

    override fun toString(): String = "${NAMESPACE_NAME}.${NAME}"
}

class M2mTransformTestCompletionProvider : CompletionProviderAbstract<M2mTransformTestDomain, SentenceContext>()
