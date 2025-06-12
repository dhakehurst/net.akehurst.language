package net.akehurst.language.agl.generators

import net.akehurst.language.asm.simple.AglAsm
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.reference.processor.AglCrossReference
import net.akehurst.language.scope.processor.AglScope
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.processor.AglTypes
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

    private fun gen_base(): Pair<TypeModel,List<QualifiedName>> {
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Base"),
            listOf(StdLibDefault),
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString)
        )
        gen.addPackage("net.akehurst.language.base.api")
        gen.addPackage("net.akehurst.language.base.asm")
        val tm = gen.generate()
        return Pair(tm, emptyList())
    }

    @Test
    fun test_format_base() {
        val (tm,added) = gen_base()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(listOf(StdLibDefault.qualifiedName)))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_grammar():Pair<TypeModel,List<QualifiedName>> {
        val (btm,_) = gen_base()
        val added = btm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Grammar"),
            btm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglGrammar.kompositeString)
        )
        gen.addPackage("net.akehurst.language.grammar.api")
        gen.addPackage("net.akehurst.language.grammar.asm")
        val tm = gen.generate()
        return Pair(tm,added)
    }

    @Test
    fun test_format_grammar() {
        val (tm,added) = gen_grammar()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_typemodel():Pair<TypeModel,List<QualifiedName>> {
        val (grammar,gadded) = gen_grammar()
        val added = grammar.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Typemodel"),
            grammar.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglTypes.kompositeString)
        )
        gen.addPackage("net.akehurst.language.typemodel.api")
        gen.addPackage("net.akehurst.language.typemodel.asm")
        gen.addPackage("net.akehurst.language.grammarTypemodel.api")
        gen.addPackage("net.akehurst.language.grammarTypemodel.asm")
        val tm = gen.generate()
        return Pair(tm,added)
    }

    @Test
    fun test_format_typemodel() {
        val (tm,added) = gen_typemodel()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_asm():Pair<TypeModel,List<QualifiedName>> {
        val (baseTm,_) = gen_base()
        val added = baseTm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Asm"),
            baseTm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglAsm.komposite)
        )
        gen.include("net.akehurst.language.collections.ListSeparated")
        gen.addPackage("net.akehurst.language.asm.api")
        gen.addPackage("net.akehurst.language.asm.simple")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_asm() {
        val (tm,added) = gen_asm()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf()))
    }

    fun gen_expressions():Pair<TypeModel,List<QualifiedName>> {
        val (btm, _) = gen_base()
        val added = btm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            btm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglExpressions.kompositeString)
        )
        gen.addPackage("net.akehurst.language.expressions.api")
        gen.addPackage("net.akehurst.language.expressions.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_expressions() {
        val (tm,added) = gen_expressions()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_reference():Pair<TypeModel,List<QualifiedName>> {
        val (expr, eadd) = gen_expressions()
        val added = expr.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            expr.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglExpressions.kompositeString, AglCrossReference.komposite)
        )
        gen.addPackage("net.akehurst.language.reference.api")
        gen.addPackage("net.akehurst.language.reference.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_reference() {
        val (tm,added) = gen_reference()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_scope():Pair<TypeModel,List<QualifiedName>> {
        val (btm,_) = gen_base()
        val added = btm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Scope"),
            btm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglScope.komposite)
        )
        gen.addPackage("net.akehurst.language.scope.api")
        gen.addPackage("net.akehurst.language.scope.asm")
        val tm = gen.generate()
        return Pair(tm,added)
    }

    @Test
    fun test_format_scope() {
        val (tm,added) = gen_scope()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

    fun gen_style():Pair<TypeModel,List<QualifiedName>> {
        val (baseTm,_) = gen_base()
        val added = baseTm.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            baseTm.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            listOf(AglBase.kompositeString, AglStyle.komposite)
        )
        gen.addPackage("net.akehurst.language.style.api")
        gen.addPackage("net.akehurst.language.style.asm")
        val tm = gen.generate()
        return Pair(tm, added)
    }

    @Test
    fun test_format_style() {
        val (tm,added) = gen_style()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(added))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }


    fun gen_runtime(): TypeModel {
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            listOf(StdLibDefault),
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            emptyList()
        )
        gen.addPackage("net.akehurst.language.agl.api.runtime")
        return gen.generate()
    }

    @Test
    fun test_format_runtime() {
        val tm = gen_runtime()
        val fmrtr = FormatTypeModelAsKotlinTypeModelBuilder(formatConfig(listOf(StdLibDefault.qualifiedName)))
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf()))
    }

    fun gen_sppt():Pair<TypeModel,List<QualifiedName>> {
        val runtime = gen_runtime()
        val added = runtime.namespace.map { it.qualifiedName }
        val gen = GenerateTypeModelViaReflection(
            SimpleName("Test"),
            runtime.namespace,
            GenerateTypeModelViaReflection.KOTLIN_TO_AGL,
            emptyList()
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
        println(fmrtr.formatTypeModel(Indent(), tm, true, listOf("std")))
    }

}