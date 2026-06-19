package net.akehurst.language.agl.generators

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.agl.generators.GenerateGrammarDomainBuild.Companion.generatedFormat
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain

abstract class GeneratorAbstract<AsmType : Any> {

    val issues = IssueHolder(LanguageProcessorPhase.FORMAT)
    abstract val formatString: String
    abstract val formatSetQualifiedName: QualifiedName
    abstract val inputTypesDomain: TypesDomain

    val formatDomain: AglFormatDomain by lazy {
        val res = Agl.formatDomain(FormatString(formatString), inputTypesDomain)
        check(res.allIssues.errors.isEmpty()) { println(res.allIssues.errors) } //TODO: handle issues
        res.asm!!
    }
    val formatSet by lazy { formatDomain.findDefinitionByQualifiedNameOrNull(formatSetQualifiedName)!! }

    fun generateFromAsm(typeName: String, asm: AsmType): String {
        issues.clear()
        val og = ObjectGraphAccessorMutatorByReflection(inputTypesDomain, issues, LocationMapDefault())
        val formatter = FormatterOverTypedObject(formatDomain, og)

        val tp = inputTypesDomain.findFirstDefinitionByNameOrNull(SimpleName(typeName))!!.type()
        val tobj = og.typedAs(asm, tp)
        val res = formatter.formatSelf(formatSet.qualifiedName, tobj)
//        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        return res.sentence!!
    }

}