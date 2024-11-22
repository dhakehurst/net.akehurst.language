package test

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import kotlin.test.Test
import kotlin.test.assertTrue

class test_Processor {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WHITESPACE = "\s+" ;
                skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
                skip leaf SINGLE_LINE_COMMENT = "//[\n\r]*?" ;
            
                value = predefined | object | literal ;
            
                predefined = IDENTIFIER ;
                object = '{' property* '}' ;
                property = IDENTIFIER ':' value ;
            
                literal = BOOLEAN | INTEGER | REAL | STRING ;
                
                leaf BOOLEAN = "true|false";
                leaf REAL = "[0-9]+[.][0-9]+";
                leaf STRING = "'([^'\\]|\\'|\\\\)*'";
                leaf INTEGER = "[0-9]+";
                leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
            }
        """.trimIndent()

        interface Value
        data class LiteralValue(val value: Any, val typeName: String) : Value
        data class ObjectValue(val properties: Map<String, Value>) : Value
        class PredefinedValue(val name: String) : Value {
            var value: Any? = null
            override fun toString(): String = "PredefinedValue($name=$value)"
        }

        class MySyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Value>() {
            override fun registerHandlers() {
                super.register(this::value)
                super.register(this::predefined)
                super.registerFor("object", this::object_)
                super.register(this::property)
                super.register(this::literal)
            }

            // value = object | literal | predefined ;
            private fun value(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Value {
                return children[0] as Value
            }

            // predefined = IDENTIFIER ;
            private fun predefined(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PredefinedValue {
                val name = children[0] as String
                return PredefinedValue(name)
            }

            // object = '{' property* '}' ;
            private fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectValue {
                val props = children[1] as List<Pair<String, Value>>?
                val properties = props?.associate { it } ?: emptyMap()
                return ObjectValue(properties)
            }

            // property = IDENTIFIER ':' value ;
            private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Value> {
                val name = children[0] as String
                val value = children[2] as Value
                return Pair(name, value)
            }

            // literal = BOOLEAN | INTEGER | REAL | STRING ;
            private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralValue {
                val valueStr = children[0] as String
                return when (nodeInfo.alt.option.value) {
                    0 -> LiteralValue(valueStr, "Boolean")
                    1 -> LiteralValue(valueStr, "Integer")
                    2 -> LiteralValue(valueStr, "Real")
                    3 -> LiteralValue(valueStr, "String")
                    else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
                }
            }
        }

        class MyContext(
            val predefined:Map<String, Value>
        ) : SentenceContext<Value>

        class MySemanticAnalyser : SemanticAnalyser<Value, MyContext> {
            val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

            override fun clear() { }

            override fun analyse(asm: Value, locationMap: Map<Any, InputLocation>?, context: MyContext?, options: SemanticAnalysisOptions<MyContext>): SemanticAnalysisResult {
                analyseValue(asm, context)
                return SemanticAnalysisResultDefault(issues)
            }

            private fun analyseValue(asm: Value, context: MyContext?) {
                when(asm) {
                    is LiteralValue -> Unit
                    is ObjectValue -> asm.properties.values.forEach { analyseValue(it, context) }
                    is PredefinedValue -> {
                        asm.value = context?.predefined?.get(asm.name)
                        if (null ==asm.value) {
                            issues.error(null, "Predefined value '${asm.name}' not found in context")
                        }
                    }
                }
            }
        }

        val processor:LanguageProcessor<Value,MyContext> = Agl.processorFromString<Value,MyContext>(grammarStr, Agl.configuration {
            syntaxAnalyserResolverResult { MySyntaxAnalyser() }
            semanticAnalyserResolverResult { MySemanticAnalyser() }
        }).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }
    }

    @Test
    fun process() {
        val sentences = listOf(
            "true", //BOOLEAN
            "1", //INTEGER
            "3.14", //REAL
            "'Hello World!'", // STRING
            "var1", // predefined
            "{}", // empty object
            "{ a:false b:1 c:3.141 d:'bob' e:var2 }", // object
            "{ f:{x:1 y:{a:3 b:7}} }" //nested objects
        )

        val context = MyContext(mapOf(
            "var1" to LiteralValue(true, "Boolean"),
            "var2" to LiteralValue(55, "Integer")
        ))
        for (sentence in sentences) {
            println(sentence)
            val pres = processor.process(sentence, Agl.options {
                semanticAnalysis { context(context) }
            })
            assertTrue(pres.issues.errors.isEmpty(), pres.issues.toString())
            println("  ${pres.asm}")
        }

    }

}