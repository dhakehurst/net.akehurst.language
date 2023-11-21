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
package net.akehurst.language.agl.language.format

import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.language.grammar.RuleItem
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.typemodel.api.TypeModel

internal class AglFormatSyntaxAnalyser(
    grammarNamespaceQualifiedName: String,
    val typeModel: TypeModel,
    val scopeModel: CrossReferenceModel
) : SyntaxAnalyser<AglFormatterModel> {

    private val _sa = SyntaxAnalyserDefault(grammarNamespaceQualifiedName, typeModel, scopeModel)

    override val locationMap: Map<Any, InputLocation> get() = _sa.locationMap

    override val extendsSyntaxAnalyser: Map<String, SyntaxAnalyser<*>> = emptyMap()
    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<AglFormatterModel>> = emptyMap()

    override fun clear() {
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem?): SyntaxAnalysisResult<AglFormatterModel> {
        val res = _sa.transform(sppt, mapToGrammar)
        val asm = AglFormatterModelFromAsm(res.asm)
        return SyntaxAnalysisResultDefault(asm, res.issues, this.locationMap)
    }
}
