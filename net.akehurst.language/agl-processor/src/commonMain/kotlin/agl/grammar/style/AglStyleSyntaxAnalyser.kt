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

import net.akehurst.language.agl.grammar.scopes.AglScopesSyntaxAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleRule


internal class AglStyleSyntaxAnalyser : SyntaxAnalyser<List<AglStyleRule>, SentenceContext> {

    override val locationMap = mutableMapOf<Any, InputLocation>()
    private val _issues = mutableListOf<LanguageIssue>()

    override fun clear() {
        locationMap.clear()
        _issues.clear()
    }

    override fun configure(configurationContext: SentenceContext, configuration: String): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, context: SentenceContext?): Pair<List<AglStyleRule>, List<LanguageIssue>> {
        val rules: List<AglStyleRule> = this.rules(sppt.root.asBranch, sppt.root.asBranch.branchNonSkipChildren, "")

        if (null != context) {
            rules.forEach {
                if (context.rootScope.isMissing(it.selector, "Rule")) {
                    val loc = this.locationMap[it.selector]
                    _issues.raise(loc, "Rule '${it.selector}' not found for scope")
                }else {
                    //no issues
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
        val selector = children[0].nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")  //TODO: ? selector combinations, and/or/contains etc
        val rule = AglStyleRule(selector)
        val styles: List<AglStyle> = this.styleList(children[1], children[1].branchNonSkipChildren, arg)
        styles.forEach {
            rule.styles[it.name] = it
        }
        val asm = rule
        this.locationMap[asm] = target.location
        return asm
    }

    // selectorExpression = selectorSingle ; //TODO
    fun selectorExpression(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return selectorSingle(children[0], children[0].branchNonSkipChildren, arg)
    }

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    fun selectorSingle(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
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
        val asm= AglStyle(name, value)
        this.locationMap[asm] = target.location
        return asm
    }
}