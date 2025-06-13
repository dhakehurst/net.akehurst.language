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

package net.akehurst.language.agl.format.builder

import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmPathSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.AsmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.formatter.api.FormatNamespace
import net.akehurst.language.sppt.api.ParsePath

@DslMarker
annotation class FormatModelDslMarker

fun formatModel(name:String, init: FormatModelBuilder.() -> Unit): AglFormatModel {
    val b = FormatModelBuilder(SimpleName(name))
    b.init()
    return b.build()
}

@FormatModelDslMarker
class FormatModelBuilder(
    val name:SimpleName
) {

    private val _namespaces = mutableListOf<FormatNamespace>()
    private val _options = mutableMapOf<String,String>()
    private val _model = AglFormatModelDefault(name, OptionHolderDefault(null, _options), _namespaces)

    private val _asm = AsmSimple()
    private val _ruleList = mutableListOf<AsmStructure>()
    private val rules = _asm.createStructure("/", QualifiedName("Unit")).also {
        _asm.addRoot(it)
        it.setProperty(PropertyValueName("ruleList"), AsmListSimple(_ruleList), 0)//TODO childIndex
    }

    fun rule(forTypeName: String, init: FormatExpressionBuilder.() -> Unit) {
        val b = FormatExpressionBuilder(_asm)
        b.init()
        val expr = b.build()
        val formatRuleElement = _asm.createStructure("/",QualifiedName("FormatRule"))
        val typeReference = _asm.createStructure("/", QualifiedName("TypeReference"))
        typeReference.setProperty(PropertyValueName("identifier"), AsmPrimitiveSimple.stdString(forTypeName), 0)//TODO childIndex
        formatRuleElement.setProperty(PropertyValueName("typeReference"), typeReference, 0)//TODO childIndex
        formatRuleElement.setProperty(PropertyValueName("formatExpression"), expr, 0)//TODO childIndex
        _ruleList.add(formatRuleElement)
    }

    fun build(): AglFormatModel = _model
}

@FormatModelDslMarker
class FormatExpressionBuilder(
    private val _asm: AsmSimple
) {

    private lateinit var _exp: AsmStructure

    fun literalString(value: String) {
        val el = _asm.createStructure(
            parsePath = "/",
            typeName = QualifiedName("LiteralString")
        )
        el.setProperty(PropertyValueName("literal_string"), AsmPrimitiveSimple.stdString(value), 0)//TODO childIndex
        _exp = el
    }

    fun build(): AsmStructure = _exp
}