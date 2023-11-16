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

package net.akehurst.language.api.asm

import net.akehurst.language.agl.agl.default.ScopeCreator
import net.akehurst.language.agl.asm.*
import net.akehurst.language.agl.default.GrammarTypeNamespaceFromGrammar
import net.akehurst.language.agl.default.ReferenceResolverDefault
import net.akehurst.language.agl.default.ResolveFunction
import net.akehurst.language.agl.language.expressions.ExpressionsInterpreterOverAsmSimple
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.agl.semanticAnalyser.ScopeSimple
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.language.reference.Scope
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import net.akehurst.language.typemodel.simple.TupleTypeSimple
import net.akehurst.language.typemodel.simple.UnnamedSupertypeTypeSimple

@DslMarker
annotation class AsmSimpleBuilderMarker

fun asmSimple(
    typeModel: TypeModel = typeModel("StdLib", false) {},
    crossReferenceModel: CrossReferenceModel = CrossReferenceModelDefault(),
    context: ContextSimple? = null,
    /** need to pass in a context if you want to resolveReferences */
    resolveReferences: Boolean = true,
    failIfIssues: Boolean = true,
    init: AsmSimpleBuilder.() -> Unit
): Asm {
    val b = AsmSimpleBuilder(typeModel, crossReferenceModel, context, resolveReferences, failIfIssues)
    b.init()
    return b.build()
}

@AsmSimpleBuilderMarker
class AsmSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _context: ContextSimple?,
    private val resolveReferences: Boolean,
    private val failIfIssues: Boolean
) {

    private val _asm = AsmSimple()
    private val _scopeMap = mutableMapOf<AsmPath, ScopeSimple<AsmPath>>()

    fun string(value: String) {
        _asm.addRoot(AsmPrimitiveSimple.stdString(value))
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val b = AsmElementSimpleBuilder(_typeModel, _crossReferenceModel, _scopeMap, this._asm, path, typeName, true, _context?.rootScope)
        b.init()
        return b.build()
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        element(TupleTypeSimple.NAME, init)

    fun listOfString(vararg items: String): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val l = items.asList().map { AsmPrimitiveSimple.stdString(it) }
        val list = AsmListSimple(l)
        _asm.addRoot(list)
        return list
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val b = ListAsmElementSimpleBuilder(_typeModel, _crossReferenceModel, _scopeMap, this._asm, path, _context?.rootScope)
        b.init()
        val list = b.build()
        _asm.addRoot(list)
        return list
    }

    fun build(): AsmSimple {
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (resolveReferences && null != _context) {
            val scopeCreator = ScopeCreator(_typeModel, _crossReferenceModel as CrossReferenceModelDefault, _context.rootScope, emptyMap(), issues)
            _asm.traverseDepthFirst(scopeCreator)

            val resolveFunction: ResolveFunction = { ref ->
                _asm.elementIndex[ref]
            }
            _asm.traverseDepthFirst(ReferenceResolverDefault(_typeModel, _crossReferenceModel, _context.rootScope, resolveFunction, emptyMap(), issues))
        }
        if (failIfIssues && issues.errors.isNotEmpty()) {
            error("Issues building asm:\n${issues.all.joinToString(separator = "\n") { "$it" }}")

        } else {
            return _asm
        }
    }
}

