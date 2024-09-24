package net.akehurst.language.agl.generators

import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeNamespace
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import kotlin.test.Test

class test_GenerateTypeModelViaReflection {

    companion object {
        fun formatConfig(exludedNamespaces:List<QualifiedName>) = TypeModelFormatConfiguration(
            exludedNamespaces = exludedNamespaces,
            includeInterfaces = false,
            properties =PropertiesTypeModelFormatConfiguration(
                includeDerived = false
            )
        )
    }

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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(listOf(SimpleTypeModelStdLib.qualifiedName)))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(listOf(SimpleTypeModelStdLib.qualifiedName)))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
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
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("SimpleTypeModelStdLib")))
    }

    fun gen_asm():Pair<TypeModel,List<QualifiedName>> {
        //TODO: remove - builder needs these and no way to exclude builder at present
        val (typemodel,added1) = gen_typemodel()
        val (reference,added2) = gen_reference()
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            (typemodel.namespace + reference.namespace).toSet().toList(),
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL
        )
        //gen.include("net.akehurst.language.collections.ListSeparated")
        gen.exclude("net.akehurst.language.asm.api.AsmSimpleBuilder")
        gen.addPackage("net.akehurst.language.api.scope")
        gen.addPackage("net.akehurst.language.scope.simple")
        gen.addPackage("net.akehurst.language.asm.api")
        gen.addPackage("net.akehurst.language.asm.simple")
        val tm = gen.generate()
        return Pair(tm, added1+added2)
    }

    @Test
    fun test_format_asm() {
        val (tm,added) = gen_asm()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf()))
    }
}