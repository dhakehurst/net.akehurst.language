/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.sppt.treedata

import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.SpptWalker
import net.akehurst.language.sppt.api.TreeData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Behavioural tests for [TreeDataGrowing], written against the current
 * implementation as a safety net before refactoring the per-parent
 * `_growingChildren` list to avoid the per-mutation full-list copy.
 *
 * Key invariants exercised here (and that any replacement structure MUST
 * preserve):
 *  - the growing-children sequence for a parent is observable in order
 *    via the public [TreeDataGrowing.growingChildren] map;
 *  - mutating the new parent's children via the `setNext*` operations
 *    must NOT mutate the old parent's children (structural sharing is
 *    fine, aliasing is not);
 *  - [TreeDataGrowing.setFirstChildForGrowing] replaces any existing
 *    children for a parent (resets to a single-element sequence);
 *  - [TreeDataGrowing.setNextChildForCompleteParent] forwards the
 *    materialised child-list to `complete.setChildren` and does not
 *    remove the growing entry;
 *  - [TreeDataGrowing.setNextChildForCompleteParent] currently raises a
 *    `NotImplementedError` (TODO) when the old growing parent is absent
 *    -- locked in so a refactor doesn't accidentally change it.
 */
class test_TreeDataGrowing {

    // ----- minimal fakes -------------------------------------------------

    /** Skeleton [Rule]; only `tag` is used by [CompleteTreeDataNode.toString]. */
    private class FakeRule(override val tag: String) : Rule {
        override val ruleSetNumber: Int = 1
        override val number: Int = 1
        override val isSkip: Boolean = false
        override val isGoal: Boolean = false
        override val isPseudo: Boolean = false
        override val isTerminal: Boolean = false
        override val isEndOfText: Boolean = false
        override val isEmptyTerminal: Boolean = false
        override val isEmptyListTerminal: Boolean = false
        override val isLiteral: Boolean = false
        override val isPattern: Boolean = false
        override val isEmbedded: Boolean = false
        override val isChoice: Boolean = false
        override val isChoiceAmbiguous: Boolean = false
        override val isList: Boolean = false
        override val isListSimple: Boolean = false
        override val isListSeparated: Boolean = false
        override val isOptional: Boolean = false
        override val isListOptional: Boolean = false
        override val rhsItems: List<List<Rule>> = emptyList()
        override val unescapedTerminalValue: String get() = error("not used")
        override fun toString(): String = tag
        // value equality on `tag` so two `cn("S", …)` calls produce nodes
        // that compare equal (CompleteTreeDataNode.equals compares `rule`).
        override fun equals(other: Any?): Boolean = other is FakeRule && other.tag == tag
        override fun hashCode(): Int = tag.hashCode()
    }

    private fun cn(tag: String, sp: Int = 0, np: Int = 0, opt: OptionNum = RulePosition.OPTION_NONE): SpptDataNode =
        CompleteTreeDataNode(FakeRule(tag), sp, np, np, opt, emptyList())