@AsmSimpleBuilderMarker
class AsmElementSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _scopeMap: MutableMap<AsmPath, ScopeSimple<AsmPath>>,
    private val _asm: AsmSimple,
    _asmPath: AsmPath,
    _typeName: String,
    _isRoot: Boolean,
    _parentScope: ScopeSimple<AsmPath>?
) {
    private val _elementQualifiedTypeName = _typeName.let {
        when (it) {
            TupleTypeSimple.NAME -> it
            UnnamedSupertypeTypeSimple.NAME -> it
            else -> _typeModel.findFirstByNameOrNull(_typeName)?.qualifiedName ?: _typeName //?: error("Cannot find type for name '$it'")
        }
    }
    private val _element = _asm.createStructure(_asmPath, _elementQualifiedTypeName).also {
        if (_isRoot) _asm.addRoot(it)
    }
    private val _elementScope by lazy {
        _parentScope?.let {
            if (_crossReferenceModel.isScopeDefinedFor(_element.typeName)) {
                val expr = (_crossReferenceModel as CrossReferenceModelDefault).identifyingExpressionFor(_parentScope.forTypeName, _element.qualifiedTypeName)
                val refInParent = expr?.createReferenceLocalToScope(_parentScope, _element)
                    ?: _element.typeName //error("Trying to create child scope but cannot create a reference for $_element")
                val newScope = _parentScope.createOrGetChildScope(refInParent, _element.typeName, _element.path)
                _scopeMap[_asmPath] = newScope
                newScope
            } else {
                _parentScope
            }
        }
    }
    private val _interpreter = ExpressionsInterpreterOverAsmSimple(_typeModel)
    private fun Expression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is Navigation -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val v = _interpreter.evaluateExpression(element, this)
        return when (v) {
            is AsmNothing -> null
            is AsmPrimitive -> v.value as String
            else -> TODO()
        }
    }

    private fun Navigation.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val res = _interpreter.evaluateExpression(element, this)
        return when (res) {
            is AsmNothing -> null
            is AsmPrimitive -> res.value as String
            else -> error("Evaluation of navigation '$this' on '$element' should result in a String, but it does not!")
        }
    }

    private fun _property(name: String, value: AsmValue) {
        _element.setProperty(name, value, 0)//TODO childIndex
    }

    fun propertyUnnamedString(value: String?) = this.propertyString(GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, value)
    fun propertyString(name: String, value: String?) = this._property(name, value?.let { AsmPrimitiveSimple.stdString(it) } ?: AsmNothingSimple)
    fun propertyNothing(name: String) = this._property(name, AsmNothingSimple)
    fun propertyUnnamedElement(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        propertyElementExplicitType(GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, typeName, init)

    fun propertyTuple(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure = propertyElementExplicitType(name, TupleTypeSimple.NAME, init)
    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure = propertyElementExplicitType(name, name, init)
    fun propertyElementExplicitType(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _element.path + name
        val b = AsmElementSimpleBuilder(_typeModel, _crossReferenceModel, _scopeMap, this._asm, newPath, typeName, false, _elementScope)
        b.init()
        val el = b.build()
        this._element.setProperty(name, el, 0)//TODO childIndex
        return el
    }

    fun propertyUnnamedListOfString(list: List<String>) = this.propertyListOfString(GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME, list)
    fun propertyListOfString(name: String, list: List<String>) = this._property(name, AsmListSimple(list.map { AsmPrimitiveSimple.stdString(it) }))
    fun propertyUnnamedListOfElement(init: ListAsmElementSimpleBuilder.() -> Unit) = this.propertyListOfElement(GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME, init)
    fun propertyListOfElement(name: String, init: ListAsmElementSimpleBuilder.() -> Unit): AsmList {
        val newPath = _element.path + name
        val b = ListAsmElementSimpleBuilder(_typeModel, _crossReferenceModel, _scopeMap, this._asm, newPath, _elementScope)
        b.init()
        val list = b.build()
        this._element.setProperty(name, list, 0)//TODO childIndex
        return list
    }

    fun reference(name: String, elementReference: String) {
        val ref = AsmReferenceSimple(elementReference, null)
        _element.setProperty(name, ref, 0)//TODO childIndex
    }

    fun build(): AsmStructure {
        val es = _elementScope
        if (null == es) {
            //do nothing
        } else {
            val scopeFor = es.forTypeName
            val nav = (_crossReferenceModel as CrossReferenceModelDefault).identifyingExpressionFor(scopeFor, _element.typeName) as Navigation?
            val res = nav?.let { ExpressionsInterpreterOverAsmSimple(_typeModel).evaluateExpression(_element, it) }
            val referableName = when (res) {
                null -> null
                is AsmPrimitive -> res.value as String
                else -> error("Evaluation of navigation '$nav' on '$_element' should result in a String, but it does not!")
            }
            if (null != referableName) {
//                es.addToScope(referableName, _element.typeName, _element.path)
            }
        }
        return _element
    }
}

@AsmSimpleBuilderMarker
class ListAsmElementSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _scopeModel: CrossReferenceModel,
    private val _scopeMap: MutableMap<AsmPath, ScopeSimple<AsmPath>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmPath,
    private val _parentScope: ScopeSimple<AsmPath>?
) {

    private val _list = mutableListOf<AsmValue>()

    fun string(value: String) {
        _list.add(AsmPrimitiveSimple.stdString(value))
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit) {
        val newPath = _asmPath + (_list.size).toString()
        val b = ListAsmElementSimpleBuilder(_typeModel, _scopeModel, _scopeMap, _asm, newPath, _parentScope)
        b.init()
        val list = b.build()
        _list.add(list)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _asmPath + (_list.size).toString()
        val b = AsmElementSimpleBuilder(_typeModel, _scopeModel, _scopeMap, this._asm, newPath, typeName, false, _parentScope)
        b.init()
        val el = b.build()
        _list.add(el)
        return el
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure = element(TupleTypeSimple.NAME, init)

    fun build(): AsmList {
        return AsmListSimple(this._list)
    }
}