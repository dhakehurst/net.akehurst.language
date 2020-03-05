package net.akehurst.language.processor.xml

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
//import java.io.BufferedReader
//import java.io.InputStreamReader
import java.util.ArrayList

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import net.akehurst.language.processor.vistraq.test_QueryParserValid
import java.io.BufferedReader
import java.io.InputStreamReader


class test_StatechartTools_Singles {

    companion object {
        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl").readText()
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl").readText()
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processor(grammarStr1)
        var processor: LanguageProcessor = Agl.processor(grammarStr2)
    }

    @Test
    fun ConditionalExpression_integer() {
        val goal = "ConditionalExpression"
        val sentence = "integer"
        val result = processor.parse(goal, sentence)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(sentence, resultStr)
    }

    @Test
    fun ConditionalExpression_97() {
        val goal = "ConditionalExpression"
        val sentence = "97"
        val result = processor.parse(goal, sentence)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(sentence, resultStr)
    }

    @Test
    fun AssignmentExpression_integer_AS_97() {
        val goal = "AssignmentExpression"
        val sentence = "integer = 97"
        val result = processor.parse(goal, sentence)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(sentence, resultStr)
    }

    @Test
    fun ScopeDeclaration_integer_AS_97() {
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer = 97"
        val result = processor.parse(goal, sentence)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(sentence, resultStr)
    }

}
