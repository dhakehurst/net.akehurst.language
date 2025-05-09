package net.akehurst.language.agl.generators

import net.akehurst.language.api.processor.*
import net.akehurst.language.typemodel.processor.AglTypes
import kotlin.test.Test
import kotlin.test.assertEquals

class test_GenerateGrammarModelBuild {

    @Test
    fun generte_AglTypemodel_grammarString() {
        val gen = GenerateGrammarModelBuild()

        val actual = gen.generateFromString(GrammarString(AglTypes.grammarString))
        println(actual)
    }

    @Test
    fun AglTypemodel_grammarString_assert() {
        val actual = AglTypes.grammarModel.asString()
        val expected = AglTypes.grammarString
        assertEquals(expected, actual)
    }

}