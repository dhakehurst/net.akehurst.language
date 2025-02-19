package net.akehurst.language.agl.generators

import net.akehurst.language.api.processor.*
import net.akehurst.language.typemodel.processor.AglTypemodel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_GenerateGrammarModelBuild {

    @Test
    fun generte_AglTypemodel_grammarString() {
        val gen = GenerateGrammarModelBuild()

        val actual = gen.generateFromString(GrammarString(AglTypemodel.grammarString))
        println(actual)
    }

    @Test
    fun AglTypemodel_grammarString_assert() {
        val actual = AglTypemodel.grammarModel.asString()
        val expected = AglTypemodel.grammarString
        assertEquals(expected, actual)
    }

}