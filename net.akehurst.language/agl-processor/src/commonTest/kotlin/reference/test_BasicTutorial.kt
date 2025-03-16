/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.*

class test_BasicTutorial {

    companion object {
        val grammarStr = """
            // use this editor to enter the grammar for your language
            
            // each set of grammars must be defined in a namespace
            // one namespace is given for the whole file/grammar-definition
            namespace net.akehurst.language.example
            
            // multiple grammars can be defined,
            // the last grammar defined is the 'target grammar' - see other examples that illustrate this
            
            grammar BasicTutorial {
            
                #defaultGoalRule: document
                skip leaf WS = "\s+";
                skip leaf COMMENT = "//[^\r\n]*" ;
                document = targetDefList* greeting+ ;
                greeting = hello greetingTargetList '!' ;
                greetingTargetList = [targetRefOrWorld / ',']+ ;
                targetDefList = targetDef ;
                targetDef = 'target' NAME ;
                leaf hello = 'Hello' ;
                targetRefOrWorld = targetRef | 'World' ;
                targetRef = NAME ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val referencesStr = """
            namespace net.akehurst.language.example.BasicTutorial
                identify TargetDef by name   
                references {
                    in Greeting {
                        forall greetingTargetList of-type TargetRef {
                            property name refers-to TargetDef
                        }
                    }
                }
        """.trimIndent()

        val _processor = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            referenceStr = CrossReferenceString(referencesStr)
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }

        fun testPass(sentence: String) {
            val result = _processor.process(sentence, Agl.options { semanticAnalysis { context(ContextAsmSimple()) } })
            check(_processor.issues.errors.isEmpty()){ _processor.issues.toString()}
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
        }

        fun testFail(sentence: String, expectedIssues: Set<LanguageIssue>) {
            val result = _processor.process(sentence, Agl.options { semanticAnalysis { context(ContextAsmSimple()) } })
            assertTrue(_processor.issues.errors.isEmpty(), _processor.issues.toString())
            assertEquals(expectedIssues,result.issues.all)
            assertNull(result.asm)
        }
    }

    @Test
    fun noErrors() {
        val sentence = """
            target Julian
            target George
            
            Hello World !
            Hello Julian !
            Hello George !
        """.trimIndent()

        testPass(sentence)
    }

    @Test
    fun target_not_found() {
        val sentence = """
            target Julian
            target George
            
            Hello World !
            Hello Julian !
            Hello Ann !
            Hello George !
        """.trimIndent()

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(76, 7, 7, 6),
                "No target of type(s) [TargetDef] found for referring value 'Ann' in scope of element ':TargetRef[/0/greeting/2/greetingTargetList/0]'"
            )
        )
//FIXME: path different
        testFail(sentence, expIssues)
    }
}