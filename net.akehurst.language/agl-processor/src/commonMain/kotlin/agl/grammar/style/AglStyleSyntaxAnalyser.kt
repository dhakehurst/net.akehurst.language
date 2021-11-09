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
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyle
import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree


internal class AglStyleSyntaxAnalyser : SyntaxAnalyserAbstract<List<AglStyleRule>,SentenceContext>() {

    private val issues = mutableListOf<LanguageIssue>()

    init {
        this.register("rules", this::rules as BranchHandler<List<AglStyleRule>>)
        this.register("rule", this::rule as BranchHandler<AglStyleRule>)
        this.register("selectorExpression", this::selectorExpression as BranchHandler<String>)
        this.register("selectorSingle", this::selectorSingle as BranchHandler<String>)
        this.register("styleList", this::styleList as BranchHandler<List<AglStyle>>)
        this.register("style", this::style as BranchHandler<AglStyle>)

    }

    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext, configuration: String): List<LanguageIssue> {
        return emptyList()
    }
    override fun transform(sppt: SharedPackedParseTree, context: SentenceContext?): Pair<List<AglStyleRule>, List<LanguageIssue>> {
        val rules:List<AglStyleRule> =  this.transformBranch(sppt.root.asBranch, "")

        if (null != context) {
            rules.forEach {
                if (context.rootScope.isMissing(it.selector, "Rule")) {
                    val loc = this.locationMap[AglScopesSyntaxAnalyser.PropertyValue(it, "typeReference")]
                    issues.raise(loc, "Rule '${it.selector}' not found for scope")
                }
            }
        }

        return Pair(rules, emptyList()) //TODO
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
                this.transformBranch<AglStyleRule>(it, arg)
            }
        }
    }

    // rule = selectorExpression '{' styleList '}' ;
    fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyleRule {
        val selector = children[0].nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")  //TODO: ? selector combinations, and/or/contains etc
        val rule = AglStyleRule(selector)
        val styles:List<AglStyle> = this.transformBranch(children[1], arg)
        styles.forEach {
            rule.styles[it.name] = it
        }
        return rule
    }

    // selectorExpression = selectorSingle ; //TODO
    fun selectorExpression(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return transformBranch<String>(children[0],arg)
    }

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER ;
    fun selectorSingle(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText.replace("\\\\", "\\").replace("\\\"", "\"")
    }

    // styleList = style* ;
    fun styleList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?) : List<AglStyle> {
        return children.mapIndexed { index, it ->
            this.transformBranch<AglStyle>(it, arg)
        }
    }

    // style = STYLE_ID ':' STYLE_VALUE ';' ;
    fun style(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): AglStyle {
        val name = target.nonSkipChildren[0].nonSkipMatchedText
        val value = target.nonSkipChildren[2].nonSkipMatchedText
        return AglStyle(name, value)
    }
}