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

package net.akehurst.language.agl.language.grammar

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ScopeSimple
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.semanticAnalyser.SentenceContext

// used by other languages that reference rules  in a grammar
class ContextFromGrammar(
) : SentenceContext<String> {
    companion object {
        fun createContextFrom(grammars: List<Grammar>): ContextFromGrammar {
            val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
            val namespace: GrammarTypeNamespace = aglGrammarTypeModel.namespace[Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName] as GrammarTypeNamespace? ?: error("")
            val context = ContextFromGrammar()
            val scope = ScopeSimple<String>(null, "", grammars.last().name)
            grammars.forEach { g ->
                g.allResolvedGrammarRule.forEach {
                    val rType = namespace.findTypeUsageForRule("grammarRule") ?: error("Type not found for rule '${it.name}'")
                    scope.addToScope(it.name, rType.type.qualifiedName, it.name)
                }
                g.allResolvedTerminal.forEach {
                    val rTypeName = when {
                        it.isPattern -> "PATTERN" //namespace.findTypeUsageForRule("PATTERN") ?: error("Type not found for rule 'PATTERN'")
                        else -> "LITERAL" //namespace.findTypeUsageForRule("LITERAL") ?: error("Type not found for rule 'LITERAL'")
                    }
                    scope.addToScope(it.name, rTypeName, it.name)
                }
            }
            context.rootScope = scope
            return context
        }
    }

    override var rootScope = ScopeSimple<String>(null, "", "")

    fun clear() {
        this.rootScope = ScopeSimple<String>(null, "", "")
    }

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromGrammar -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextFromGrammar"
}