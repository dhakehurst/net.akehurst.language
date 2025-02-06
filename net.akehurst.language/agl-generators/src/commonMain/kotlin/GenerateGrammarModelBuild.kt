package net.akehurst.language.agl.generators

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.format.processor.FormatterSimple
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarNamespace
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry

class GenerateGrammarModelBuild {

    companion object {
        val generatedFormat = """
            namespace net.akehurst.language.grammar
              format asm {
                GrammarModel -> "grammarModel() {
                
                                }"
              }
        """
        val formatModel by lazy {
            val res = Agl.registry.agl.format.processor!!.process(generatedFormat)
            check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
            res.asm!!
        }
    }


    fun generateFromString(grammarString: GrammarString) {
        val res = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarString.value,
            options = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            }
        )
        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        val asm = res.asm!!
        generateFromAsm(asm)
    }

    fun generateFromAsm(grammarModel: GrammarModel): String {
        val formatter = FormatterSimple<GrammarModel>(formatModel, AglGrammar.typeModel)
        val res = formatter.format(grammarModel)
        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        //val str = grammarModel.namespace.joinToString(separator = "\n\n") { generateNamespace(it) }
        return res.sentence!!
    }

    fun generateNamespace(asm: GrammarNamespace): String {
        val sb = StringBuilder()

        asm.options
        sb.append(asm.import.joinToString(separator = "\n", postfix = "\n") { "import(\"${it}\")" })


        return sb.toString()
    }
}