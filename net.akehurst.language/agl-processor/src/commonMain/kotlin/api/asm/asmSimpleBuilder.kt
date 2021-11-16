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
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
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
    private val _scopeMap = mutableMapOf<AsmElementPath, ScopeSimple<AsmElementPath>>()

    fun root(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val path = AsmElementPath.ROOT + (_asm.rootElements.size).toString()
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, path, typeName, true, _context?.rootScope)
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
    private val _scopeMap: MutableMap<AsmElementPath, ScopeSimple<AsmElementPath>>,
    private val _asm: AsmSimple,
    _asmPath: AsmElementPath,
    _typeName: String,
    _isRoot: Boolean,
    _parentScope: ScopeSimple<AsmElementPath>?
) {
    private val _element = _asm.createElement(_asmPath, _typeName).also {
        if (_isRoot) _asm.addRoot(it)
    }
    private val _elementScope by lazy {
        _parentScope?.let {
            if (_scopeModel.isScopeDefinition(_element.typeName)) {
                val refInParent = _scopeModel.createReferenceLocalToScope(_parentScope, _element)
                    ?: error("Trying to create child scope but cannot create a reference for $_element")
                val newScope = _parentScope.createOrGetChildScope(refInParent, _element.typeName)
                _scopeMap[_asmPath] = newScope
                newScope
            } else {
                _parentScope
            }
        }
    }

    private fun _property(name: String, value: Any?) {
        _element.setProperty(name, value, false)
    }

    fun propertyUnnamedString(value: String?) = this._property(TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, value)
    fun propertyString(name: String, value: String?) = this._property(name, value)
    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = propertyElement(name, name, init)
    fun propertyElement(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val newPath = _element.asmPath + name
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, typeName, false, _elementScope)
        b.init()
        val el = b.build()
        this._element.setProperty(name, el, false)
        return el
    }

    fun propertyUnnamedListOfString(list: List<String>) = this._property(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_VALUE, list)
    fun propertyListOfString(name: String, list: List<String>) = this._property(name, list)
    fun propertyListOfElement(name: String, init: ListAsmElementSimpleBuilder.() -> Unit): List<Any> {
        val newPath = _element.asmPath + name
        val b = ListAsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, _elementScope)
        b.init()
        val list = b.build()
        this._element.setProperty(name, list, false)
        return list
    }

    fun reference(name: String, elementReference: String) {
        _element.setProperty(name, elementReference, true)
    }

    fun build(): AsmElementSimple {
        val es = _elementScope
        if (null == es) {
            //do nothing
        } else {
            val scopeFor = es.forTypeName
            val referablePropertyName = _scopeModel.getReferablePropertyNameFor(scopeFor, _element.typeName)
            val referableName = referablePropertyName?.let { _element.getPropertyAsString(it) }
            if (null != referableName) {
                es.addToScope(referableName, _element.typeName, _element.asmPath)
            }
        }
        return _element
    }
}

@AsmSimpleBuilderMarker
class ListAsmElementSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _scopeMap: MutableMap<AsmElementPath, ScopeSimple<AsmElementPath>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmElementPath,
    private val _parentScope: ScopeSimple<AsmElementPath>?
) {

    private val _list = mutableListOf<Any>()

    fun string(value: String){
        _list.add(value)
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit) {
        val newPath = _asmPath + (_list.size).toString()
        val b = ListAsmElementSimpleBuilder(_scopeModel, _scopeMap, _asm, newPath, _parentScope)
        b.init()
        val list = b.build()
        _list.add(list)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val newPath = _asmPath + (_list.size).toString()
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, typeName, false, _parentScope)
        b.init()
        val el = b.build()
        _list.add(el)
        return el
    }

    fun build(): List<Any> {
        return this._list
    }
}