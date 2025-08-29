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
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain

fun TypesDomain.findTypeForRule(ruleName: GrammarRuleName): TypeInstance? {
    return this.namespace.firstNotNullOfOrNull { ns ->
        when (ns) {
            is GrammarTypesNamespace -> ns.findTypeForRule(ruleName)
            else -> null
        }
    }
}

fun contextFromGrammar(grammars: GrammarDomain): ContextWithScope<Any, Any> {
    val proc = Agl.registry.agl.grammar.processor!!
    val aglGrammarTypeModel = proc.typesDomain
    val context = ContextWithScope<Any, Any>()
    grammars.allDefinitions.forEach { g ->
        val scope = context.newScopeForSentence(g.qualifiedName.toString())
        g.allResolvedGrammarRule.forEach {
            val rType = aglGrammarTypeModel.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Type not found for rule '${it.name}'")
            scope.addToScope(it.name.value, rType.resolvedDeclaration.qualifiedName, null, it.name.value,  false)
        }
        g.allResolvedTerminal.forEach {
            val rTypeName = when {
                it.isPattern -> "PATTERN" //namespace.findTypeUsageForRule("PATTERN") ?: error("Type not found for rule 'PATTERN'")
                else -> "LITERAL" //namespace.findTypeUsageForRule("LITERAL") ?: error("Type not found for rule 'LITERAL'")
            }
            scope.addToScope(it.id, QualifiedName(rTypeName), null, it.escapedValue, false)
        }
    }
    return context
}
/*
// used by other languages that reference rules  in a grammar
class ContextFromGrammar(
) : SentenceContext {
    companion object {

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

 */