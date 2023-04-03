package net.akehurst.language.agl.agl.grammar.format

import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SentenceContext

class AglFormatSemanticAnalyser : SemanticAnalyser<AglFormatterModel, SentenceContext<GrammarItem>> {
    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun analyse(asm: AglFormatterModel, locationMap: Map<Any, InputLocation>?, context: SentenceContext<GrammarItem>?, options:Map<String,Any>): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS))
    }
}