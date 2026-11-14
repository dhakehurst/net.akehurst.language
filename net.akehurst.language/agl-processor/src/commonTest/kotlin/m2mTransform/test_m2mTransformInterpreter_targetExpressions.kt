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

package net.akehurst.language.m2mTransform.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.PropertyCharacteristic.*
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Expressions on the target side of a mapping ('domain tgt n:Node := <expression>') that use
 * objects and types of a source domain. The source and target domains have their own accessor-mutator (by reflection).
 * Runs both modes of the combined interpreter.
 */
class test_m2mTransformInterpreter_targetExpressions {

    private companion object {
        // source domain
        data class Link(val role: String, val targetId: String)
        data class Page(val name: String, val labels: List<String>, val links: List<Link>)
        data class Folder(val name: String)

        // target domain (a generic knowledge graph)
        data class Relation(val type: String, val targetKey: String)
        data class Node(val label: String, val relationships: List<Any>)

        val srcTypes = typesDomain("Source", true) {
            namespace("mock.src") {
                data("Page") {
                    propertyOf(setOf(VAL, CMP, STR), "name", "String")
                    propertyOf(setOf(VAL, CMP, STR), "labels", "List") { typeArgument("String") }
                    propertyOf(setOf(VAL, CMP, STR), "links", "List") { typeArgument("Link") }
                }
                data("Folder") {
                    propertyOf(setOf(VAL, CMP, STR), "name", "String")
                }
                data("Link") {
                    propertyOf(setOf(VAL, CMP, STR), "role", "String")
                    propertyOf(setOf(VAL, CMP, STR), "targetId", "String")
                }
            }
        }

        val kgTypes = typesDomain("Graph", true) {
            namespace("mock.kg") {
                data("Node") {
                    constructor_ {
                        parameter(setOf(), "label", "String")
                        parameter(setOf(), "relationships", "List") // no type arguments, as List made in expressions are List<Any>
                        execution { args -> Node(args[0] as String, (args[1] as List<Any>).map { (it as? TypedObject)?.self ?: it }) }
                    }
                }
                data("Relation") {
                    constructor_ {
                        parameter(setOf(), "type", "String")
                        parameter(setOf(), "targetKey", "String")
                        execution { args -> Relation(args[0] as String, args[1] as String) }
                    }
                }
            }
        }

        val typeDomains = mapOf(DomainReference("src") to srcTypes, DomainReference("kg") to kgTypes)

        fun compile(transform: String) = run {
            val context = SentenceContextAny()
            typeDomains.forEach { (_, v) -> context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v) }
            val res = Agl.registry.agl.m2mTransform.processor!!.process(transform, options = Agl.options { semanticAnalysis { sentenceContext(context) } })
            check(res.allIssues.errors.isEmpty()) { res.allIssues.toString() }
            res.asm!!
        }

        fun transformFor(nodeExpression: String) = $$"""
            namespace test
            transform T(src:Source, kg:Graph) {
                top mapping P2N {
                    domain src p:Page {}
                    domain kg n:Node := $$nodeExpression
                }
            }
        """

