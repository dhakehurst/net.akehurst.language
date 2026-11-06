/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.asm.simple.AsmAnySimple
import net.akehurst.language.asm.simple.AsmStructureParseIdentity
import net.akehurst.language.asm.simple.AsmStructureSimple
import net.akehurst.language.asm.simple.asValueName
import net.akehurst.language.asmTransform.processor.AsmTransformInterpreter
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.asmTransform.asm.asmTransformationRule
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.parser.api.ParseStreamListener
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sentence.common.SentenceAbstract
import net.akehurst.language.sppt.api.AltInfo
import net.akehurst.language.sppt.api.ChildInfo
import net.akehurst.language.sppt.api.ParsePath
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.types.asm.StdLibDefault

/**
 * Builds the ASM while a streaming parse runs, using the same transform rules as
 * [SyntaxAnalyserFromAsmTransformAbstract.walkTree], so no parse tree is needed.
 *
 * Use it as the parse option `streamListener`. Call `clear()` on the syntax analyser first, as before `transform`.
 * When the parse is done, [result] holds the syntax analysis result (its asm is null if the parse failed).
 *
 * The parse path of a node needs all the nodes above it, which are not known while streaming (nodes arrive bottom-up).
 * So parse paths, structure identities and locations are set when the parse finishes.
 * While streaming, `$path` in a transform rule is not the parse path, and the identity of a structure
 * must not be used before the parse finishes.
 */
