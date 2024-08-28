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

package net.akehurst.language.agl.language.style

import net.akehurst.language.agl.api.language.base.QualifiedName
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.style.*
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList

internal class AglStyleSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AglStyleModel>() {

    override fun registerHandlers() {
        super.register(this::rules)
        super.register(this::rule)
        super.register(this::selectorExpression)
        super.register(this::selectorAndComposition)
        super.register(this::selectorSingle)
        super.register(this::styleList)
        super.register(this::style)
    }

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<AglStyleModel>> = emptyMap()

    // rules : rule* ;
    fun rules(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleModelDefault =
        AglStyleModelDefault(children as List<AglStyleRule>)

    // rule = selectorExpression '{' styleList '}' ;
    fun rule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleRule {
        val selector = children[0] as List<AglStyleSelector> //TODO: ? selector combinations, and/or/contains etc
        val rule = AglStyleRule(selector)
        val styles: List<AglStyleDeclaration> = children[2] as List<AglStyleDeclaration>
        styles.forEach { rule.styles[it.name] = it }
        return rule
    }

    // selectorExpression
    //             = selectorAndComposition
    //             | selectorSingle
    //             ; //TODO
    fun selectorExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        when (nodeInfo.alt.option) {
            0 -> children[0] as List<AglStyleSelector>
            1 -> listOf(children[0] as AglStyleSelector)
            else -> error("Internal error: alternative 'selectorExpression' not handled")
        }


    // selectorAndComposition = [selectorSingle /',']2+ ;
    fun selectorAndComposition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        (children as List<Any>).toSeparatedList<Any, AglStyleSelector, String>().items

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER | META_IDENTIFIER ;
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
        val kind = when (nodeInfo.alt.option) {
            0 -> AglStyleSelectorKind.LITERAL
            1 -> AglStyleSelectorKind.PATTERN
            2 -> AglStyleSelectorKind.RULE_NAME
            3 -> AglStyleSelectorKind.META
            else -> error("Internal error: AglStyleSelectorKind not handled")
        }
        //val str = target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
        //return listOf(value)
        return AglStyleSelector(mt, kind)
    }

    // styleList = style* ;
    fun styleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleDeclaration> =
        children as List<AglStyleDeclaration>

    // style = STYLE_ID ':' STYLE_VALUE ';' ;
    fun style(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleDeclaration {
        val name = children[0] as String
        val value = children[2] as String
        return AglStyleDeclaration(name, value)
    }
}