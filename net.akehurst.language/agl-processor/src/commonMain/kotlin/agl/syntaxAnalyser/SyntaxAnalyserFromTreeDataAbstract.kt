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

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.api.sppt.TreeData
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

val SpptDataNode.isEmptyMatch get() = this.startPosition == this.nextInputPosition

abstract class SyntaxAnalyserFromTreeDataAbstract<out AsmType : Any> : SyntaxAnalyser<AsmType> {

    override val locationMap = mutableMapOf<Any, InputLocation>()
    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    abstract val asm: AsmType

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()
    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()

    override fun clear() {
        this.locationMap.clear()
        this.issues.clear()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem?): SyntaxAnalysisResult<AsmType> {
        val sentence = (sppt as SPPTFromTreeData).sentence
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
    abstract fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean)
}