    /** Minimal [TreeData] stand-in usable as `initialSkipData` / embedded tree. */
    private class StubTreeData : TreeData {
        override val root: SpptDataNode? = null
        override val userRoot: SpptDataNode get() = error("not used")
        override val initialSkip: TreeData? = null
        override val isEmpty: Boolean = true
        override fun skipDataAfter(node: SpptDataNode): TreeData? = null
        override fun childrenFor(node: SpptDataNode): List<Pair<OptionNum, List<SpptDataNode>>> = emptyList()
        override fun embeddedFor(node: SpptDataNode): TreeData? = null
        override fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean) {}
        override fun preferred(node: SpptDataNode): SpptDataNode? = null
        override fun skipNodesAfter(node: SpptDataNode): List<SpptDataNode> = emptyList()
        override fun matches(other: TreeData): Boolean = this === other
        override fun start(initialSkipData: TreeData?) {}
        override fun setRootTo(root: SpptDataNode) {}
        override fun setUserGoalChildrenAfterInitialSkip(nug: SpptDataNode, userGoalChildren: List<SpptDataNode>) {}
        override fun setChildren(parent: SpptDataNode, completeChildren: List<SpptDataNode>, isAlternative: Boolean) {}
        override fun setSkipDataAfter(leafNodeIndex: SpptDataNode, skipData: TreeData) {}
        override fun setEmbeddedTreeFor(n: SpptDataNode, treeData: TreeData) {}
        override fun remove(node: SpptDataNode) {}
    }

    /** Read every child stored under [parent] via the public map. */
    private fun TreeDataGrowing<String, SpptDataNode>.growingFor(parent: String): List<SpptDataNode> =
        this.growingChildren[parent] ?: emptyList()

    // ----- construction --------------------------------------------------

    @Test
    fun construct_isClean_isEmpty() {
        val sut = TreeDataGrowing<String, SpptDataNode>(7)

        assertTrue(sut.isClean)
        assertTrue(sut.isEmpty)
        assertTrue(sut.growingChildren.isEmpty())
        assertEquals(7, sut.hashCode())
        assertEquals("TreeData{7}", sut.toString())
    }

    @Test
    fun equals_and_hashCode_are_by_stateSetNumber() {
        val a = TreeDataGrowing<String, SpptDataNode>(3)
        val b = TreeDataGrowing<String, SpptDataNode>(3)
        val c = TreeDataGrowing<String, SpptDataNode>(4)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assertFalse(a.equals("not a TreeDataGrowing"))
    }

    // ----- initialise ----------------------------------------------------

    @Test
    fun initialise_creates_empty_growing_entry_for_parent() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        sut.initialise("P", null)

        assertFalse(sut.isClean, "growing entry for P means not clean")
        assertTrue(sut.growingChildren.containsKey("P"))
        assertEquals(emptyList(), sut.growingChildren["P"])
        assertNull(sut.complete.initialSkip)
    }

    @Test
    fun initialise_with_initialSkip_is_forwarded_to_complete() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val skip = StubTreeData()
        sut.initialise("P", skip)

        assertSame(skip, sut.complete.initialSkip)
    }

    // ----- setFirstChildForGrowing --------------------------------------

    @Test
    fun setFirstChildForGrowing_when_no_existing_entry_creates_singleton_sequence() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a")

        sut.setFirstChildForGrowing("P", a)

        assertEquals(listOf(a), sut.growingFor("P"))
        assertEquals(listOf(a), sut.growingChildren["P"])
    }

    @Test
    fun setFirstChildForGrowing_called_twice_replaces_first_child() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        sut.initialise("P", null)
        val a = cn("a")
        val b = cn("b", sp = 1, np = 1)

        sut.setFirstChildForGrowing("P", a)
        sut.setFirstChildForGrowing("P", b)

        assertEquals(listOf(b), sut.growingFor("P"))
    }

    // ----- setNextChildForGrowingParent ---------------------------------

    @Test
    fun setNextChildForGrowingParent_appends_under_new_parent() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForGrowingParent("P0", "P1", b)

        assertEquals(listOf(a, b), sut.growingFor("P1"))
    }

    @Test
    fun setNextChildForGrowingParent_does_not_mutate_old_parent_sequence() {
        // THE invariant the current defensive copy is protecting.
        // Any replacement structure must preserve it.
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)
        val c = cn("c", 1, 2)

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForGrowingParent("P0", "P1", b)
        // Now make a *different* extension of P0; P1 must not change.
        sut.setNextChildForGrowingParent("P0", "P1bis", c)

        assertEquals(listOf(a), sut.growingFor("P0"), "old parent unchanged")
        assertEquals(listOf(a, b), sut.growingFor("P1"), "first extension unchanged")
        assertEquals(listOf(a, c), sut.growingFor("P1bis"), "second extension independent")
    }

    @Test
    fun setNextChildForGrowingParent_chained_builds_full_sequence_and_keeps_prefixes() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)
        val c = cn("c", 2, 3)

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForGrowingParent("P0", "P1", b)
        sut.setNextChildForGrowingParent("P1", "P2", c)

        assertEquals(listOf(a), sut.growingFor("P0"))
        assertEquals(listOf(a, b), sut.growingFor("P1"))
        assertEquals(listOf(a, b, c), sut.growingFor("P2"))
    }

    // ----- setNextChildForCompleteParent --------------------------------

    @Test
    fun setNextChildForCompleteParent_stores_full_sequence_in_complete() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)
        val parentComplete = cn("S", 0, 2)

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForCompleteParent("P0", parentComplete, b, isAlternative = false)

        val children = sut.complete.childrenFor(parentComplete)
        assertEquals(1, children.size)
        assertEquals(listOf(a, b), children[0].second)
    }

    @Test
    fun setNextChildForCompleteParent_does_not_remove_growing_entry() {
        // Current implementation leaves the old growing entry in place
        // (the `_growingChildren.remove(oldParent)` line is commented out).
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)
        val parentComplete = cn("S", 0, 2)

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForCompleteParent("P0", parentComplete, b, isAlternative = false)

        assertEquals(listOf(a), sut.growingFor("P0"))
    }

    @Test
    fun setNextChildForCompleteParent_when_oldParent_missing_throws_TODO() {
        // Locked-in behaviour: the TODO branch is currently reachable only
        // for ambiguous grammars. We pin it down so a future change is
        // explicit.
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val parentComplete = cn("S", 0, 1)

        assertFailsWith<NotImplementedError> {
            sut.setNextChildForCompleteParent("missing", parentComplete, cn("x"), false)
        }
    }

    @Test
    fun setNextChildForCompleteParent_isAlternative_false_marks_preferred() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val a = cn("a", 0, 1)
        val b = cn("b", 1, 2)
        val parent1 = cn("S", 0, 2, OptionNum(0))
        val parent2 = cn("S", 0, 2, OptionNum(1))

        sut.setFirstChildForGrowing("P0", a)
        sut.setNextChildForCompleteParent("P0", parent1, b, isAlternative = true)

        sut.setFirstChildForGrowing("P0b", a)
        sut.setNextChildForCompleteParent("P0b", parent2, b, isAlternative = false)

        // SpptDataNode equality ignores `option`; either node serves as a key.
        val pref = sut.preferred(parent1)
        assertNotNull(pref)
        assertEquals(parent2.option, pref.option)
    }

    // ----- setFirstChildForComplete -------------------------------------

    @Test
    fun setFirstChildForComplete_stores_singleton_in_complete() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val parent = cn("S", 0, 1)
        val a = cn("a", 0, 1)

        sut.setFirstChildForComplete(parent, a, isAlternative = false)

        val children = sut.complete.childrenFor(parent)
        assertEquals(1, children.size)
        assertEquals(listOf(a), children[0].second)
    }

    // ----- setEmbeddedChild ---------------------------------------------

    @Test
    fun setEmbeddedChild_records_child_and_embedded_tree() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val parent = cn("E", 0, 3)
        val child = cn("e", 0, 3)
        val embedded = StubTreeData()

        sut.setEmbeddedChild(parent, child, embedded)

        assertEquals(listOf(child), sut.complete.childrenFor(parent)[0].second)
        assertSame(embedded, sut.complete.embeddedFor(parent))
    }

    // ----- setSkipDataAfter ---------------------------------------------

    @Test
    fun setSkipDataAfter_forwards_to_complete() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        val leaf = cn("a", 0, 1)
        val skip = StubTreeData()

        sut.setSkipDataAfter(leaf, skip)

        assertSame(skip, sut.complete.skipDataAfter(leaf))
    }

    // ----- removeTreeGrowing --------------------------------------------

    @Test
    fun removeTreeGrowing_removes_entry() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        sut.setFirstChildForGrowing("P", cn("a"))
        assertFalse(sut.isClean)

        sut.removeTreeGrowing("P")

        assertTrue(sut.isClean)
        assertFalse(sut.growingChildren.containsKey("P"))
    }

    @Test
    fun removeTreeGrowing_for_unknown_parent_is_a_noop() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        sut.removeTreeGrowing("nope") // must not throw
        assertTrue(sut.isClean)
    }

    // ----- isClean / isEmpty interplay ----------------------------------

    @Test
    fun isEmpty_requires_complete_empty_and_no_growing() {
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        assertTrue(sut.isEmpty)

        sut.initialise("P", null)
        assertFalse(sut.isEmpty, "growing entry breaks isEmpty")

        sut.removeTreeGrowing("P")
        assertTrue(sut.isEmpty)

        // populate complete only
        val parent = cn("S", 0, 1)
        sut.setFirstChildForComplete(parent, cn("a", 0, 1), isAlternative = false)
        assertFalse(sut.isEmpty, "complete content breaks isEmpty")
        assertTrue(sut.isClean, "but isClean only looks at growing")
    }

    // =============================================================
    //  ChildChain (snoc-list) -- internal structure backing growing
    //  children. Tested directly so any future change keeps the same
    //  contract.
    // =============================================================

    @Test
    fun childChain_of_creates_singleton() {
        val c = ChildChain.of("a")
        assertEquals(1, c.size)
        assertEquals("a", c.head)
        assertNull(c.prev)
        assertEquals(listOf("a"), c.toList())
    }

    @Test
    fun childChain_append_returns_new_chain_with_size_plus_one() {
        val c1 = ChildChain.of("a")
        val c2 = c1.append("b")

        assertEquals(1, c1.size, "original is unchanged")
        assertEquals(2, c2.size)
        assertEquals("b", c2.head, "head holds the most-recent element")
        assertSame(c1, c2.prev, "prev is the original chain (structural sharing)")
    }

    @Test
    fun childChain_append_does_not_mutate_receiver() {
        val c1 = ChildChain.of("a")
        val c2 = c1.append("b")
        val c3 = c1.append("c") // diverge from c1

        assertEquals(listOf("a"), c1.toList())
        assertEquals(listOf("a", "b"), c2.toList())
        assertEquals(listOf("a", "c"), c3.toList())
    }

    @Test
    fun childChain_toList_preserves_insertion_order() {
        val c = ChildChain.of("a").append("b").append("c").append("d")
        assertEquals(4, c.size)
        assertEquals(listOf("a", "b", "c", "d"), c.toList())
    }

    @Test
    fun childChain_long_chain_round_trips() {
        // Guard against off-by-one in the reverse-fill loop.
        val n = 1000
        var c = ChildChain.of(0)
        for (i in 1 until n) c = c.append(i)

        assertEquals(n, c.size)
        assertEquals((0 until n).toList(), c.toList())
    }

    @Test
    fun childChain_structural_sharing_keeps_common_prefix() {
        // Shared prefix [a, b]; two divergent tails should both reach the
        // very same `c1.append("b")` node via `prev`.
        val c1 = ChildChain.of("a")
        val c2 = c1.append("b")
        val left = c2.append("L")
        val right = c2.append("R")

        assertSame(c2, left.prev)
        assertSame(c2, right.prev)
        assertEquals(listOf("a", "b", "L"), left.toList())
        assertEquals(listOf("a", "b", "R"), right.toList())
    }

    // =============================================================
    //  Extra structural-sharing assertions on TreeDataGrowing that
    //  are easy now we have access to ChildChain directly.
    // =============================================================

    @Test
    fun setNextChildForGrowingParent_does_not_copy_the_prefix() {
        // Build [a, b] under P1 then extend to [a, b, c] under P2.
        // The chain stored for P2 must share its `prev` with the chain
        // stored for P1 (i.e. NO copy of the [a, b] prefix was made).
        val sut = TreeDataGrowing<String, SpptDataNode>(0)
        sut.setFirstChildForGrowing("P0", cn("a", 0, 1))
        sut.setNextChildForGrowingParent("P0", "P1", cn("b", 1, 2))
        sut.setNextChildForGrowingParent("P1", "P2", cn("c", 2, 3))

        // Reach into the snapshot to confirm content; structural sharing
        // is exercised by the tests on ChildChain itself.
        assertEquals(listOf("a", "b"), sut.growingChildren["P1"]!!.map { it.rule.tag })
        assertEquals(listOf("a", "b", "c"), sut.growingChildren["P2"]!!.map { it.rule.tag })
    }
}

