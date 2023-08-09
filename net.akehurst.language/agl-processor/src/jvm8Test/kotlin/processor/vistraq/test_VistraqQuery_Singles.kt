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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.LanguageProcessorPhase
import org.junit.Test
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class test_VistraqQuery_Singles {

    private companion object {

        private val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/Query.agl")?.readText() ?: error("File not found")
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = tgqlprocessor()

        fun tgqlprocessor(): LanguageProcessor<AsmSimple, ContextSimple> {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processorFromStringDefault(grammarStr).processor!! //TODO: use build
        }

    }

    @Test
    fun NULL() {
        val sentence = "null"
        val goal = "NULL"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun REAL_0() {
        val sentence = "0"
        val goal = "REAL"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNull(result.sppt)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^0", setOf("REAL"))
            ), result.issues.all
        )
    }

    @Test
    fun REAL_p0() {
        val sentence = ".0"
        val goal = "REAL"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNull(result.sppt)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^.0", setOf("REAL"))
            ), result.issues.all
        )

    }

    @Test
    fun REAL_0p0() {
        val sentence = "0.0"
        val goal = "REAL"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun REAL_3p14() {
        val sentence = "3.14"
        val goal = "REAL"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun expression_1() {
        val queryStr = "1"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun literalValue_0p1() {
        val queryStr = "0.1"
        val goal = "literalValue"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_0p1() {
        val queryStr = "0.1"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun nodeSelector_NOT_A() {
        val queryStr = "NOT A"
        val goal = "nodeSelector"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathExpression_NOT_A() {
        val queryStr = "NOT A"
        val goal = "pathExpression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery_MATCH_NOT_A() {
        val queryStr = "MATCH NOT A"
        val goal = "pathQuery"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_literalValue_true() {
        val queryStr = "true"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun expression_long_3() {
        val queryStr = "a.p AND b.p AND c.p AND d.p"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
        println(result.sppt!!.toStringAllWithIndent("  "))
    }

    @Test(timeout = 5000)
    fun expression_long_5() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test//(timeout = 5000)
    fun expression_long_11() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p AND g.p AND h.p AND i.p AND j.p AND k.p AND l.p"
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test//(timeout = 5000)
    fun expression_long_fromBlog() {
        val queryStr = """
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
        val goal = "expression"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun whereClause_expression_literalValue_true() {
        val queryStr = "WHERE true"
        val goal = "whereClause"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery1() {
        val queryStr = "MATCH Milestone WHERE true"
        val goal = "pathQuery"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery1() {
        val queryStr = "MATCH Milestone RETURN 1"
        val goal = "singleQuery"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery2() {
        val queryStr = """
   MATCH Milestone WHERE true RETURN 1
        """.trimIndent()
        val goal = "singleQuery"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun q2() {
        val queryStr = """
            FOR TIMESPAN all EVERY month
             FROM artefactCount
             RETURN TABLE
             COLUMN timestamp CONTAINING timestamp
             COLUMN count CONTAINING count
             COLUMN X CONTAINING Name WHERE Name=='X'
             COLUMN Y CONTAINING Name WHERE Name=='Y'
        """.trimIndent()
        val goal = "query"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun query1() {
        val queryStr = "MATCH A WHERE a == b AND a == b AND true RETURN TABLE COLUMN a CONTAINING a"
        //val queryStr = "MATCH A  RETURN 1"
        val goal = "query"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun query2() {
        val queryStr = """
FOR TIMESPAN '01-Jan-2017' UNTIL '31-Dec-2017' EVERY month
 MATCH Milestone AS ms
   WHERE ms.dueDate <= now
   RETURN TABLE COLUMN Due CONTAINING COUNT(ms)
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
   RETURN TABLE COLUMN Met CONTAINING COUNT(ms)
 JOIN
   RETURN TABLE COLUMN Percent CONTAINING (Met / Due)* 100
        """.trimIndent()
        //val queryStr = "MATCH A  RETURN 1"

        val goal = "query"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }

    @ExperimentalTime
    @Test//(timeout = 10000)
    fun fromBlog() {
        val queryStr = """
FOR TIMESPAN '01-Jan-2017' UNTIL '31-Dec-2017' EVERY month
 MATCH Milestone AS ms
   WHERE ms.dueDate <= now
   RETURN TABLE COLUMN Due CONTAINING COUNT(ms)
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
   RETURN TABLE COLUMN Met CONTAINING COUNT(ms)
 JOIN
   RETURN TABLE COLUMN Percent CONTAINING (Met / Due)* 100
        """.trimIndent()

        try {
            println("parse")
            val goal = "query"
            processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
            println("timed parse")
            val v = measureTimedValue {
                processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
            }
            println(v.duration)
            val result = v.value
            assertNotNull(result.sppt)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            val resultStr = result.sppt!!.asSentence
            assertEquals(queryStr, resultStr)

            val res = processor.process(queryStr)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        } catch (e: ParseFailedException) {
            fail("${e.message}, at ${e.location}, expected ${e.expected}")
        }
    }
}
