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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.processor.Agl
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class test_VistraqQuery_Singles {

    companion object {

        private val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/Query.agl").readText()
        var processor: LanguageProcessor = tgqlprocessor()

        fun tgqlprocessor() : LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processor(grammarStr).buildFor("query")
         }

    }

    @Test
    fun NULL() {
        processor.parse("NULL", "null")
    }

    @Test
    fun REAL_0() {

        val e = assertFailsWith(ParseFailedException::class) {
            processor.parse("REAL", "0")
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun REAL_p0() {
        val e = assertFailsWith(ParseFailedException::class) {
            processor.parse("REAL", ".0")
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun REAL_0p0() {
        processor.parse("REAL", "0.0")
    }

    @Test
    fun REAL_3p14() {
        processor.parse("REAL", "3.14")
    }

    @Test
    fun expression_1() {
        val queryStr = "1"
        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun literalValue_0p1() {
        val queryStr = "0.1"
        val result = processor.parse("literalValue", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_0p1() {
        val queryStr = "0.1"
        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun nodeSelector_NOT_A() {
        val queryStr = "NOT A"
        val result = processor.parse("nodeSelector", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }


    @Test
    fun pathExpression_NOT_A() {
        val queryStr = "NOT A"
        val result = processor.parse("pathExpression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery_MATCH_NOT_A() {
        val queryStr = "MATCH NOT A"
        val result = processor.parse("pathQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_literalValue_true() {
        val queryStr = "true"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout=5000)
    fun expression_long_3() {
        val queryStr = "a.p AND b.p AND c.p AND d.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
        println(result.toStringIndented("  "))
    }

    @Test(timeout=5000)
    fun expression_long_5() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout=5000)
    fun expression_long_11() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p AND g.p AND h.p AND i.p AND j.p AND k.p AND l.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }


    @Test
    fun whereClause_expression_literalValue_true() {
        val queryStr = "WHERE true"

        val result = processor.parse("whereClause", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery1() {
        val queryStr = "MATCH Milestone WHERE true"

        val result = processor.parse("pathQuery", queryStr)

        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery1() {
        val queryStr = "MATCH Milestone RETURN 1"

        val result = processor.parse("singleQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery2() {
        val queryStr = """
   MATCH Milestone WHERE true RETURN 1
        """.trimIndent()

        val result = processor.parse("singleQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
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

        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun query1() {
        val queryStr = """
   MATCH A WHERE a == b AND a == b AND true RETURN TABLE COLUMN a CONTAINING a
        """.trimIndent()

        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test//(timeout = 5000)
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
            val result = processor.parse("query", queryStr)
            Assert.assertNotNull(result)
            val resultStr = result.asString
            Assert.assertEquals(queryStr, resultStr)
        } catch (e:ParseFailedException) {
            fail("${e.message}, at ${e.location}, expected ${e.expected}")
        }
    }
}
