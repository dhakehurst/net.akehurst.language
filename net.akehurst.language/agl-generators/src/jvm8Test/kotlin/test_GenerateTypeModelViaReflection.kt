package net.akehurst.language.agl.generators

import net.akehurst.language.api.language.base.SimpleName
import kotlin.test.Test

class test_GenerateTypeModelViaReflection {

    @Test
    fun t() {
        val gen = GenerateTypeModelViaReflection(SimpleName("Test"), true, emptyList())
        gen.namespace("net.akehurst.language.api.language.base")
        val out = gen.generate()
        println(out)
    }

}