        val page = Page("p1", listOf("a", "b"), listOf(Link("parent", "x"), Link("child", "y")))
    }

    private enum class Mode { SYNC, SUSPEND }

    private class Run(val result: M2MTransformResult, val issues: List<IssueHolder>, val kg: ObjectGraphAccessorMutator) {
        val nodes get() = result.targets.map { kg.untyped(it) as Node }
        val allIssues get() = issues.flatMap { it.all } + result.issues.all
    }

    private suspend fun run(mode: Mode, nodeExpression: String, sourceObject: Any = page): Run {
        val m2m = compile(transformFor(nodeExpression))
        val srcIssues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val kgIssues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val srcOg = ObjectGraphAccessorMutatorByReflection(srcTypes, srcIssues, LocationMapDefault())
        val kgOg = ObjectGraphAccessorMutatorByReflection(kgTypes, kgIssues, LocationMapDefault())
        val ogs = mapOf<SimpleName, ObjectGraphAccessorMutator>(srcTypes.name to srcOg, kgTypes.name to kgOg)
        val interp = M2mTransformInterpreterCombined(m2m, ogs, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val source = mapOf(DomainReference("src") to listOf(srcOg.toTypedObject(sourceObject, srcTypes.findByQualifiedNameOrNull(QualifiedName("mock.src.Page"))!!.type())))
        val tgt = m2m.allTransformRuleSet.first()
        val res = when (mode) {
            Mode.SYNC -> interp.transform(tgt, DomainReference("kg"), emptyMap(), source)
            Mode.SUSPEND -> interp.transformSuspend(tgt, DomainReference("kg"), emptyMap(), source)
        }
        return Run(res, listOf(srcIssues, kgIssues), kgOg)
    }

    private fun assertNoIssues(r: Run) = assertTrue(r.allIssues.isEmpty(), r.allIssues.joinToString("\n"))

    // --- 1. type references to types of a source domain ---

    private suspend fun isSourceType(mode: Mode) {
        val r = run(mode, "Node(label := when { p is Page -> 'page' else -> 'other' }, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("page", emptyList())), r.nodes)
    }

    private suspend fun isOtherSourceType(mode: Mode) {
        val r = run(mode, "Node(label := when { p is Folder -> 'folder' else -> 'other' }, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("other", emptyList())), r.nodes)
    }

    private suspend fun isQualifiedSourceType(mode: Mode) {
        val r = run(mode, "Node(label := when { p is mock.src.Page -> 'page' else -> 'other' }, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("page", emptyList())), r.nodes)
    }

    private suspend fun asSourceType(mode: Mode) {
        val r = run(mode, "Node(label := (p as Page).name, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("p1", emptyList())), r.nodes)
    }

    // --- 2. lists made in the expression from source objects ---

    private suspend fun mapSourceListToTargetObjects(mode: Mode) {
        val r = run(mode, "Node(label := p.name, relationships := p.links.map({ l -> Relation(type := l.role, targetKey := l.targetId) {} })) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("p1", listOf(Relation("parent", "x"), Relation("child", "y")))), r.nodes)
    }

    private suspend fun filterSourceList(mode: Mode) {
        val r = run(mode, "Node(label := p.name, relationships := p.links.filter({ l -> l.role == 'child' }).map({ l -> Relation(type := l.role, targetKey := l.targetId) {} })) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("p1", listOf(Relation("child", "y")))), r.nodes)
    }

    private suspend fun mapSourceListToStrings(mode: Mode) {
        val r = run(mode, "Node(label := p.links.map({ l -> l.targetId }).join, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("xy", emptyList())), r.nodes)
    }

    // --- 3. list concatenation and flatMap ---

    private suspend fun concatenateLists(mode: Mode) {
        val r = run(mode, "Node(label := p.name, relationships := List(Relation(type := 'fixed', targetKey := 'k') {}) + p.labels.map({ l -> Relation(type := 'L', targetKey := l) {} })) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("p1", listOf(Relation("fixed", "k"), Relation("L", "a"), Relation("L", "b")))), r.nodes)
    }

    private suspend fun flatMap(mode: Mode) {
        val r = run(mode, "Node(label := p.name, relationships := p.links.flatMap({ l -> List(Relation(type := l.role, targetKey := l.targetId) {}, Relation(type := 'back', targetKey := p.name) {}) })) {}")
        assertNoIssues(r)
        assertEquals(
            listOf(Node("p1", listOf(Relation("parent", "x"), Relation("back", "p1"), Relation("child", "y"), Relation("back", "p1")))),
            r.nodes
        )
    }

    private suspend fun flatMapOfListOfLists(mode: Mode) {
        val r = run(mode, "Node(label := List(List('a', 'b'), List('c')).flatMap({ l -> l }).join, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("abc", emptyList())), r.nodes)
    }

    private suspend fun plusMethod(mode: Mode) {
        val r = run(mode, "Node(label := p.labels.plus(List('c')).join, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("abc", emptyList())), r.nodes)
    }

    private suspend fun concatenateKeepsDuplicates(mode: Mode) {
        val r = run(mode, "Node(label := (p.labels + List('a')).join, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("aba", emptyList())), r.nodes)
    }

    private suspend fun unionMethod(mode: Mode) {
        val r = run(mode, "Node(label := 'n' + p.labels.union(List('b', 'c')).size, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("n3", emptyList())), r.nodes)
    }

    private suspend fun flatten(mode: Mode) {
        val r = run(mode, "Node(label := List(p.labels, List('c')).flatten.join, relationships := List()) {}")
        assertNoIssues(r)
        assertEquals(listOf(Node("abc", emptyList())), r.nodes)
    }

    @Test
    fun plusMethod_combined() = runTest { plusMethod(Mode.SYNC) }

    @Test
    fun plusMethod_combinedSuspend() = runTest { plusMethod(Mode.SUSPEND) }

    @Test
    fun concatenateKeepsDuplicates_combined() = runTest { concatenateKeepsDuplicates(Mode.SYNC) }

    @Test
    fun concatenateKeepsDuplicates_combinedSuspend() = runTest { concatenateKeepsDuplicates(Mode.SUSPEND) }

    @Test
    fun unionMethod_combined() = runTest { unionMethod(Mode.SYNC) }

    @Test
    fun unionMethod_combinedSuspend() = runTest { unionMethod(Mode.SUSPEND) }

    @Test
    fun flatten_combined() = runTest { flatten(Mode.SYNC) }

    @Test
    fun flatten_combinedSuspend() = runTest { flatten(Mode.SUSPEND) }

    @Test
    fun isSourceType_combined() = runTest { isSourceType(Mode.SYNC) }

    @Test
    fun isSourceType_combinedSuspend() = runTest { isSourceType(Mode.SUSPEND) }

    @Test
    fun isOtherSourceType_combined() = runTest { isOtherSourceType(Mode.SYNC) }

    @Test
    fun isOtherSourceType_combinedSuspend() = runTest { isOtherSourceType(Mode.SUSPEND) }

    @Test
    fun isQualifiedSourceType_combined() = runTest { isQualifiedSourceType(Mode.SYNC) }

    @Test
    fun isQualifiedSourceType_combinedSuspend() = runTest { isQualifiedSourceType(Mode.SUSPEND) }

    @Test
    fun asSourceType_combined() = runTest { asSourceType(Mode.SYNC) }

    @Test
    fun asSourceType_combinedSuspend() = runTest { asSourceType(Mode.SUSPEND) }

    @Test
    fun mapSourceListToTargetObjects_combined() = runTest { mapSourceListToTargetObjects(Mode.SYNC) }

    @Test
    fun mapSourceListToTargetObjects_combinedSuspend() = runTest { mapSourceListToTargetObjects(Mode.SUSPEND) }

    @Test
    fun filterSourceList_combined() = runTest { filterSourceList(Mode.SYNC) }

    @Test
    fun filterSourceList_combinedSuspend() = runTest { filterSourceList(Mode.SUSPEND) }

    @Test
    fun mapSourceListToStrings_combined() = runTest { mapSourceListToStrings(Mode.SYNC) }

    @Test
    fun mapSourceListToStrings_combinedSuspend() = runTest { mapSourceListToStrings(Mode.SUSPEND) }

    @Test
    fun concatenateLists_combined() = runTest { concatenateLists(Mode.SYNC) }

    @Test
    fun concatenateLists_combinedSuspend() = runTest { concatenateLists(Mode.SUSPEND) }

    @Test
    fun flatMap_combined() = runTest { flatMap(Mode.SYNC) }

    @Test
    fun flatMap_combinedSuspend() = runTest { flatMap(Mode.SUSPEND) }

    @Test
    fun flatMapOfListOfLists_combined() = runTest { flatMapOfListOfLists(Mode.SYNC) }

    @Test
    fun flatMapOfListOfLists_combinedSuspend() = runTest { flatMapOfListOfLists(Mode.SUSPEND) }
}
