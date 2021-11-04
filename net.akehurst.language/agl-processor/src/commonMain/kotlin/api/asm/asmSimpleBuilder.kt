/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.asm

import net.akehurst.language.agl.grammar.scopes.ScopeModel
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.createReferenceLocalToScope
import net.akehurst.language.agl.syntaxAnalyser.resolveReferencesElement

@DslMarker
annotation class AsmSimpleBuilderMarker

fun asmSimple(scopeModel: ScopeModel = ScopeModel(), context: ContextSimple? = null, init: AsmSimpleBuilder.() -> Unit): AsmSimple {
    val b = AsmSimpleBuilder(scopeModel, context)
    b.init()
    return b.build()
}

@AsmSimpleBuilderMarker
class AsmSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _context: ContextSimple?
) {

    private val _asm = AsmSimple()
    private val _scopeMap = mutableMapOf<AsmElementSimple, ScopeSimple<AsmElementPath>>()

    fun root(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, AsmElementPath("/"), typeName, true, _context?.rootScope)
        b.init()
        return b.build()
    }

    fun build(): AsmSimple {
        _asm.rootElements.forEach { el ->
            _scopeModel.resolveReferencesElement(el, emptyMap(), _context?.rootScope, _scopeMap)
        }
        return _asm
    }
}

@AsmSimpleBuilderMarker
class AsmElementSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _scopeMap: MutableMap<AsmElementSimple, ScopeSimple<AsmElementPath>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmElementPath,
    _typeName: String,
    _isRoot: Boolean,
    private val _scope: ScopeSimple<AsmElementPath>?
) {
    private val _element = if (_isRoot) _asm.createRootElement(_typeName) else _asm.createNonRootElement(_asmPath, _typeName)

    private fun _property(name: String, value: Any?) {
        _element.setProperty(name, value, false)
    }

    fun propertyString(name: String, value: String?) = this._property(name, value)
    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = propertyElement(name, name, init)
    fun propertyElement(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val childScope = _scope?.let {
            if (_scopeModel.isScopeDefinition(_element.typeName)) {
                val refInParent = _scopeModel.createReferenceLocalToScope(_scope, _element) ?: error("Trying to create child scope but cannot create a reference for $_element")
                val newScope = _scope.createOrGetChildScope(refInParent, _element.typeName)
                _scopeMap[_element] = newScope
                newScope
            } else {
                _scope
            }
        }
        val newPath = _element.asmPath + name
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, typeName, false, childScope)
        b.init()
        val el = b.build()
        this._element.setProperty(name, el, false)
        return el
    }

    fun propertyListOfString(name: String, list: List<String>) = this._property(name, list)
    fun propertyListOfElement(name: String, init: ListAsmElementSimpleBuilder.() -> Unit): List<AsmElementSimple> {
        val b = ListAsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, _asmPath, _scope)
        b.init()
        val list = b.build()
        this._element.setProperty(name, list, false)
        return list
    }

    fun reference(name: String, elementReference: String) {
        _element.setProperty(name, elementReference, true)
    }

    fun build(): AsmElementSimple {
        if (null == _scope) {
            //do nothing
        } else {
            val scopeFor = _scope.forTypeName
            val referablePropertyName = _scopeModel.getReferablePropertyNameFor(scopeFor, _element.typeName)
            val referableName = referablePropertyName?.let { _element.getPropertyAsString(it) }
            if (null != referableName) {
                _scope.addToScope(referableName, _element.typeName, _element.asmPath)
            }
        }
        return _element
    }
}

@AsmSimpleBuilderMarker
class ListAsmElementSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _scopeMap: MutableMap<AsmElementSimple, ScopeSimple<AsmElementPath>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmElementPath,
    private val _scope: ScopeSimple<AsmElementPath>?
) {

    private val _list = mutableListOf<AsmElementSimple>()

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val newPath = _asmPath + (_list.size).toString()
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, typeName, false, _scope)
        b.init()
        val el = b.build()
        _list.add(el)
        return el
    }

    fun build(): List<AsmElementSimple> {
        return this._list
    }
}