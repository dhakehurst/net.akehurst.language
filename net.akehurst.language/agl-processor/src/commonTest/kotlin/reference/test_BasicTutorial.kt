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
import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.default_.ContextAsmDefault
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @defaultGoalRule: document
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
namespace net.akehurst.language.example.BasicTutorial {
    identify TargetDef by name   
    references {
        in Greeting {
            forall greetingTargetList of-type TargetRef {
                property name refers-to TargetDef
            }
        }
    }
}
        """.trimIndent()

        val processor = Agl.processorFromStringDefault(
            grammarDefinitionStr = GrammarString(grammarStr),
            crossReferenceModelStr = CrossReferenceString(referencesStr)
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
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

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(ContextAsmDefault()) } })

        assertTrue(result.issues.isEmpty(), result.issues.toString())
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

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(ContextAsmDefault()) } })

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(64, 7, 6, 3),
                "No target of type(s) [TargetDef] found for referring value 'Ann' in scope of element ':TargetRef[/0/greeting/2/greetingTargetList/0]'"
            )
        )

        assertEquals(expIssues, result.issues.all)
    }
}