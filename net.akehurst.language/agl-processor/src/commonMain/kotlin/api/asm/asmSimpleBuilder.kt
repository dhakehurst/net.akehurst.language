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

import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.typeModel.TupleType

@DslMarker
annotation class AsmSimpleBuilderMarker

fun asmSimple(scopeModel: ScopeModelAgl = ScopeModelAgl(), context: ContextSimple? = null, resolveReferences: Boolean = true, init: AsmSimpleBuilder.() -> Unit): AsmSimple {
    val b = AsmSimpleBuilder(scopeModel, context, resolveReferences)
    b.init()
    return b.build()
}

@AsmSimpleBuilderMarker
class AsmSimpleBuilder(
    private val _scopeModel: ScopeModelAgl,
    private val _context: ContextSimple?,
    private val resolveReferences: Boolean
) {

    private val _asm = AsmSimple()
    private val _scopeMap = mutableMapOf<AsmElementPath, ScopeSimple<AsmElementPath>>()

    fun string(value: String) {
        _asm.addRoot(value)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val path = AsmElementPath.ROOT + (_asm.rootElements.size).toString()
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, path, typeName, true, _context?.rootScope)
        b.init()
        return b.build()
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit): List<Any> {
        val path = AsmElementPath.ROOT + (_asm.rootElements.size).toString()
        val b = ListAsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, path, _context?.rootScope)
        b.init()
        val list = b.build()
        _asm.addRoot(list)
        return list
    }

    private fun resolveReferences(issues: IssueHolder, o: Any?, locationMap: Map<Any, InputLocation>?, context: ScopeSimple<AsmElementPath>?) {
        when (o) {
            is AsmElementSimple -> _scopeModel.resolveReferencesElement(issues, o, locationMap, context?.rootScope)
            is List<*> -> o.forEach { resolveReferences(issues, it, locationMap, context) }
        }
    }

    fun build(): AsmSimple {
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (resolveReferences) {
            _asm.rootElements.forEach { el ->
                resolveReferences(issues, el, emptyMap(), _context?.rootScope)
            }
        }
        if (issues.all.isEmpty()) {
            return _asm
        } else {
            error("Issues building asm:\n${issues.all.joinToString(separator = "\n") { "$it" }}")
        }
    }
}

@AsmSimpleBuilderMarker
class AsmElementSimpleBuilder(
    private val _scopeModel: ScopeModelAgl,
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
                val newScope = _parentScope.createOrGetChildScope(refInParent, _element.typeName, _element.asmPath)
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

    fun propertyUnnamedString(value: String?) = this._property(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, value)
    fun propertyString(name: String, value: String?) = this._property(name, value)
    fun propertyNull(name: String) = this._property(name, null)
    fun propertyUnnamedElement(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple =
        propertyElementExplicitType(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, typeName, init)

    fun propertyTuple(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = propertyElementExplicitType(name, TupleType.INSTANCE_NAME, init)
    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = propertyElementExplicitType(name, name, init)
    fun propertyElementExplicitType(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val newPath = _element.asmPath + name
        val b = AsmElementSimpleBuilder(_scopeModel, _scopeMap, this._asm, newPath, typeName, false, _elementScope)
        b.init()
        val el = b.build()
        this._element.setProperty(name, el, false)
        return el
    }

    fun propertyUnnamedListOfString(list: List<String>) = this._property(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, list)
    fun propertyListOfString(name: String, list: List<String>) = this._property(name, list)
    fun propertyUnnamedListOfElement(init: ListAsmElementSimpleBuilder.() -> Unit) = this.propertyListOfElement(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, init)
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
            val referableName = referablePropertyName?.let { _element.getPropertyAsStringOrNull(it) }
            if (null != referableName) {
                es.addToScope(referableName, _element.typeName, _element.asmPath)
            }
        }
        return _element
    }
}

@AsmSimpleBuilderMarker
class ListAsmElementSimpleBuilder(
    private val _scopeModel: ScopeModelAgl,
    private val _scopeMap: MutableMap<AsmElementPath, ScopeSimple<AsmElementPath>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmElementPath,
    private val _parentScope: ScopeSimple<AsmElementPath>?
) {

    private val _list = mutableListOf<Any>()

    fun string(value: String) {
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

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = element(TupleType.INSTANCE_NAME, init)


    fun build(): List<Any> {
        return this._list
    }
}