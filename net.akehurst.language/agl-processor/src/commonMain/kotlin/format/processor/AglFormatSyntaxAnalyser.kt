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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.simple.SyntaxAnalyserSimple
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.format.asm.AglFormatterModelFromAsm
import net.akehurst.language.formatter.api.AglFormatterModel
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.typemodel.api.TypeModel

internal class AglFormatSyntaxAnalyser(
    //grammarNamespaceQualifiedName: QualifiedName,
    typeModel: TypeModel,
    asmTransformModel: TransformModel,
    relevantTrRuleSet: QualifiedName
) : SyntaxAnalyser<AglFormatterModel> {

    private val _sa = SyntaxAnalyserSimple(typeModel, asmTransformModel, relevantTrRuleSet)

    override val locationMap: Map<Any, InputLocation> get() = _sa.locationMap

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()
    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<AglFormatterModel>> = emptyMap()

    override fun clear() {
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem?): SyntaxAnalysisResult<AglFormatterModel> {
        val res = _sa.transform(sppt, mapToGrammar)
        val asm = AglFormatterModelFromAsm(res.asm)
        return SyntaxAnalysisResultDefault(asm, res.issues, this.locationMap)
    }
}
