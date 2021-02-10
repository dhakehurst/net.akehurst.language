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
package net.akehurst.language.agl.processor.dot

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
//import java.io.BufferedReader
//import java.io.InputStreamReader

import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.fail


class test_Dot_Singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/dot/Dot.agl").readText()
        var processor: LanguageProcessor = Agl.processor(grammarStr)

    }

    @Test
    fun SINGLE_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          // a comment
          graph { }
        """.trimIndent()
        processor.parse(goal, sentence)
    }
    @Test
    fun MULTI_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          /* a comment */
          graph { }
        """.trimIndent()
        processor.parse(goal, sentence)
    }
    @Test
    fun a_list__from_Data_Structures() {
        val goal = "attr_list_content"
        val sentence = """
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        """
        processor.parse(goal, sentence)
    }

    @Test
    fun attr_list__from_Data_Structures() {
        val goal = "attr_list"
        val sentence = """[
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        ]"""
        processor.parse(goal, sentence)
    }

    @Test
    fun attr_stmt__from_Data_Structures() {
        val goal = "attr_stmt"
        val sentence = """
            edge [ ]
        """.trimIndent()
        processor.parse(goal, sentence)
    }

    @Test
    fun stmt_list__from_Data_Structures() {
        val goal = "stmt_list"
        val sentence = """
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
        """.trimIndent()
        processor.parse(goal, sentence)
    }

    @Test
    fun graph__from_Data_Structures() {
        val goal = "graph"
        val sentence = """
            digraph g {
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
            "node0" [
            label = "<f0> 0x10ba8| <f1>"
            shape = "record"
            ];
            }
        """.trimIndent()
        processor.parse(goal, sentence)

    }

    @Test
    fun stmt_list__1() {
        val goal = "stmt_list"
        val sentence = "graph[a=a ]; node [b=b c=c]; edge[];"
        processor.parse(goal, sentence)
    }

    @Test
    fun attr_list__2s() {
        val goal = "attr_list"
        val sentence = "[x = x; y=y]"
        processor.parse(goal, sentence)

    }

    @Test
    fun attr_list__2n() {
        val goal = "attr_list"
        val sentence = "[x = x y=y]"
        processor.parse(goal, sentence)

    }

    @Test
    fun LionShare__node_id() {
        val goal = "node_id"
        val sentence = """
            "001"
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun LionShare__attr_list() {
        val goal = "attr_list"
        val sentence = """
            [shape=box     , regular=1,style=filled,fillcolor=white   ]
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun LionShare__node_stmt() {
        val goal = "node_stmt"
        val sentence = """
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ]
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun stmt_list_automaton() {
        val goal = "stmt_list"
        val sentence = "a -> b ;"

        val converterToRuntimeRules = ConverterToRuntimeRules(processor.grammar)
        val parser = ScanOnDemandParser(converterToRuntimeRules.transform())
        val rrs = parser.runtimeRuleSet
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD
        val lhs_U = rrs.createLookaheadSet(setOf(UP))
        val stmt_list = rrs.findRuntimeRule("stmt_list")
        val stmt_list_multi = stmt_list.rhs.items[0]
        val stmt1 = rrs.findRuntimeRule("stmt1")
        val stmt = rrs.findRuntimeRule("stmt")
        val edge_list = rrs.findRuntimeRule("edge_list")
        val edge_list_m = rrs.findRuntimeRule("§edge_list§sList1")
        val SM = rrs.fetchStateSetFor(stmt_list)
        val rps = stmt_list_multi.rulePositionsAt[0]



        val stmt_list_0_0_firstOf = SM.buildCache.firstOf(RulePosition(stmt_list,0,0), setOf(UP))
        val stmt_list_multi_0_0_firstOf = SM.buildCache.firstOf(RulePosition(stmt_list_multi,0,0), setOf(UP))
        val stmt_list_multi_0_1_firstOf = SM.buildCache.firstOf(RulePosition(stmt_list_multi,0,RulePosition.POSITION_MULIT_ITEM), setOf(UP))
        val stmt1_0_0_firstOf = SM.buildCache.firstOf(RulePosition(stmt1,0,0), setOf(UP))
        val stmt1_0_1_firstOf = SM.buildCache.firstOf(RulePosition(stmt1,0,1), setOf(UP))

        val edge_list_m_rps = edge_list_m.rulePositionsAt[0]
        val edge_list_m_firstOf_0_0 = SM.buildCache.firstOf(RulePosition(edge_list_m,0,0), setOf(UP))
        val edge_list_m_rps_nxt = edge_list_m_rps.flatMap { it.next() }
        val edge_list_m_rps_nxt_nxt = edge_list_m_rps_nxt.flatMap { it.next() }
     //   val edge_list_m_firstOf_0_2 = SM.firstOf(RulePosition(edge_list_m, RuntimeRuleItem.SLIST__ITEM,RulePosition.SLIST_ITEM_POSITION), setOf(UP))

        val cls_stmt_list_0_0 = SM.buildCache.calcClosure(RulePosition(stmt_list,0,0),lhs_U)
        val cls_edge_list_0_0 = SM.buildCache.calcClosure(RulePosition(edge_list,0,0),lhs_U)
        val cls_edge_list_m = SM.buildCache.calcClosure(RulePosition(edge_list_m,RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR,RulePosition.POSITION_SLIST_SEPARATOR),lhs_U)
        val edge_list_m_firstOf_0_2 = SM.buildCache.firstOf(RulePosition(edge_list_m, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR,RulePosition.POSITION_SLIST_ITEM), setOf(UP))

        //lh of sList at pos 2 doesn't work (firstOf)?

        val filt = cls_edge_list_m.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
        val grouped = filt.groupBy { it.rulePosition.item!! }.map {
            val rr = it.key
            val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
            val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
            val lhs = SM.createLookaheadSet(lhsc)
            Pair(rp, lhs)
        }.toSet()
        //fails at season 9 with edge_list
        parser.parse(goal, sentence)
    }

    @Test
    fun stmt_list1() {
        val goal = "stmt_list"
        val sentence = "a -> b ;"
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun LionShare__stmt_list1() {
        val goal = "stmt_list"
        val sentence = """
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun LionShare__stmt_list() {
        val goal = "stmt_list"
        val sentence = """
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "002" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "003" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "004" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "005" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "006" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "007" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "009" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "014" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "015" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "016" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ01" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ02" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "017" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "012" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "008" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "011" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "013" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "010" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "023" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "020" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "021" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "018" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "025" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "019" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "022" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "024" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "027" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "026" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "028" [shape=box     , regular=1,style=filled,fillcolor=grey    ] ;
            "marr0001" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0001" [dir=none,weight=1] ;
            "007" -> "marr0001" [dir=none,weight=1] ;
            "marr0001" -> "017" [dir=none, weight=2] ;
            "marr0002" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0002" [dir=none,weight=1] ;
            "ZZ02" -> "marr0002" [dir=none,weight=1] ;
            "marr0002" -> "012" [dir=none, weight=2] ;
            "marr0003" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0003" [dir=none,weight=1] ;
            "003" -> "marr0003" [dir=none,weight=1] ;
            "marr0003" -> "008" [dir=none, weight=2] ;
            "marr0004" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0004" [dir=none,weight=1] ;
            "006" -> "marr0004" [dir=none,weight=1] ;
            "marr0004" -> "011" [dir=none, weight=2] ;
            "marr0005" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0005" [dir=none,weight=1] ;
            "ZZ01" -> "marr0005" [dir=none,weight=1] ;
            "marr0005" -> "013" [dir=none, weight=2] ;
            "marr0006" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "004" -> "marr0006" [dir=none,weight=1] ;
            "009" -> "marr0006" [dir=none,weight=1] ;
            "marr0006" -> "010" [dir=none, weight=2] ;
            "marr0007" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0007" [dir=none,weight=1] ;
            "015" -> "marr0007" [dir=none,weight=1] ;
            "marr0007" -> "023" [dir=none, weight=2] ;
            "marr0008" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0008" [dir=none,weight=1] ;
            "016" -> "marr0008" [dir=none,weight=1] ;
            "marr0008" -> "020" [dir=none, weight=2] ;
            "marr0009" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0009" [dir=none,weight=1] ;
            "012" -> "marr0009" [dir=none,weight=1] ;
            "marr0009" -> "021" [dir=none, weight=2] ;
            "marr0010" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "008" -> "marr0010" [dir=none,weight=1] ;
            "017" -> "marr0010" [dir=none,weight=1] ;
            "marr0010" -> "018" [dir=none, weight=2] ;
            "marr0011" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "011" -> "marr0011" [dir=none,weight=1] ;
            "023" -> "marr0011" [dir=none,weight=1] ;
            "marr0011" -> "025" [dir=none, weight=2] ;
            "marr0012" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "013" -> "marr0012" [dir=none,weight=1] ;
            "014" -> "marr0012" [dir=none,weight=1] ;
            "marr0012" -> "019" [dir=none, weight=2] ;
            "marr0013" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "010" -> "marr0013" [dir=none,weight=1] ;
            "021" -> "marr0013" [dir=none,weight=1] ;
            "marr0013" -> "022" [dir=none, weight=2] ;
            "marr0014" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "019" -> "marr0014" [dir=none,weight=1] ;
            "020" -> "marr0014" [dir=none,weight=1] ;
            "marr0014" -> "024" [dir=none, weight=2] ;
            "marr0015" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "022" -> "marr0015" [dir=none,weight=1] ;
            "025" -> "marr0015" [dir=none,weight=1] ;
            "marr0015" -> "027" [dir=none, weight=2] ;
            "marr0016" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "024" -> "marr0016" [dir=none,weight=1] ;
            "018" -> "marr0016" [dir=none,weight=1] ;
            "marr0016" -> "026" [dir=none, weight=2] ;
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun LionShare() {
        val goal = "graph"
        val sentence = """
            ##"A few people in the field of genetics are using dot to draw "marriage node diagram"  pedigree drawings.  Here is one I have done of a test pedigree from the FTREE pedigree drawing package (Lion Share was a racehorse)." Contributed by David Duffy.
            
            ##Command to get the layout: "dot -Tpng thisfile > thisfile.png"
            
            digraph Ped_Lion_Share           {
            # page = "8.2677165,11.692913" ;
            ratio = "auto" ;
            mincross = 2.0 ;
            label = "Pedigree Lion_Share" ;
            
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "002" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "003" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "004" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "005" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "006" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "007" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "009" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "014" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "015" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "016" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ01" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ02" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "017" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "012" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "008" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "011" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "013" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "010" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "023" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "020" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "021" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "018" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "025" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "019" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "022" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "024" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "027" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "026" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "028" [shape=box     , regular=1,style=filled,fillcolor=grey    ] ;
            "marr0001" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0001" [dir=none,weight=1] ;
            "007" -> "marr0001" [dir=none,weight=1] ;
            "marr0001" -> "017" [dir=none, weight=2] ;
            "marr0002" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0002" [dir=none,weight=1] ;
            "ZZ02" -> "marr0002" [dir=none,weight=1] ;
            "marr0002" -> "012" [dir=none, weight=2] ;
            "marr0003" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0003" [dir=none,weight=1] ;
            "003" -> "marr0003" [dir=none,weight=1] ;
            "marr0003" -> "008" [dir=none, weight=2] ;
            "marr0004" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0004" [dir=none,weight=1] ;
            "006" -> "marr0004" [dir=none,weight=1] ;
            "marr0004" -> "011" [dir=none, weight=2] ;
            "marr0005" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0005" [dir=none,weight=1] ;
            "ZZ01" -> "marr0005" [dir=none,weight=1] ;
            "marr0005" -> "013" [dir=none, weight=2] ;
            "marr0006" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "004" -> "marr0006" [dir=none,weight=1] ;
            "009" -> "marr0006" [dir=none,weight=1] ;
            "marr0006" -> "010" [dir=none, weight=2] ;
            "marr0007" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0007" [dir=none,weight=1] ;
            "015" -> "marr0007" [dir=none,weight=1] ;
            "marr0007" -> "023" [dir=none, weight=2] ;
            "marr0008" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0008" [dir=none,weight=1] ;
            "016" -> "marr0008" [dir=none,weight=1] ;
            "marr0008" -> "020" [dir=none, weight=2] ;
            "marr0009" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0009" [dir=none,weight=1] ;
            "012" -> "marr0009" [dir=none,weight=1] ;
            "marr0009" -> "021" [dir=none, weight=2] ;
            "marr0010" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "008" -> "marr0010" [dir=none,weight=1] ;
            "017" -> "marr0010" [dir=none,weight=1] ;
            "marr0010" -> "018" [dir=none, weight=2] ;
            "marr0011" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "011" -> "marr0011" [dir=none,weight=1] ;
            "023" -> "marr0011" [dir=none,weight=1] ;
            "marr0011" -> "025" [dir=none, weight=2] ;
            "marr0012" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "013" -> "marr0012" [dir=none,weight=1] ;
            "014" -> "marr0012" [dir=none,weight=1] ;
            "marr0012" -> "019" [dir=none, weight=2] ;
            "marr0013" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "010" -> "marr0013" [dir=none,weight=1] ;
            "021" -> "marr0013" [dir=none,weight=1] ;
            "marr0013" -> "022" [dir=none, weight=2] ;
            "marr0014" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "019" -> "marr0014" [dir=none,weight=1] ;
            "020" -> "marr0014" [dir=none,weight=1] ;
            "marr0014" -> "024" [dir=none, weight=2] ;
            "marr0015" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "022" -> "marr0015" [dir=none,weight=1] ;
            "025" -> "marr0015" [dir=none,weight=1] ;
            "marr0015" -> "027" [dir=none, weight=2] ;
            "marr0016" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "024" -> "marr0016" [dir=none,weight=1] ;
            "018" -> "marr0016" [dir=none,weight=1] ;
            "marr0016" -> "026" [dir=none, weight=2] ;
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
            }
            """.trimIndent()
        processor.parse(goal, sentence)
    }

    @Test
    fun emptyString() {
        val goal = "DOUBLE_QUOTE_STRING"
        val sentence = """
            ""
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }
    @Test
    fun node_stmt_with_emptyString() {
        val goal = "node_stmt"
        val sentence = """
            node[style=filled,label=""]
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }

    @Test
    fun Synchronous_Digital_Hierarchy_Stack() {
        val goal = "graph"
        val sentence = """
digraph G {
	graph [bgcolor=black];	/* set background */
	edge [color=white];
	graph[page="8.5,11",size="7.5,7",ratio=fill,center=1];
	node[style=filled,label=""];
	subgraph ds3CTP {
		rank = same;
		node[shape=box,color=green];
		ds3CTP_1_1;
		ds3CTP_1_2;
		ds3CTP_5_1;
		ds3CTP_5_2;
	}
	subgraph t3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		t3TTP_1_1;
		t3TTP_5_2;
	}
	subgraph vc3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		vc3TTP_1_2;
		vc3TTP_5_1;
	}
	subgraph fabric {
		rank = same;
		node[shape=hexagon,color=blue];
		fabric_1_2;
		fabric_4_1;
		fabric_5_1;
	}
	subgraph xp {
		rank = same;
		node[shape=diamond,color=blue];
		xp_1_2;
		xp_4_1;
		xp_5_1;
	}
	subgraph au3CTP {
		rank = same;
		node[shape=box,color=green];
		au3CTP_1_2;
		au3CTP_5_1;
	}
	subgraph aug {
		rank = same;
		node[shape=invtrapezium,color=pink];
		aug_1_2;
		aug_4_1;
		aug_4_2;
		aug_5_1;
	}
	subgraph protectionTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph protectionGroup {
		rank = same;
		node[shape=hexagon,color=blue];
	}
	subgraph protectionUnit {
		rank = same;
		node[shape=diamond,color=blue];
	}
	subgraph protectionCTP {
		node[shape=box,color=green];
	}
	subgraph msTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph msCTP {
		rank = same;
		node[shape=box,color=green];
	}
	subgraph rsTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph rsCTP {
		rank = same;
		node[shape=box,color=green];
	}
	subgraph spiTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph me {
		rank = same;
		node[shape=box,peripheries=2];
		me_1;
	}
	subgraph client_server {
		edge[style=dotted,dir=none,weight=100];
		ds3CTP_1_1->t3TTP_1_1;
		ds3CTP_1_2->vc3TTP_1_2;
		au3CTP_1_2->aug_1_2->prTTP_1_2;
	}
}
        """.trimIndent()
        try {
            processor.parse(goal, sentence)
        } catch (e: ParseFailedException) {
            fail("${e.message} at ${e.location} expected ${e.expected}")
        }
    }
}
