package net.akehurst.language.agl.agl.grammar.style

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleRule

class AglStyleSemanticAnalyser : SemanticAnalyser<AglStyleModel, SentenceContext<GrammarItem>> {
    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun analyse(asm: AglStyleModel, locationMap: Map<Any, InputLocation>?, context: SentenceContext<GrammarItem>?, options:Map<String,Any>): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(emptyList())
    }
}