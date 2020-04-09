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

package net.akehurst.language.examples.simple

import net.akehurst.language.agl.processor.FormatterAbstract
import net.akehurst.language.api.processor.LanguageProcessorException


class SimpleExampleFormatter : FormatterAbstract() {
    companion object {
        val EOL: String = "\n"
    }

    override fun <T> format(asm: T): String {
        return if (null == asm) {
            throw LanguageProcessorException("Cannot format null value", null)
        } else {
            when (asm) {
                is SimpleExampleUnit -> this.format("", asm)
                else -> throw LanguageProcessorException("Cannot format ${asm}", null)
            }
        }
    }

    fun format(indent: String, asm: SimpleExampleUnit): String {
        return asm.definition.map {
            format(indent, it)
        }.joinToString(separator = EOL)
    }

    fun format(indent: String, asm: Definition): String {
        return when (asm) {
            is ClassDefinition -> format(indent, asm)
            else -> throw RuntimeException("Unknown subtype of Definition")
        }
    }

    fun format(indent: String, asm: ClassDefinition): String {
        val propertyDefinitionList = asm.properties.map {
            indent + "  " + format("", it)
        }.joinToString(
                separator = "\n"
        )
        val methodDefinitionList = asm.methods.map {
            indent + "  " + format(indent + "  ", it)
        }.joinToString(
                separator = "\n"
        )
        return "${indent}class ${asm.name} {\n" +
                "${indent}${propertyDefinitionList}\n" +
                "${indent}${methodDefinitionList}\n" +
                "}"
    }

    fun format(indent: String, asm: PropertyDefinition): String {
        return "${asm.name} : ${asm.typeName}"
    }

    fun format(indent: String, asm: MethodDefinition): String {
        val paramList = asm.paramList.map {
            format("", it)
        }.joinToString(", ")
        val body = "{\n${indent}}"
        return "${asm.name}($paramList) ${body}"
    }

    fun format(indent: String, asm: ParameterDefinition): String {
        return "${asm.name}: ${asm.typeName}"
    }
}
