/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SpptDataNode

val SpptDataNode.isEmptyMatch get() = this.startPosition == this.nextInputPosition
fun SpptDataNode.locationIn(sentence: String) = InputFromString.locationFor(sentence, this.startPosition, this.nextInputPosition - this.startPosition)
fun SpptDataNode.matchedTextNoSkip(sentence: String) = sentence.substring(this.startPosition, this.nextInputNoSkip)

abstract class SyntaxAnalyserFromTreeDataAbstract<out AsmType : Any> : SyntaxAnalyser<AsmType> {

    override val locationMap = mutableMapOf<Any, InputLocation>()
    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    abstract val asm: AsmType

    override fun clear() {
        this.locationMap.clear()
        this.issues.clear()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<AsmType> {
        val sentence = (sppt as SPPTFromTreeData).originalSentence
        val treeData = (sppt as SPPTFromTreeData).treeData
        this.walkTree(sentence, treeData, false)
        this.embeddedSyntaxAnalyser.values.forEach {
            this.issues.addAll((it as SyntaxAnalyserFromTreeDataAbstract).issues)
        }
        return SyntaxAnalysisResultDefault(asm, issues, locationMap)
    }

    /**
     * implement this to walk the tree and set the 'asm' property
     */
    abstract fun walkTree(sentence: String, treeData: TreeDataComplete<out SpptDataNode>, skipDataAsTree: Boolean)
}