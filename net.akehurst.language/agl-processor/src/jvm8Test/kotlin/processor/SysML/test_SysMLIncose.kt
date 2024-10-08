/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.test.processor.SysML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SysMLIncose {

    private companion object {
        const val grammarPath = "/_private/arch/grammar.agl"
        val grammarStr = this::class.java.getResource(grammarPath).readText()
        var processor: LanguageProcessor<Asm, ContextAsmSimple> = Agl.processorFromStringSimple(GrammarString(grammarStr)).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

    }

    @Test
    fun check_grammar() {
        val grammarStr = this::class.java.getResource(grammarPath).readText()
        val res = Agl.registry.agl.grammar.processor!!.process(
            grammarStr,
            Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun parse_grammar() {
        val grammarStr = this::class.java.getResource(grammarPath).readText()
        val result = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
    }

    @Test
    fun process_grammar() {
        val grammarStr = this::class.java.getResource(grammarPath).readText()
        val result = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun parse_SINGLE_LINE_COMMENT() {
        val goal = "statements"
        val sentence = """
          // a comment
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(sentence, result.sppt!!.asSentence)
    }


    @Test
    fun parse_single_statement() {
        val goal = "statements"
        val sentence = """
          The term AA battery is an item definition.
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun parse_two_statements() {
        val goal = "statements"
        val sentence = """
            The term AA battery is an item definition.
            The term Radio Wave is an item definition.
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(sentence, result.sppt!!.asSentence)

        println(result.sppt!!.toStringAll)
    }

    @Test
    fun parse_multiple_statements() {
        val goal = "statements"
        val sentence = """
// Entity Type Meta definition
The meta term User is an entity definition.
The meta term System of Interest is an entity definition.

// Entity definition
The term Resident is a User definition.
The term Smart Radiator Thermostat is a System of Interest definition.

// Item definition
The term AA battery is an item definition.
The term Radio Wave is an item definition.

// Value definition
The term Boolean is a value definition.
The term Millimeter is a value definition for values with unit mm.
The term Electric Potential is a value definition.
The term Electric Potential is a value definition for values with unit V.
The term LengthValue is a value definition.
The term Custom is a value definition for values with unit c.

// Constant Value
The constant value min valve position is equal to 0 mm.
The constant value max valve position is equal to 100 mm.

// Characteristic Definition
The term Accuracy is a characteristic definition.
The term Width is a characteristic definition for LengthValue values.

// Port definition
The term M30 x 1_5 is a port definition.
The term AA battery slot is a port definition.

// Port Inputs
The port definition Wi-Fi Radio Wave has the input received wave receiving Radio Wave items.
The port definition M30 x 1_5 has the input valve position receiving Millimeter values.

// Port Output
The port definition ADC Temperature Hardware Software Interface (HSI) has the output raw bits providing bit values with multiplicity 10.
The port definition Wi-Fi Hardware Software Interface (HSI) has the output received data providing byte values with multiplicity 0..*.

// Port
The System of Interest definition Smart Radiator Thermostat has the required port Connection defined by M30 x 1_5.

// Entity
The System of Interest definition Smart Radiator Thermostat has the entity battery compartment defined by Battery Compartment.

// Function
The System of Interest definition Smart Radiator Thermostat has the function control radiator.
The mechanic part definition Valve Gear has the function adjust valve position.

// Function Input
The function Valve Gear.adjust valve position has the input motor torque receiving Torque values.
The function adjust valve position has the input motor torque receiving Torque values.

// Function Output
The function Valve Gear.adjust valve position has the output valve position providing millimeter values.

// Perform Action
The function Smart Radiator Thermostat.control radiator performs the action adjust valve position referencing function Smart Radiator Thermostat.valve Gear.adjust valve position.

// Binding
The Mechanic Part definition Valve Gear binds port output motor shaft.torque and action output adjust valve position.motor torque.
The System of Interest definition Smart Radiator Thermostat binds port output control radiator.valve position and action output control radiator.adjust valve position.valve position.

// Flow
The System of Interest definition Smart Radiator Thermostat has exactly one flow from action output control radiator.rotate shaft.torque to action input control radiator.adjust valve position.motor torque.

// Allocation
The System of Interest definition Smart Radiator Thermostat allocates port matter to port wi-fi waves.

// State
The System of Interest definition Smart Radiator Thermostat has the state unpowered.
The System of Interest definition Smart Radiator Thermostat has the state powered.
The System of Interest definition Smart Radiator Thermostat has the state powered.standby.

// Transition
The System of Interest definition Smart Radiator Thermostat has a transition from state unpowered to state powered accepted when aa battery.battery.direct current (DC).Voltage at the Common Collector (VCC).voltage is greater than or equal to 1.2 V.

// Function Performed in State
The System of Interest definition Smart Radiator Thermostat performs function control radiator while in state powered.
        """.trimIndent()
        val result = processor.process(sentence)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)

        println(result.issues.toString())
        println(result.asm!!.asString())
    }


}