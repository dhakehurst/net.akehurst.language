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

package net.akehurst.language.agl.formatter.builder

import net.akehurst.language.agl.asm.AsmListSimple
import net.akehurst.language.agl.asm.AsmPathSimple
import net.akehurst.language.agl.asm.AsmPrimitiveSimple
import net.akehurst.language.agl.asm.AsmSimple
import net.akehurst.language.agl.language.format.AglFormatterModelFromAsm
import net.akehurst.language.api.asm.AsmStructure
import net.akehurst.language.formatter.api.AglFormatterModel

@DslMarker
annotation class FormatModelDslMarker

fun formatModel(init: FormatModelBuilder.() -> Unit): AglFormatterModel {
    val b = FormatModelBuilder()
    b.init()
    return b.build()
}

@FormatModelDslMarker
class FormatModelBuilder(
) {

    private val _asm = AsmSimple()
    private val _ruleList = mutableListOf<AsmStructure>()
    private val rules = _asm.createStructure(AsmPathSimple.ROOT, "Unit").also {
        _asm.addRoot(it)
        it.setProperty("ruleList", AsmListSimple(_ruleList), 0)//TODO childIndex
    }

    fun rule(forTypeName: String, init: FormatExpressionBuilder.() -> Unit) {
        val b = FormatExpressionBuilder(_asm)
        b.init()
        val expr = b.build()
        val formatRuleElement = _asm.createStructure(AsmPathSimple.ROOT, "FormatRule")
        val typeReference = _asm.createStructure(AsmPathSimple.ROOT, "TypeReference")
        typeReference.setProperty("identifier", AsmPrimitiveSimple.stdString(forTypeName), 0)//TODO childIndex
        formatRuleElement.setProperty("typeReference", typeReference, 0)//TODO childIndex
        formatRuleElement.setProperty("formatExpression", expr, 0)//TODO childIndex
        _ruleList.add(formatRuleElement)
    }

    fun build(): AglFormatterModel {
        return AglFormatterModelFromAsm(_asm)
    }
}

@FormatModelDslMarker
class FormatExpressionBuilder(
    private val _asm: AsmSimple
) {

    private lateinit var _exp: AsmStructure

    fun literalString(value: String) {
        val el = _asm.createStructure(
            asmPath = AsmPathSimple.ROOT,
            typeName = "LiteralString"
        )
        el.setProperty("literal_string", AsmPrimitiveSimple.stdString(value), 0)//TODO childIndex
        _exp = el
    }

    fun build(): AsmStructure = _exp
}