/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.style.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.regex.api.EscapedPattern
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.style.api.*
import net.akehurst.language.style.asm.*

internal class AglStyleSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AglStyleModel>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<AglStyleModel>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::styleSet)
        super.register(this::rule)
        super.register(this::metaRule)
        super.register(this::tagRule)
        super.register(this::selectorExpression)
        super.register(this::selectorAndComposition)
        super.register(this::selectorSingle)
        super.register(this::styleList)
        super.register(this::style)
        super.register(this::styleValue)
    }

    private val _localStore = mutableMapOf<String, Any>()

    // unit = namespace rule* ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleModelDefault {
        val ns = children[0] as StyleNamespaceDefault
        val ruleBuilder = children[1] as List<((ns: StyleNamespaceDefault) -> Unit)>
        ruleBuilder.forEach { it.invoke(ns) }
        val su = AglStyleModelDefault(name = SimpleName("ParsedStyleUnit"), namespace = listOf(ns))
        return su
    }

    // namespace = namespace possiblyQualifiedName ;
    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): StyleNamespace {
        val qualifiedName = children[1] as PossiblyQualifiedName
        val imports = emptyList<Import>()
        return StyleNamespaceDefault(qualifiedName = qualifiedName.asQualifiedName(null), import = imports)
    }

    // styleSet = 'styles' IDENTIFIER extends? '{' rule* '}' ;
    fun styleSet(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: StyleNamespaceDefault) -> Unit {
        val name = SimpleName(children[1] as String)
        val extends = (children[2] as List<StyleSetReference>?) ?: emptyList()
        val rules: List<AglStyleTagRule> = children[4] as List<AglStyleTagRule>
        return { ns ->
            val ss = AglStyleSetDefault(ns, name, extends)
                .also { setLocationFor(it, nodeInfo, sentence) }
            (ss.rules as MutableList).addAll(rules)
        }
    }

    // extends = ':' [possiblyQualifiedName / ',']+ ;
    fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<StyleSetReference> {
        val localNamespace = _localStore["namespace"] as StyleNamespace
        val extendNameList = children[1] as List<PossiblyQualifiedName>
        val sl = extendNameList.toSeparatedList<Any, PossiblyQualifiedName, String>()
        val extended = sl.items.map {
            // need to manually add the Reference as it is not seen by the super class
            StyleSetReferenceDefault(localNamespace, it).also { setLocationFor(it, nodeInfo, sentence) }
        }
        return extended
    }

    //  rule = metaRule | styleRule ;
    fun rule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleRule =
        children[0] as AglStyleRule

    // metaRule = '$$' PATTERN '{' styleList '}' ;
    fun metaRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleMetaRule {
        val patternStr = children[1] as String //TODO: ? selector combinations, and/or/contains etc
        val escaped = EscapedPattern(patternStr.removeSurrounding("\""))
        val styles: List<AglStyleDeclaration> = children[3] as List<AglStyleDeclaration>
        val rule = AglStyleMetaRuleDefault(escaped)
        styles.forEach { rule.declaration[it.name] = it }
        return rule
    }

    // tagRule = selectorExpression '{' styleList '}' ;
    fun tagRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleTagRule {
        val selector = children[0] as List<AglStyleSelector> //TODO: ? selector combinations, and/or/contains etc

        val styles: List<AglStyleDeclaration> = children[2] as List<AglStyleDeclaration>
        val rule = AglStyleTagRuleDefault(selector)
        styles.forEach { rule.declaration[it.name] = it }
        return rule
    }

    // selectorExpression
    //             = selectorAndComposition
    //             | selectorSingle
    //             ; //TODO
    fun selectorExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        when (nodeInfo.alt.option.asIndex) {
            0 -> children[0] as List<AglStyleSelector>
            1 -> listOf(children[0] as AglStyleSelector)
            else -> error("Internal error: alternative 'selectorExpression' not handled")
        }


    // selectorAndComposition = [selectorSingle /',']2+ ;
    fun selectorAndComposition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        (children as List<Any>).toSeparatedList<Any, AglStyleSelector, String>().items

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER | SPECIAL_IDENTIFIER ;
    fun selectorSingle(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleSelector {
        // Must match what is done in AglGrammarSyntaxAnalyser.terminal,
        // but keep the enclosing (single or double) quotes
        val isPattern = nodeInfo.node.rule.tag == "PATTERN"
        val mt = children[0] as String
        //val escaped = mt.substring(1, mt.length - 1)
        val value = if (isPattern) {
            mt.replace("\\\"", "\"")
        } else {
            mt.replace("\\'", "'").replace("\\\\", "\\")
        }
        val kind = when (nodeInfo.alt.option.asIndex) {
            0 -> AglStyleSelectorKind.LITERAL
            1 -> AglStyleSelectorKind.PATTERN
            2 -> AglStyleSelectorKind.RULE_NAME
            3 -> AglStyleSelectorKind.SPECIAL
            else -> error("Internal error: AglStyleSelectorKind not handled")
        }
        //val str = target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
        //return listOf(value)
        return AglStyleSelector(mt, kind)
    }

    // styleList = style* ;
    fun styleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleDeclaration> =
        children as List<AglStyleDeclaration>

    // style = STYLE_ID ':' styleValue ';' ;
    fun style(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleDeclaration {
        val name = children[0] as String
        val value = children[2] as String
        return AglStyleDeclaration(name, value)
    }

    // styleValue = STYLE_VALUE | STRING ;
    fun styleValue(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

}