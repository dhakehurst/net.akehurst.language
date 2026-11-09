/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.completion.processor

import net.akehurst.language.completion.builder.completionDomain
import net.akehurst.language.agl.Agl
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
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

/**
 * The AGL completion language: customises the completion items offered for a language.
 *
 * ```
 * namespace net.akehurst.language.sql
 * completion SQL {
 *   terminatedStatement -> replace [
 *     CompletionItem() { label := 'Select statement' text := 'SELECT <columns> FROM <table-id>;' description := 'query rows of a table' }
 *   ]
 *   SELECT -> decorate { description := 'select or SELECT' }
 *   COMMENT -> hide
 *   in select columns -> depth 2
 * }
 * ```
 * A rule applies where the named grammar rule (or leaf) is expected next, and with `in <rule>` only inside that rule (at any level).
 * The actions are
 * - `add [ items ]` add the items to the generated ones
 * - `replace [ items ]` leave out the generated items for the rule and add the items
 * - `hide` leave out the generated items for the rule
 * - `decorate { label := .. text := .. description := .. }` change the generated items for the rule
 * - `depth n` expand the rule to n levels of rule references
 *
 * An item is `CompletionItem() { kind := '..' label := '..' text := '..' description := '..' }`,
 * kind is one of LITERAL, PATTERN, SEGMENT (default), REFERRED. Currently the values must be literal strings.
 */
object AglCompletion : LanguageObjectAbstract<AglCompletionDomain, SentenceContext>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Completion"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) } //TODO: should be AglExpressions, as for AglFormat

    override val grammarString = """
        namespace $NAMESPACE_NAME
        grammar $NAME : ${AglExpressions.NAME} {
            override definition = completion ;
            completion = 'completion' IDENTIFIER '{' option* completionRule* '}' ;
            completionRule = context? IDENTIFIER '->' completionAction ;
            context = 'in' IDENTIFIER ;
            completionAction = add | replace | hide | decorate | depth ;
            add = 'add' itemList ;
            replace = 'replace' itemList ;
            itemList = '[' [expression / ',']* ']' ;
            hide = 'hide' ;
            decorate = 'decorate' propertyAssignmentBlock ;
            depth = 'depth' POSITIVE_INTEGER ;
        }
    """.trimIndent()

    override val typesString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val kompositeString: String = """
        namespace $NAMESPACE_NAME.completion.api
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
        namespace $NAMESPACE_NAME
          styles $NAME : ${AglBase.NAME} {

          }
      """

    override val formatString: String = """
        namespace $NAMESPACE_NAME
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
                        ref("completion")
                    }
                    concatenation("completion") {
                        lit("completion"); ref("IDENTIFIER"); lit("{")
                        lst(0, -1) { ref("option") }
                        lst(0, -1) { ref("completionRule") }
                        lit("}")
                    }
                    concatenation("completionRule") {
                        opt { ref("context") }; ref("IDENTIFIER"); lit("->"); ref("completionAction")
                    }
                    concatenation("context") { lit("in"); ref("IDENTIFIER") }
                    choice("completionAction") {
                        ref("add")
                        ref("replace")
                        ref("hide")
                        ref("decorate")
                        ref("depth")
                    }
                    concatenation("add") { lit("add"); ref("itemList") }
                    concatenation("replace") { lit("replace"); ref("itemList") }
                    concatenation("itemList") {
                        lit("["); spLst(0, -1) { ref("expression"); lit(",") }; lit("]")
                    }
                    concatenation("hide") { lit("hide") }
                    concatenation("decorate") { lit("decorate"); ref("propertyAssignmentBlock") }
                    concatenation("depth") { lit("depth"); ref("POSITIVE_INTEGER") }
                }
            }
        }
    }

    override val typesDomain: TypesDomain by lazy {
        typesDomain(NAME, true, AglBase.typesDomain.namespace) { }
    }

    override val asmTransformDomain: AsmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typesDomain = typesDomain,
            createTypes = false
        ) {
            namespace(NAMESPACE_NAME) {
                ruleSet(NAME) {
                    //TODO
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

    override val syntaxAnalyser: SyntaxAnalyser<AglCompletionDomain>? by lazy { AglCompletionSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<AglCompletionDomain, SentenceContext>? by lazy { AglCompletionSemanticAnalyser() }
    override val completionProvider: CompletionProvider<AglCompletionDomain, SentenceContext>? by lazy { AglCompletionCompletionProvider() }
}
