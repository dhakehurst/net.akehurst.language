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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.api.semanticAnalyser.SentenceContext

// used by other languages that reference rules  in a grammar
class ContextFromGrammar(
) : SentenceContext<String> {
    companion object {
        fun createContextFrom(grammars: GrammarModel): ContextFromGrammar {
            val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
            val namespace: GrammarTypeNamespace =
                aglGrammarTypeModel.findNamespaceOrNull(Agl.registry.agl.grammar.processor!!.targetGrammar!!.qualifiedName) as GrammarTypeNamespace? ?: error("")
            val context = ContextFromGrammar()
            val scope = ScopeSimple<String>(null, grammars.primary!!.name.value, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
            grammars.allDefinitions.forEach { g ->
                g.allResolvedGrammarRule.forEach {
                    val rType = namespace.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Type not found for rule '${it.name}'")
                    scope.addToScope(it.name.value, rType.resolvedDeclaration.qualifiedName, it.name.value)
                }
                g.allResolvedTerminal.forEach {
                    val rTypeName = when {
                        it.isPattern -> "PATTERN" //namespace.findTypeUsageForRule("PATTERN") ?: error("Type not found for rule 'PATTERN'")
                        else -> "LITERAL" //namespace.findTypeUsageForRule("LITERAL") ?: error("Type not found for rule 'LITERAL'")
                    }
                    scope.addToScope(it.id, QualifiedName(rTypeName), it.value)
                }
            }
            context.rootScope = scope
            return context
        }
    }

    var rootScope = ScopeSimple<String>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    fun clear() {
        this.rootScope = ScopeSimple<String>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
    }

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromGrammar -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextFromGrammar"
}