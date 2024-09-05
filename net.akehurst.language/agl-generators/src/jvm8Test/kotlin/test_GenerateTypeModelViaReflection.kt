package net.akehurst.language.agl.generators

import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import kotlin.test.Test

class test_GenerateTypeModelViaReflection {

    @Test
    fun t() {
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            true,
            listOf(SimpleTypeModelStdLib),
            mapOf(
                "kotlin" to SimpleTypeModelStdLib.qualifiedName.value,
                "kotlin.collections" to SimpleTypeModelStdLib.qualifiedName.value
            )
        )
        gen.addPackage("net.akehurst.language.api.language.base")
        val tm = gen.generate()

        println(FormatTypeModelAsKotlinTypeModelBuilder().formatTypeModel(Indent(), tm, true, listOf()))
    }

}