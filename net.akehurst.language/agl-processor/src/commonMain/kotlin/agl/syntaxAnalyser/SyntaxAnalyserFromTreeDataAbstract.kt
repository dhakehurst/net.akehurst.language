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

import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.*
import net.akehurst.language.sppt.treedata.SPPTFromTreeData
import net.akehurst.language.sppt.treedata.locationForNode

class LocationMapDefault() : LocationMap {
    private val _map = mutableMapOf<Any, MutableMap<ParsePath, InputLocation>>()
    override fun clear() {
        _map.clear()
    }

    override fun get(obj: Any?): InputLocation? {
        var ppm = _map[obj]
       return when {
            null == ppm -> null
            ppm.isEmpty() -> null
            else -> ppm.values.firstOrNull()
        }
    }

    override fun getByPath(obj: Any?, path: ParsePath): InputLocation? {
        var ppm = _map[obj]
        return when {
            null == ppm -> null
            ppm.isEmpty() -> null
            else -> ppm[path]
        }
    }

    override fun add(path: ParsePath, obj: Any, location: InputLocation) {
        var ppm = _map[obj]
        if (null == ppm) {
            ppm = mutableMapOf()
            _map[obj] = ppm
        }
        ppm[path] = location
    }
}

abstract class SyntaxAnalyserFromTreeDataAbstract<AsmType : Any> : SyntaxAnalyser<AsmType> {

    override val locationMap = LocationMapDefault()
    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    abstract val asm: AsmType

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()
    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mutableMapOf()

    fun setLocationFor(obj: Any, nodeInfo: SpptDataNodeInfo, sentence: Sentence) {
        locationMap.add(nodeInfo.path, obj, sentence.locationForNode(nodeInfo.node))
    }

    fun setEmbeddedSyntaxAnalyser(qualifiedName: QualifiedName, sa: SyntaxAnalyser<AsmType>) {
        (embeddedSyntaxAnalyser as MutableMap).set(qualifiedName, sa)
    }

    override fun <T : Any> clear(done: Set<SyntaxAnalyser<T>>) {
        when {
            done.contains(this as SyntaxAnalyser<T>) -> Unit
            else -> {
                this.locationMap.clear()
                this.issues.clear()
                val newDone = done + this
                extendsSyntaxAnalyser.values.forEach { it.clear(newDone) }
                embeddedSyntaxAnalyser.values.forEach { it.clear(newDone) }
            }
        }
    }

    override fun transform(sppt: SharedPackedParseTree): SyntaxAnalysisResult<AsmType> { //TODO: return ObjectGraph
        val sentence = (sppt as SPPTFromTreeData).sentence
        val treeData = (sppt as SPPTFromTreeData).treeData
        this.walkTree(sentence, treeData, false)
        this.embeddedSyntaxAnalyser.values.forEach {
            this.issues.addAllFrom((it as SyntaxAnalyserFromTreeDataAbstract).issues)
        }
        return SyntaxAnalysisResultDefault(asm, issues, locationMap)
    }

    /**
     * implement this to walk the tree and set the 'asm' property
     */
    abstract fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean)

    /**
     * convenience function for use from typescript
     */
    fun locationForNode(sentence: Sentence, node: SpptDataNode): InputLocation {
        return sentence.locationForNode(node)
    }
}