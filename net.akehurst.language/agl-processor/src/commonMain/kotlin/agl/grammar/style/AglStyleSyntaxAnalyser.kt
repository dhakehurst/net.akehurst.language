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

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleRule


internal class AglStyleSyntaxAnalyser : SyntaxAnalyser<List<AglStyleRule>, SentenceContext<GrammarItem>> {

    companion object {
        //not sure if this should be here or in grammar object
        const val KEYWORD_STYLE_ID = "\$keyword"
    }

    override val locationMap = mutableMapOf<Any, InputLocation>()
    private val _issues = mutableListOf<LanguageIssue>()

    override fun clear() {
        locationMap.clear()
        _issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: String): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem, context: SentenceContext<GrammarItem>?): Pair<List<AglStyleRule>, List<LanguageIssue>> {
        val rules: List<AglStyleRule> = this.rules(sppt.root.asBranch, sppt.root.asBranch.branchNonSkipChildren, "")

        //TODO: should this be semanticAnalysis ?
        if (null != context) {
            rules.forEach { rule ->
                rule.selector.forEach { sel ->
                    if (KEYWORD_STYLE_ID == sel) {
                        //it is ok
                    } else {
                        if (context.rootScope.isMissing(sel, ContextFromGrammar.GRAMMAR_RULE_CONTEXT_TYPE_NAME) &&
                            context.rootScope.isMissing(sel, ContextFromGrammar.GRAMMAR_TERMINAL_CONTEXT_TYPE_NAME)
                        ) {
                            val loc = this.locationMap[rule]
                            if (sel.startsWith("'") && sel.endsWith("'")) {
                                _issues.raise(loc, "Terminal Literal ${sel} not found for style rule")
                            } else if (sel.startsWith("\"") && sel.endsWith("\"")) {
                                _issues.raise(loc, "Terminal Pattern ${sel} not found for style rule")

                            } else {
                                _issues.raise(loc, "GrammarRule '${sel}' not found for style rule")
                            }
                        } else {
                            //no issues
                        }
                    }
                }
            }
        }

        return Pair(rules, _issues) //TODO
    }

    private fun MutableList<LanguageIssue>.raise(location: InputLocation?, message: String) {
        this.add(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, location, message))
    }

    // rules : rule* ;
    fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<AglStyleRule> {
        return if (children.isEmpty()) {
            emptyList()
        } else {
            children.mapIndexed { index, it ->
                this.rule(it, it.branchNonSkipChildren, arg)
            }
        }
    }

    // rule = selectorExpression '{' styleList '}' ;
    fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyleRule {
        val selector = selectorExpression(children[0], children[0].branchNonSkipChildren, arg)  //TODO: ? selector combinations, and/or/contains etc
        val rule = AglStyleRule(selector)
        val styles: List<AglStyle> = this.styleList(children[1], children[1].branchNonSkipChildren, arg)
        styles.forEach {
            rule.styles[it.name] = it
        }
        val asm = rule
        this.locationMap[asm] = target.location
        return asm
    }

    // selectorExpression
    //             = selectorSingle
    //             | selectorAndComposition
    //             ; //TODO
    fun selectorExpression(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<String> {
        return when (children[0].name) {
            "selectorSingle" -> selectorSingle(children[0], children[0].branchNonSkipChildren, arg)
            "selectorAndComposition" -> selectorAndComposition(children[0], children[0].branchNonSkipChildren, arg)
            else -> error("Internal Error (AglStyleSyntaxAnalyser):Unhandled choice in 'selectorExpression' of '${children[0].name}'")
        }
    }

    // selectorAndComposition = [selectorSingle /',']2+ ;
    fun selectorAndComposition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<String> {
        return children.flatMap {
            selectorSingle(it, it.branchNonSkipChildren, arg)
        }
    }

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    fun selectorSingle(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<String> {
        // Must match what is done in AglGrammarSyntaxAnalyser.terminal,
        // but keep the enclosing (single or double) quotes
        val isPattern = target.nonSkipChildren[0].name == "PATTERN"
        val mt = target.nonSkipMatchedText
        //val escaped = mt.substring(1, mt.length - 1)
        val value = if (isPattern) {
            mt.replace("\\\"", "\"")
        } else {
            mt.replace("\\'", "'").replace("\\\\", "\\")
        }
        //val str = target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
        return listOf(value)
    }

    // styleList = style* ;
    fun styleList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<AglStyle> {
        return children.mapIndexed { index, it ->
            this.style(it, it.branchNonSkipChildren, arg)
        }
    }

    // style = STYLE_ID ':' STYLE_VALUE ';' ;
    fun style(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyle {
        val name = target.nonSkipChildren[0].nonSkipMatchedText
        val value = target.nonSkipChildren[2].nonSkipMatchedText
        val asm = AglStyle(name, value)
        this.locationMap[asm] = target.location
        return asm
    }
}