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

import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.completion.api.*
import net.akehurst.language.completion.asm.*
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.LiteralExpression
import net.akehurst.language.expressions.api.VariableAssignmentStatement
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.locationForNode

internal class AglCompletionSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AglCompletionDomain>() {

    companion object {
        val ITEM_TYPE_NAME = "CompletionItem"
        val ITEM_PROPERTIES = setOf("kind", "label", "text", "description")
        val DECORATE_PROPERTIES = setOf("label", "text", "description")
    }

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::definition)
        super.register(this::completion)
        super.register(this::completionRule)
        super.register(this::forItem)
        super.register(this::context)
        super.register(this::completionAction)
        super.register(this::add)
        super.register(this::replace)
        super.register(this::itemList)
        super.register(this::hide)
        super.register(this::decorate)
        super.register(this::depth)
    }

    // override unit from BaseSyntaxAnalyser
    // unit = option* namespace* ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglCompletionDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespace = children[1] as List<CompletionNamespace>
        val optHolder = OptionHolderDefault(null, options.toMap())
        namespace.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        return AglCompletionDomainDefault(SimpleName("Unit"), optHolder, namespace)
    }

    // override namespace from BaseSyntaxAnalyser
    // namespace = 'namespace' possiblyQualifiedName option* import* definition* ;
    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, String>>
        val import = children[3] as List<Import>
        val definition = children[4] as List<(ns: CompletionNamespace) -> CompletionDefinition>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val ns = CompletionNamespaceDefault(pqn.asQualifiedName(null), optHolder, import)
        definition.forEach {
            val def = it.invoke(ns)
            ns.addDefinition(def)
        }
        return ns
    }

    // override definition = completion ;
    fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: CompletionNamespace) -> CompletionDefinition =
        children[0] as ((ns: CompletionNamespace) -> CompletionDefinition)

    // completion = 'completion' IDENTIFIER '{' option* completionRule* '}' ;
    fun completion(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: CompletionNamespace) -> CompletionSet {
        val name = SimpleName(children[1] as String)
        val optionsList = children[3] as List<Pair<String, Any>>
        val rules = (children[4] as List<CompletionRule?>).filterNotNull()
        val options = OptionHolderDefault(null, optionsList.toMap())
        return { ns -> CompletionSetDefault(ns, name, options, rules) }
    }

    // completionRule = context? forItem '->' completionAction ;
    fun completionRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionRule? {
        val context = children[0] as String?
        val (forItem, forLiteral) = children[1] as Pair<String, Boolean>
        val action = children[3] as CompletionAction?
        return action?.let { CompletionRuleDefault(context, forItem, it, forLiteral).also { setLocationFor(it, nodeInfo, sentence) } }
    }

    // forItem = IDENTIFIER | STRING ;
    // (name, true) for a literal (its text, without the quotes)
    fun forItem(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Boolean> {
        val str = children[0] as String
        return when (nodeInfo.alt.option.asIndex) {
            0 -> Pair(str, false)
            else -> Pair(CompletionRuleDefault.unquoteLiteral(str), true)
        }
    }

    // context = 'in' IDENTIFIER ;
    fun context(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[1] as String

    // completionAction = add | replace | hide | decorate | depth ;
    fun completionAction(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction? =
        children[0] as CompletionAction?

    // add = 'add' itemList ;
    fun add(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction =
        CompletionActionAddDefault(children[1] as List<CompletionItemDefinition>)

    // replace = 'replace' itemList ;
    fun replace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction =
        CompletionActionReplaceDefault(children[1] as List<CompletionItemDefinition>)

    // itemList = '[' [expression / ',']* ']' ;
    fun itemList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<CompletionItemDefinition> {
        val expressions = (children[1] as List<Any>).toSeparatedList<Any, Expression, String>().items
        return expressions.mapNotNull { exp -> itemDefinitionFor(exp, nodeInfo, sentence) }
    }

    // hide = 'hide' ;
    fun hide(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction =
        CompletionActionHideDefault

    // decorate = 'decorate' propertyAssignmentBlock ;
    fun decorate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction? {
        val assignments = children[1] as List<VariableAssignmentStatement>
        val values = expressionValuesFor(assignments, DECORATE_PROPERTIES, nodeInfo, sentence)
        return CompletionActionDecorateDefault(values["label"], values["text"], values["description"])
    }

    // depth = 'depth' POSITIVE_INTEGER ;
    fun depth(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CompletionAction =
        CompletionActionDepthDefault((children[1] as String).toInt())

    private fun itemDefinitionFor(exp: Expression, nodeInfo: SpptDataNodeInfo, sentence: Sentence): CompletionItemDefinition? {
        val location = sentence.locationForNode(nodeInfo.node)
        return when {
            exp is CreateObjectExpression && ITEM_TYPE_NAME == exp.possiblyQualifiedTypeName.simpleName.value -> {
                val values = expressionValuesFor(exp.propertyAssignments, ITEM_PROPERTIES, nodeInfo, sentence)
                val kindExp = values["kind"]
                val kindName = when (kindExp) {
                    null -> CompletionItemKind.SEGMENT.name
                    is LiteralExpression -> kindExp.value as String
                    else -> null
                }
                val kind = CompletionItemKind.entries.firstOrNull { it.name == kindName }
                when {
                    null == kindName -> {
                        issues.error(location, "Value of 'kind' must be a literal string, one of ${CompletionItemKind.entries.joinToString { it.name }}")
                        null
                    }

                    null == kind -> {
                        issues.error(location, "Unknown kind '$kindName', expected one of ${CompletionItemKind.entries.joinToString { it.name }}")
                        null
                    }

                    null == values["text"] -> {
                        issues.error(location, "A $ITEM_TYPE_NAME needs a 'text'")
                        null
                    }

                    else -> CompletionItemDefinitionDefault(kind, values["label"], values["text"]!!, values["description"])
                }
            }

            else -> {
                issues.error(location, "Expected '$ITEM_TYPE_NAME() { ... }', found '$exp'")
                null
            }
        }
    }

    /**
     * the expression of each property assignment, an issue for any other property, or for a literal value that is not a String
     * (other expressions are evaluated when items are provided, see CompletionProviderFromDomain)
     */
    private fun expressionValuesFor(assignments: List<VariableAssignmentStatement>, allowed: Set<String>, nodeInfo: SpptDataNodeInfo, sentence: Sentence): Map<String, Expression> {
        val location = sentence.locationForNode(nodeInfo.node)
        val result = mutableMapOf<String, Expression>()
        for (asg in assignments) {
            val name = asg.variable.name
            val rhs = asg.rhs
            when {
                name !in allowed -> issues.error(location, "Unknown property '$name', expected one of ${allowed.joinToString()}")
                rhs is LiteralExpression && rhs.value !is String -> issues.error(location, "Value of '$name' must be a String")
                else -> result[name] = rhs
            }
        }
        return result
    }
}