class SyntaxAnalysisStream<AsmType : Any, AsmValueType : Any>(
    private val syntaxAnalyser: SyntaxAnalyserFromAsmTransformAbstract<AsmType, AsmValueType>
) : ParseStreamListener {

    companion object {
        /** the parse path given to transform rules while streaming */
        val PATH_NOT_KNOWN = ParsePath(listOf("<path not known while streaming>"))

        private val PATH_PROPERTY = AsmTransformInterpreter.PATH.asValueName

        private val pseudoTrRule: AsmTransformationRule = asmTransformationRule(StdLibDefault.AnyType, RootExpressionDefault.SELF)
    }

    /** one per sent node; linked to the parent when the parent arrives, so the parse path can be made at the end */
    private class PathCell(val tag: String) {
        var parent: PathCell? = null
        var listIndex = -1
    }

    private class NodeInfo(
        override val node: SpptDataNode,
        numChildren: Int
    ) : SpptDataNodeInfo {
        override val path: ParsePath get() = PATH_NOT_KNOWN
        override val parentAlt = AltInfo(RulePosition.OPTION_NONE, 0, 1)
        override val alt = AltInfo(node.option, 0, 1)
        override val child = ChildInfo(0, 0, 1)
        override val numChildrenAlternatives = mapOf(node.option to numChildren)
        override val totalChildrenFromAllAlternatives = numChildren
        override val numSkipChildren = 0
    }

    private class Item(val nodeInfo: SpptDataNodeInfo, val value: TypedObject?, val cell: PathCell)

    private class Located(val obj: Any, val cell: PathCell, val node: SpptDataNode)

    var result: SyntaxAnalysisResult<AsmType>? = null; private set

    private lateinit var sentence: Sentence
    private val saStack = ArrayList<SyntaxAnalyserFromAsmTransformAbstract<AsmType, AsmValueType>>()
    private val used = LinkedHashSet<SyntaxAnalyserFromAsmTransformAbstract<*, *>>()
    private val stack = ArrayList<Item>()

    // in the order they were made (bottom-up), as needed to set parsePath and identity the same way as walkTree
    private val structures = ArrayList<Pair<AsmStructureSimple, PathCell>>()
    private val locations = ArrayList<Located>()

    private val sa get() = saStack[saStack.lastIndex]

    override fun start(sentence: Sentence) {
        this.sentence = sentence
        result = null
        saStack.clear()
        stack.clear()
        structures.clear()
        locations.clear()
        saStack.add(syntaxAnalyser)
        use(syntaxAnalyser)
        syntaxAnalyser.setAsm(syntaxAnalyser.asmOrNull ?: syntaxAnalyser.constructAsm())
    }

    private fun use(s: SyntaxAnalyserFromAsmTransformAbstract<*, *>) {
        s.deferParseIdentity = true
        used.add(s)
    }

    override fun leaf(node: SpptDataNode) {
        val info = NodeInfo(node, 0)
        val value = sa.createValueFromLeaf(sentence, info)
        stack.add(Item(info, value, PathCell(node.rule.tag)))
    }

    override fun skip(startPosition: Int, nextInputPosition: Int) {
        // not used in the asm
    }

    override fun branch(node: SpptDataNode, numChildren: Int) {
        val children = pop(numChildren)
        val cell = PathCell(node.rule.tag)
        val isList = node.rule.isList
        for (i in children.indices) {
            val c = children[i].cell
            c.parent = cell
            if (isList) c.listIndex = i
        }
        val info = NodeInfo(node, numChildren)
        val s = sa
        val trRule = trRuleFor(s, node)
        val downData = s.resolveCompressed(PATH_NOT_KNOWN, trRule, info)
        val value = when {
            //NothingType -> branch not used for element property value, push null for correct num children on stack
            s.typesDomain.NothingType == downData.trRule.forNode.resolvedType.resolvedDefinition -> s.objectGraph.nothing()
            else -> s.createValueFromBranch(sentence, downData, info, children.map { ChildData(it.nodeInfo, it.value) })
        }
        if (null != value) {
            val self = value.self
            if (self is AsmStructureSimple) structures.add(Pair(self, cell))
            locations.add(Located(self, cell, node))
        }
        stack.add(Item(info, value, cell))
    }

    /** as SyntaxAnalyserFromAsmTransformAbstract.trRuleForNode, which only needs the parent rule to be there */
    private fun trRuleFor(s: SyntaxAnalyserFromAsmTransformAbstract<*, *>, node: SpptDataNode): AsmTransformationRule = when {
        node.rule.isPseudo -> pseudoTrRule
        else -> s.findTrRuleForGrammarRuleNamedOrNull(node.rule.tag) ?: error("Transform Rule for '${node.rule.tag}' not found")
    }

    override fun beginEmbedded(node: SpptDataNode) {
        val embeddedRhs = (node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
        val embGrmName = embeddedRhs.embeddedRuntimeRuleSet.qualifiedName
        @Suppress("UNCHECKED_CAST")
        val embSyntaxAnalyser = syntaxAnalyser.embeddedSyntaxAnalyser[QualifiedName(embGrmName)] as SyntaxAnalyserFromAsmTransformAbstract<AsmType, AsmValueType>?
            ?: error("Embedded SyntaxAnalyser not found for '$embGrmName' in SyntaxAnalyser using TrRuleSet '${syntaxAnalyser.relevantTrRuleSet}'")
        // the same asm is used for embedded trees (as in walkTree)
        embSyntaxAnalyser.setAsm(syntaxAnalyser.asmOrNull ?: syntaxAnalyser.constructAsm())
        saStack.add(embSyntaxAnalyser)
        use(embSyntaxAnalyser)
    }

    override fun endEmbedded(node: SpptDataNode) {
        // the embedded root; walkTree adds it as an asm root and removes it again, so it is just used as the value here
        val root = pop(1)[0]
        saStack.removeAt(saStack.lastIndex)
        val value = root.value?.self
        val cell = PathCell(node.rule.tag)
        // the embedded tree has its own paths, starting at its root, so root.cell is not linked to cell
        if (null != value) locations.add(Located(value, cell, node))
        val typed = value?.let { syntaxAnalyser.objectGraph.toTypedObject(it, StdLibDefault.AnyType) }
        stack.add(Item(NodeInfo(node, 1), typed, cell))
    }

    override fun finished(issues: IssueCollection<LanguageIssue>) {
        val root = pop(1)[0]
        @Suppress("UNCHECKED_CAST")
        syntaxAnalyser.addAsmRoot(syntaxAnalyser.asm, root.value!!.self as AsmValueType)
        setPathsAndLocations()
        done()
        result = SyntaxAnalysisResultDefault(syntaxAnalyser.asm, syntaxAnalyser.issues, syntaxAnalyser.locationMap)
    }

    override fun failed(issues: IssueCollection<LanguageIssue>, committedUpTo: Int) {
        done()
        result = SyntaxAnalysisResultDefault(null, syntaxAnalyser.issues, syntaxAnalyser.locationMap)
    }

    private fun done() {
        used.forEach { it.deferParseIdentity = false }
        used.forEach { if (it !== syntaxAnalyser) syntaxAnalyser.issues.addAllFrom(it.issues) }
        used.clear()
        stack.clear()
        structures.clear()
        locations.clear()
    }

    private fun pop(n: Int): List<Item> {
        val from = stack.size - n
        val out = ArrayList(stack.subList(from, stack.size))
        for (i in stack.lastIndex downTo from) stack.removeAt(i)
        return out
    }

    private fun setPathsAndLocations() {
        val paths = HashMap<PathCell, ParsePath>()
        fun pathOf(cell: PathCell): ParsePath {
            paths[cell]?.let { return it }
            // find the nearest ancestor with a known path (or the root), then come back down
            val chain = ArrayList<PathCell>()
            var c: PathCell? = cell
            var base = ParsePath(emptyList())
            while (null != c) {
                val known = paths[c]
                if (null != known) {
                    base = known
                    break
                }
                chain.add(c)
                c = c.parent
            }
            var p = base
            for (i in chain.lastIndex downTo 0) {
                val ch = chain[i]
                // the root of a tree has the empty path
                if (null != ch.parent) {
                    if (ch.listIndex >= 0) p = p + ch.listIndex.toString()
                    p = p + ch.tag
                }
                paths[ch] = p
            }
            return p
        }

        val sentenceIdentity = (sentence as? SentenceAbstract)?.identity
        for ((self, cell) in structures) {
            // as in createValueFromBranch: the outermost node sets the parsePath, the innermost one the identity
            val path = pathOf(cell)
            val pp = path.toString()
            self.parsePath = pp
            // a parse-node tuple (e.g. from \$self) holds its own path; the innermost node made it, so the first record sets it
            val pathProp = self.property[PATH_PROPERTY]
            if (null != pathProp) {
                val v = pathProp.value
                when {
                    v === PATH_NOT_KNOWN -> self.setProperty(PATH_PROPERTY, path, pathProp.index)
                    v is AsmAnySimple && v.value === PATH_NOT_KNOWN -> self.setProperty(PATH_PROPERTY, AsmAnySimple(path), pathProp.index)
                    v is TypedObject && v.self === PATH_NOT_KNOWN -> self.setProperty(PATH_PROPERTY, syntaxAnalyser.objectGraph.any(path), pathProp.index)
                }
            }
            if (self.hasIdentity.not()) self.setIdentity(AsmStructureParseIdentity(sentenceIdentity, self.qualifiedTypeName, pp))
        }
        for (l in locations) {
            syntaxAnalyser.locationMap.add(pathOf(l.cell), l.obj, sentence.locationForNode(l.node))
        }
    }
}
