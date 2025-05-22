package agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.TransformString
import kotlin.test.Test
import kotlin.test.assertTrue

typealias MapStringString = MutableMap<String, String>

class test_FileMap {

    companion object {
        val grammar = """
            namespace ide
            grammar FileExtensionMap {
                skip leaf WS = "\s+" ;
                
                unit = entry* ;
                entry = GLOB ':' value ';' ;
                value = PATH | QUALIFIED_NAME ;
                leaf GLOB = "([a-zA-Z0-9_*/.?{},\\[\\]-]|\\.)+" ;
                QUALIFIED_NAME = [ID / '.']+ ;
                PATH = '/' [ID / '/']+ ;
                leaf ID = "[a-zA-Z._][a-zA-Z0-9._-]*" ;
            }
        """

        val asmTransform = """
            #create-missing-types
            #override-default-transform
            
            namespace ide
            transform FileExtensionMap {
               unit : children.asMap as Map<String,String>
               entry : tuple { key:=child[0] value:=child[2] }
               PATH: (child[0] + child[1].children.join) as String
               QUALIFIED_NAME: children.join as String
            }
        """

        val processor: LanguageProcessor<List<MapStringString>, ContextWithScope<Any, Any>> by lazy {
            val res = Agl.processorFromString<List<MapStringString>, ContextWithScope<Any, Any>>(
                grammarDefinitionStr = grammar,
                configuration = Agl.configuration<List<MapStringString>, ContextWithScope<Any, Any>>(base = Agl.configurationBase()) {
                    transformString(TransformString(asmTransform))
//                    syntaxAnalyserResolver { p ->
//                        ProcessResultDefault(
//                            FileExtensionMapSyntaxAnalyser(
//                                p.typesModel,
//                                p.asmTransformModel,
//                                p.targetAsmTransformRuleSet.qualifiedName
//                            ),
//                            IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)
//                        )
//                    }
                }
            )
            check(res.issues.errors.isEmpty()) { res.issues.toString() }
            res.processor!!
        }

        fun process(sentence: String): MapStringString {
            val res = processor.process(sentence)
            check(res.issues.errors.isEmpty()) { res.issues.toString() }
            val asm = res.asm!!
            return asm[0]
        }
    }

    @Test
    fun process_PATH() {
        val goal = "PATH"
        val sentence = "/a/b/c"
        val proc = Agl.processorFromStringSimple(GrammarString(grammar)).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }
        val res = proc.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString() )
        println(res.asm!!.asString())
    }

    @Test
    fun process_unit() {
        val sentence = "a : /a/b/c ;"
        val proc = Agl.processorFromStringSimple(GrammarString(grammar)).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }
        val res = proc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString() )
        println(res.asm!!.asString())
    }
}