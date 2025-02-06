package net.akehurst.language.agl.generators

import net.akehurst.language.agl.GrammarString
import net.akehurst.language.typemodel.processor.AglTypemodel
import kotlin.test.Test

class test_GenerateGrammarModelBuild {

    @Test
    fun Agl_types() {
        val gen = GenerateGrammarModelBuild()

        val actual = gen.generateFromString(GrammarString(AglTypemodel.grammarString))
        println(actual)
    }

}