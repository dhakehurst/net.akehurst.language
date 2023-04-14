package net.akehurst.language.agl.agl.grammar.style

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleRule

class AglStyleSemanticAnalyser : SemanticAnalyser<AglStyleModel, SentenceContext<String>> {
    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun analyse(asm: AglStyleModel, locationMap: Map<Any, InputLocation>?, context: SentenceContext<String>?, options:Map<String,Any>): SemanticAnalysisResult {
        val locMap = locationMap?: mapOf()
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (null != context) {
            asm.rules.forEach { rule ->
                rule.selector.forEach { sel ->
                    if (AglStyleSyntaxAnalyser.KEYWORD_STYLE_ID == sel) {
                        //it is ok
                    } else {
                        if (context.rootScope.isMissing(sel, ContextFromGrammar.GRAMMAR_RULE_CONTEXT_TYPE_NAME) &&
                            context.rootScope.isMissing(sel, ContextFromGrammar.GRAMMAR_TERMINAL_CONTEXT_TYPE_NAME)
                        ) {
                            val loc = locMap[rule]
                            if (sel.startsWith("'") && sel.endsWith("'")) {
                                issues.error(loc, "Terminal Literal ${sel} not found for style rule")
                            } else if (sel.startsWith("\"") && sel.endsWith("\"")) {
                                issues.error(loc, "Terminal Pattern ${sel} not found for style rule")

                            } else {
                                issues.error(loc, "GrammarRule '${sel}' not found for style rule")
                            }
                        } else {
                            //no issues
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(issues)
    }
}