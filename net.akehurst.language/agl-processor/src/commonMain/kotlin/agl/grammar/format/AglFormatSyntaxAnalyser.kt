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
package net.akehurst.language.agl.grammar.format

import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimpleAbstract
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.typeModel.TypeModel

internal class AglFormatSyntaxAnalyser(
     typeModel: TypeModel?,
     scopeModel: ScopeModel?
) : SyntaxAnalyserSimpleAbstract<AglFormatterModelDefault, SentenceContext<GrammarItem>>(typeModel, scopeModel) {


    override fun clear() {
        //TODO("not implemented")
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        //TODO("not implemented")
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem, context: SentenceContext<GrammarItem>?): SyntaxAnalysisResult<AglFormatterModelDefault> {
        val res = super.transform(sppt, mapToGrammar, context)
        val asm = AglFormatterModelDefault(res.asm)
        return SyntaxAnalysisResultDefault(asm, res.issues, super.locationMap)
    }
}
