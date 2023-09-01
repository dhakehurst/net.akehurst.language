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
package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleRule

internal class AglStyleSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AglStyleModel>() {

    companion object {
        //not sure if this should be here or in grammar object
        const val KEYWORD_STYLE_ID = "\$keyword"
    }

    override fun registerHandlers() {
        super.register(this::rules)
        super.register(this::rule)
        super.register(this::selectorExpression)
        super.register(this::selectorAndComposition)
        super.register(this::selectorSingle)
        super.register(this::styleList)
        super.register(this::style)
    }

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<AglStyleModel>> = emptyMap()

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    // rules : rule* ;
    fun rules(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleModelDefault =
        AglStyleModelDefault(children as List<AglStyleRule>)

    // rule = selectorExpression '{' styleList '}' ;
    fun rule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleRule {
        val selector = children[0] as List<String> //TODO: ? selector combinations, and/or/contains etc
        val rule = AglStyleRule(selector)
        val styles: List<AglStyle> = children[2] as List<AglStyle>
        styles.forEach { rule.styles[it.name] = it }
        return rule
    }

    // selectorExpression
    //             = selectorSingle
    //             | selectorAndComposition
    //             ; //TODO
    fun selectorExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        children[0] as List<String>

    // selectorAndComposition = [selectorSingle /',']2+ ;
    fun selectorAndComposition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        (children as List<String>).toSeparatedList<List<String>, String>().items.flatten()

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    fun selectorSingle(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
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
        //val str = target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
        //return listOf(value)
        return listOf(mt)
    }

    // styleList = style* ;
    fun styleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyle> =
        children as List<AglStyle>

    // style = STYLE_ID ':' STYLE_VALUE ';' ;
    fun style(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyle {
        val name = children[0] as String
        val value = children[2] as String
        return AglStyle(name, value)
    }
}