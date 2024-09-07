package net.akehurst.language.agl.generators

import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import kotlin.test.Test

class test_GenerateTypeModelViaReflection {

    private fun gen_base(): TypeModel {
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            listOf(SimpleTypeModelStdLib),
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.language.base")
        gen.addPackage("net.akehurst.language.agl.language.base")
        return gen.generate()
    }

    @Test
    fun test_format_base() {
        val tm = gen_base()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = listOf(SimpleTypeModelStdLib.qualifiedName),
                properties =PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_runtime(): TypeModel {
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            listOf(SimpleTypeModelStdLib),
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.agl.api.runtime")
        return gen.generate()
    }

    @Test
    fun test_format_runtime() {
        val tm = gen_runtime()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = listOf(SimpleTypeModelStdLib.qualifiedName),
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf()))
    }

    fun gen_grammar():Pair<TypeModel,List<QualifiedName>> {
        val baseTm = gen_base()
        val added = baseTm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            baseTm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.language.grammar")
        gen.addPackage("net.akehurst.language.agl.language.grammar.asm")
        val tm = gen.generate()
        return Pair(tm,added)
    }

    @Test
    fun test_format_grammar() {
        val (tm,added) = gen_grammar()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_sppt():Pair<TypeModel,List<QualifiedName>> {
        val runtime = gen_runtime()
        val added = runtime.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            runtime.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.parser")
        gen.addPackage("net.akehurst.language.api.sppt")
        gen.addPackage("net.akehurst.language.agl.sppt")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_sppt() {
        val (tm,added) = gen_sppt()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties =PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_typemodel():Pair<TypeModel,List<QualifiedName>> {
        val (grammar,gadded) = gen_grammar()
        val added = grammar.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            grammar.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.typemodel.api")
        gen.addPackage("net.akehurst.language.typemodel.simple")
        gen.addPackage("net.akehurst.language.api.grammarTypeModel")
        gen.addPackage("net.akehurst.language.agl.grammarTypeModel")
        val tm = gen.generate()
        return Pair(tm,added)
    }

    @Test
    fun test_format_typemodel() {
        val (tm,added) = gen_typemodel()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_style():Pair<TypeModel,List<QualifiedName>> {
        val baseTm = gen_base()
        val added = baseTm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            baseTm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.language.style")
        gen.addPackage("net.akehurst.language.agl.language.style.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_style() {
        val (tm,added) = gen_style()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_expressions():Pair<TypeModel,List<QualifiedName>> {
        val (typemodel, tmadditions) = gen_typemodel()
        val added = typemodel.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            typemodel.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.language.expressions")
        gen.addPackage("net.akehurst.language.agl.language.expressions.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_expressions() {
        val (tm,added) = gen_expressions()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_reference():Pair<TypeModel,List<QualifiedName>> {
       val (expr, eadd) = gen_expressions()
       val added = expr.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            expr.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.language.reference")
        gen.addPackage("net.akehurst.language.agl.language.reference.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_reference() {
        val (tm,added) = gen_reference()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                exludedNamespaces = added,
                properties = PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_api():Pair<TypeModel,List<QualifiedName>> {
        val runtime = gen_runtime()
        val (grammar,added) = gen_grammar()
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            runtime.namespace + grammar.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        gen.addPackage("net.akehurst.language.api.parser")
        gen.addPackage("net.akehurst.language.api.processor")
        val tm = gen.generate()
        return Pair(tm, listOf())
    }

    @Test
    fun test_format_api() {
        val (tm,added) = gen_api()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(
            TypeModelFormatConfiguration(
                properties =PropertiesTypeModelFormatConfiguration(
                    includeDerived = false
                )
            )
        )
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf()))
    }
}