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
package net.akehurst.language.agl.processor.vistraq

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.sentence.api.InputLocation
import testFixture.utils.parseError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class test_VistraqQuery_Singles {

    private companion object {

        private val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/version_/grammar.agl")?.readText() ?: error("File not found")
        var processor: LanguageProcessor<Asm, ContextAsmSimple> = tgqlprocessor()

        fun tgqlprocessor(): LanguageProcessor<Asm, ContextAsmSimple> {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processorFromStringSimple(GrammarString(grammarStr)).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.processor!!
            } //TODO: use build
        }

        fun test_process(sentence: String, goal: String, grammarName: String? = null) {
            val proc = when (grammarName) {
                null -> processor
                else -> Agl.processorFromStringSimple(
                    GrammarString(grammarStr),
                    configurationBase = Agl.configuration(Agl.configurationSimple()) {
                        targetGrammarName(grammarName)
                    }
                ).let {
                    assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                    it.processor!!
                }
            }
            val result = proc.process(sentence, Agl.options { parse { goalRuleName(goal) } })
            assertTrue(processor.issues.errors.isEmpty(), processor.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        }

        fun test_process_fail(sentence: String, goal: String, grammarName: String? = null, expected: Set<LanguageIssue>) {
            val proc = when (grammarName) {
                null -> processor
                else -> Agl.processorFromStringSimple(
                    GrammarString(grammarStr),
                    configurationBase = Agl.configuration(Agl.configurationSimple()) {
                        targetGrammarName(grammarName)
                    }
                ).let {
                    assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                    it.processor!!
                }
            }
            val result = proc.process(sentence, Agl.options { parse { goalRuleName(goal) } })
            assertEquals(expected, result.issues.all)
        }

    }

    @Test
    fun Expressions_process_NULL_null() {
        val sentence = "null"
        val grammarName = "Expressions"
        val goal = "NULL"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_REAL_0__fail() {
        val sentence = "0"
        val grammarName = "Expressions"
        val goal = "REAL"
        val expected = setOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("REAL"))
        )
        test_process_fail(sentence, goal, grammarName, expected)
    }

    @Test
    fun Expressions_process_REAL_p0__fail() {
        val sentence = ".0"
        val grammarName = "Expressions"
        val goal = "REAL"
        val expected =  setOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("REAL"))
        )
        test_process_fail(sentence, goal, grammarName, expected)
    }

    @Test
    fun Expressions_process_REAL_0p0() {
        val sentence = "0.0"
        val grammarName = "Expressions"
        val goal = "REAL"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_REAL_3p14() {
        val sentence = "3.14"
        val grammarName = "Expressions"
        val goal = "REAL"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_expression_1() {
        val sentence = "1"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_literalValue_0p1() {
        val sentence = "0.1"
        val grammarName = "Expressions"
        val goal = "literalValue"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_expression_true() {
        val sentence = "true"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_navigationExpression_apb() {
        val sentence = "a.b"
        val grammarName = "Expressions"
        val goal = "navigationExpression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_expression_apb() {
        val sentence = "a.b"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Expressions_process_expression_0p1() {
        val sentence = "0.1"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun GraphPathExpressions_process_nodeSelector_NOT_A() {
        val sentence = "NOT A"
        val grammarName = "GraphPathExpressions"
        val goal = "nodeSelector"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun GraphPathExpressions_process_pathExpression_NOT_A() {
        val sentence = "NOT A"
        val grammarName = "GraphPathExpressions"
        val goal = "pathExpression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_pathQuery_MATCH_NOT_A() {
        val sentence = "MATCH NOT A"
        val grammarName = "Queries"
        val goal = "pathQuery"
        test_process(sentence, goal, grammarName)
    }

    @Test(timeout = 5000)
    fun Expressions_process_expression_3x_AND_nav_3() {
        val sentence = "a.p AND b.p AND c.p AND d.p"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test(timeout = 5000)
    fun Expressions_process_expression_5x_AND_nav() {
        val sentence = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test//(timeout = 5000)
    fun Expressions_process_expression_11x_AND_nav() {
        val sentence = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p AND g.p AND h.p AND i.p AND j.p AND k.p AND l.p"
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test//(timeout = 5000)
    fun Expressions_process_long_fromBlog() {
        val sentence = """
     ms.released == true
     AND ft.status == 'done'
     AND task.status == 'done'
     AND iResult.value == 'pass'
     AND uResult.value == 'pass'
     AND build.testCoverage >= 80
     AND bug.status == 'done'
     AND bResult.value == 'pass'
     AND build.buildDate < ms.dueDate
     AND build.version == ms.version
     AND binary.version == ms.version
     AND binary.publishedDate < ms.dueDate
        """.trimIndent()
        val grammarName = "Expressions"
        val goal = "expression"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_whereClause_expression_literalValue_true() {
        val sentence = "WHERE true"
        val grammarName = "Queries"
        val goal = "whereClause"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_pathQuery1() {
        val sentence = "MATCH Milestone WHERE true"
        val grammarName = "Queries"
        val goal = "pathQuery"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_singleQuery_MATCH_X_RETURN_1() {
        val sentence = "MATCH Milestone RETURN VALUE 1"
        val grammarName = "Queries"
        val goal = "singleQuery"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_singleQuery_MATCH_X_WHERE_t_RETURN_1() {
        val sentence = """
   MATCH Milestone WHERE true RETURN VALUE 1
        """.trimIndent()
        val grammarName = "Queries"
        val goal = "singleQuery"
        test_process(sentence, goal, grammarName)
    }

    @Test
    fun Queries_process_columnDefinition() {
        val sentence = "COLUMN a CONTAINING a;"
        val grammarName = "Queries"
        val goal = "columnDefinition"
        test_process(sentence, goal,grammarName)
    }

    @Test
    fun Queries_process_columnDefinition_with_whereClause() {
        val sentence = "COLUMN a CONTAINING a WHERE true;"
        val grammarName = "Queries"
        val goal = "columnDefinition"
        test_process(sentence, goal,grammarName)
    }

    @Test(timeout = 5000)
    fun Queries_process_query1() {
        val sentence = "MATCH A WHERE a == b AND a == b AND true RETURN TABLE COLUMN a CONTAINING a;"
        val grammarName = "Queries"
        val goal = "query"
        test_process(sentence, goal, grammarName)
    }

    @Test(timeout = 5000)
    fun Queries_process_q2() {
        val sentence = """
            FOR TIMESPAN all EVERY month
             FROM artefactCount
             RETURN TABLE
             COLUMN timestamp CONTAINING timestamp;
             COLUMN count CONTAINING count;
             COLUMN X CONTAINING Name WHERE Name=='X';
             COLUMN Y CONTAINING Name WHERE Name=='Y';
        """.trimIndent()
        val grammarName = "Queries"
        val goal = "query"
        test_process(sentence, goal, grammarName)
    }

    @Test(timeout = 5000)
    fun Queries_process_query2() {
        val sentence = """
FOR TIMESPAN '01-Jan-2017' UNTIL '31-Dec-2017' EVERY month
 MATCH Milestone AS ms
   WHERE ms.dueDate <= now
   RETURN TABLE COLUMN Due CONTAINING COUNT(ms);
 JOIN
   MATCH Milestone AS ms
    ( LINKED * TIMES WITH Bug AS bug
      LINKED USING 1..* LINKS WITH TestResult AS bResult
    OR LINKED WITH Feature AS ft
      ( LINKED * TIMES WITH SubTask AS task
      OR LINKED WITH Requirement AS req
        ( LINKED USING 1..* LINKS WITH TestResult AS iResult
        AND
          LINKED TO CodeFile AS code
          LINKED USING 1..* LINKS WITH TestResult AS uResult
        ) ) )
    LINKED 1 TIMES TO Build AS build
    LINKED 1 TIMES TO Binary AS binary
   RETURN TABLE COLUMN Met CONTAINING COUNT(ms);
 JOIN
   RETURN TABLE COLUMN Percent CONTAINING (Met / Due)* 100;
        """.trimIndent()
        val grammarName = "Queries"
        val goal = "query"
        test_process(sentence, goal, grammarName)
    }

    @ExperimentalTime
    @Test//(timeout = 10000)
    fun Queries_process__fromBlog() {
        val queryStr = """
FOR TIMESPAN '01-Jan-2017' UNTIL '31-Dec-2017' EVERY month
 MATCH Milestone AS ms
   WHERE ms.dueDate <= now
   RETURN TABLE COLUMN Due CONTAINING COUNT(ms);
 JOIN
   MATCH Milestone AS ms
    ( LINKED * TIMES WITH Bug AS bug
      LINKED USING 1..* LINKS WITH TestResult AS bResult
    OR LINKED WITH Feature AS ft
      ( LINKED * TIMES WITH SubTask AS task
      OR LINKED WITH Requirement AS req
        ( LINKED USING 1..* LINKS WITH TestResult AS iResult
        AND
          LINKED TO CodeFile AS code
          LINKED USING 1..* LINKS WITH TestResult AS uResult
        ) ) )
    LINKED 1 TIMES TO Build AS build
    LINKED 1 TIMES TO Binary AS binary
   WHERE
     ms.released == true AND ft.status == 'done' AND task.status == 'done'
     AND iResult.value == 'pass' AND uResult.value == 'pass'
     AND build.testCoverage >= 80 AND bug.status == 'done'
     AND bResult.value == 'pass' AND build.buildDate < ms.dueDate
     AND build.version == ms.version AND binary.version == ms.version
     AND binary.publishedDate < ms.dueDate
   RETURN TABLE COLUMN Met CONTAINING COUNT(ms);
 JOIN
   RETURN TABLE COLUMN Percent CONTAINING (Met / Due)* 100;
        """.trimIndent()

        println("parse")
        val goal = "query"
        processor.parse(queryStr, ParseOptionsDefault(goalRuleName = goal))
        println("timed parse")
        val v = measureTimedValue {
            processor.parse(queryStr, ParseOptionsDefault(goalRuleName = goal))
        }
        println(v.duration)
        val result = v.value
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)

        val res = processor.process(queryStr)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun Queries_process_returnTable1() {
        val sentence = "RETURN TABLE COLUMN a CONTAINING a; ORDER BY a"
        val grammarName = "Queries"
        val goal = "returnTable"
        test_process(sentence, goal,grammarName)
    }

    @Test
    fun Queries_process_returnTable2() {
        val sentence = "MATCH A AS a LINKED TO B AS b RETURN TABLE COLUMN a CONTAINING a.identity; COLUMN b CONTAINING b.identity; ORDER BY a"
        val grammarName = "Queries"
        val goal = "singleQuery"
        test_process(sentence, goal,grammarName)
    }

    @Test
    fun Queries_process_RETURN_null() {
        val sentence = "RETURN VALUE null"
        val grammarName = "Queries"
        val goal = "query"
        test_process(sentence, goal,grammarName)
    }
}
