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

import net.akehurst.language.agl.agl.grammar.scopes.ScopeModel
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.resolveReferencesElement

fun asmSimple(scopeModel: ScopeModel = ScopeModel(), context: ContextSimple? = null, init: AsmSimpleBuilder.() -> Unit): AsmSimple {
    val b = AsmSimpleBuilder(scopeModel, context)
    b.init()
    return b.build()
}

class AsmSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _context: ContextSimple?
) {

    private val _asm = AsmSimple()

    fun root(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val b = AsmElementSimpleBuilder(_scopeModel, this._asm, typeName, true, _context?.scope)
        b.init()
        return b.build()
    }

    fun build(): AsmSimple {
        _asm.rootElements.forEach { el ->
            _scopeModel.resolveReferencesElement(el, emptyMap(),_context?.scope)
        }
        return _asm
    }
}

class AsmElementSimpleBuilder(
    private val _scopeModel: ScopeModel,
    private val _asm: AsmSimple,
    typeName: String,
    isRoot: Boolean,
    private val _scope: ScopeSimple<AsmElementSimple>?
) {
    private val _element = if (isRoot) _asm.createRootElement(typeName) else _asm.createNonRootElement(typeName)

    private fun _property(name: String, value: Any?) {
        _element.setProperty(name, value, false)
    }

    fun property(name: String, value: String?) = this._property(name, value)
    fun property(name: String, list: List<*>) = this._property(name, list) //TODO: should only be List<String> or List<AsmElementSimple>
    fun property(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = property(name, name, init)
    fun property(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val childScope = _scope?.let {
            if (_scopeModel.isScope(_element.typeName)) {
                val newScope = _scope.childScope(_element.typeName)
                _element.ownScope = newScope
                newScope
            } else {
                _scope
            }
        }
        val b = AsmElementSimpleBuilder(_scopeModel, this._asm, typeName, false,childScope)
        b.init()
        val el = b.build()
        this._element.setProperty(name, el, false)
        return el
    }

    fun reference(name: String, elementReference: String) {
        _element.setProperty(name, elementReference, true)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val childScope = _scope?.let {
            if (_scopeModel.isScope(_element.typeName)) {
                val newScope = _scope.childScope(_element.typeName)
                _element.ownScope = newScope
                newScope
            } else {
                _scope
            }
        }
        val b = AsmElementSimpleBuilder(_scopeModel, this._asm, typeName, false,childScope)
        b.init()
        return b.build()
    }

    fun build(): AsmElementSimple {
        if (null == _scope) {
            //do nothing
        } else {
            val scopeFor = _scope.forTypeName
            val referablePropertyName = _scopeModel.getReferablePropertyNameFor(scopeFor, _element.typeName)
            val referableName = referablePropertyName?.let { _element.getPropertyAsString(it) }
            if (null != referableName) {
                _scope.addToScope(referableName, _element.typeName, _element)
            }
        }
        return _element
    }
}