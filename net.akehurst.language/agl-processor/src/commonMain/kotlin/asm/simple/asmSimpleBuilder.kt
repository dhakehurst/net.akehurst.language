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

package net.akehurst.language.asm.simple

import net.akehurst.language.agl.simple.*
import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.asmValue
import net.akehurst.language.expressions.processor.toTypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.scope.simple.ScopeSimple
import net.akehurst.language.typemodel.api.TupleType
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeNamespace
import net.akehurst.language.typemodel.api.UnnamedSupertypeType
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.typeModel

@DslMarker
annotation class AsmSimpleBuilderMarker

fun asmSimple(
    typeModel: TypeModel = typeModel("StdLib", false) {},
    defaultNamespace: QualifiedName = SimpleTypeModelStdLib.qualifiedName,
    crossReferenceModel: CrossReferenceModel = CrossReferenceModelDefault(),
    context: ContextAsmSimple? = null,
    /** need to pass in a context if you want to resolveReferences */
    resolveReferences: Boolean = true,
    failIfIssues: Boolean = true,
    init: AsmSimpleBuilder.() -> Unit
): Asm {
    val defNs = typeModel.findNamespaceOrNull(defaultNamespace) ?: SimpleTypeModelStdLib
    val b = AsmSimpleBuilder(typeModel, defNs, crossReferenceModel, context, resolveReferences, failIfIssues)
    b.init()
    return b.build()
}

@AsmSimpleBuilderMarker
class AsmSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _defaultNamespace: TypeNamespace,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _context: ContextAsmSimple?,
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
        val b = AsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _scopeMap, this._asm, path, typeName, true, _context?.rootScope)
        b.init()
        return b.build()
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        element(TupleType.NAME.value, init)

    fun listOfString(vararg items: String): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val l = items.asList().map { AsmPrimitiveSimple.stdString(it) }
        val list = AsmListSimple(l)
        _asm.addRoot(list)
        return list
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val b = ListAsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _scopeMap, this._asm, path, _context?.rootScope)
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
            _asm.traverseDepthFirst(ReferenceResolverSimple(_typeModel, _crossReferenceModel, _context.rootScope, resolveFunction, emptyMap(), issues))
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
    private val _defaultNamespace: TypeNamespace,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _scopeMap: MutableMap<AsmPath, ScopeSimple<AsmPath>>,
    private val _asm: AsmSimple,
    _asmPath: AsmPath,
    _typeName: String,
    _isRoot: Boolean,
    _parentScope: ScopeSimple<AsmPath>?
) {
    private val _elementQualifiedTypeName: QualifiedName = _typeName.let {
        val qtn = it.asPossiblyQualifiedName
        when (qtn) {
            is QualifiedName -> {
                _typeModel.findByQualifiedNameOrNull(qtn)?.qualifiedName
                    ?: error("Type not found '${qtn.value}'")
            }

            is SimpleName -> {
                when (qtn) {
                    TupleType.NAME -> TupleType.NAME
                    UnnamedSupertypeType.NAME -> UnnamedSupertypeType.NAME
                    else -> _typeModel.findFirstByPossiblyQualifiedOrNull(qtn)?.qualifiedName
                        ?: _defaultNamespace.qualifiedName.append(SimpleName(_typeName))
                }
            }

            else -> error("Unsupported")
        }
    }
    private val _element = _asm.createStructure(_asmPath, _elementQualifiedTypeName).also {
        if (_isRoot) _asm.addRoot(it)
    }
    private val _elementScope by lazy {
        _parentScope?.let {
            if (_crossReferenceModel.isScopeDefinedFor(_element.typeName)) {
                val expr = (_crossReferenceModel as CrossReferenceModelDefault).identifyingExpressionFor(_parentScope.forTypeName.last, _element.qualifiedTypeName)
                val refInParent = expr?.createReferenceLocalToScope(_parentScope, _element)
                    ?: _element.typeName.value //error("Trying to create child scope but cannot create a reference for $_element")
                val newScope = _parentScope.createOrGetChildScope(refInParent, _element.qualifiedTypeName, _element.path)
                _scopeMap[_asmPath] = newScope
                newScope
            } else {
                _parentScope
            }
        }
    }
    private val _interpreter = ExpressionsInterpreterOverTypedObject(_typeModel)
    private fun Expression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is NavigationExpression -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val elType = _typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        val v = _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this)
        return when (v) {
            is AsmNothing -> null
            is AsmPrimitive -> v.value as String
            else -> TODO()
        }
    }

    private fun NavigationExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val elType = _typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        val res = _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this)
        return when (res) {
            is AsmNothing -> null
            is AsmPrimitive -> res.value as String
            else -> error("Evaluation of navigation '$this' on '$element' should result in a String, but it does not!")
        }
    }

    private fun _property(name: String, value: AsmValue) {
        _element.setProperty(PropertyValueName(name), value, 0)//TODO childIndex
    }

    fun propertyUnnamedString(value: String?) = this.propertyString(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, value)
    fun propertyString(name: String, value: String?) = this._property(name, value?.let { AsmPrimitiveSimple.stdString(it) } ?: AsmNothingSimple)
    fun propertyNothing(name: String) = this._property(name, AsmNothingSimple)
    fun propertyUnnamedElement(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        propertyElementExplicitType(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, typeName, init)

    fun propertyTuple(name: String, tupleTypeId:Int? = null, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front) ?: error("No namespace '${_elementQualifiedTypeName.front.value}'")
        val tt =  tupleTypeId?.let {
            ns.findTupleTypeWithIdOrNull(tupleTypeId)
        } ?: ns.createTupleType()
        return propertyElementExplicitType(name, tt.qualifiedName.value, init)
    }

    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure = propertyElementExplicitType(name, name, init)
    fun propertyElementExplicitType(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _element.path + name
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front)
            ?: _defaultNamespace
        val b = AsmElementSimpleBuilder(_typeModel, ns, _crossReferenceModel, _scopeMap, this._asm, newPath, typeName, false, _elementScope)
        b.init()
        val el = b.build()
        this._element.setProperty(PropertyValueName(name), el, 0)//TODO childIndex
        return el
    }

    fun propertyUnnamedListOfString(list: List<String>) = this.propertyListOfString(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, list)
    fun propertyListOfString(name: String, list: List<String>) = this._property(name, AsmListSimple(list.map { AsmPrimitiveSimple.stdString(it) }))
    fun propertyUnnamedListOfElement(init: ListAsmElementSimpleBuilder.() -> Unit) =
        this.propertyListOfElement(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, init)

    fun propertyListOfElement(name: String, init: ListAsmElementSimpleBuilder.() -> Unit): AsmList {
        val newPath = _element.path + name
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front)
            ?: _defaultNamespace
        val b = ListAsmElementSimpleBuilder(_typeModel, ns, _crossReferenceModel, _scopeMap, this._asm, newPath, _elementScope)
        b.init()
        val list = b.build()
        this._element.setProperty(PropertyValueName(name), list, 0)//TODO childIndex
        return list
    }

    fun reference(name: String, elementReference: String) {
        val ref = AsmReferenceSimple(elementReference, null)
        _element.setProperty(PropertyValueName(name), ref, 0)//TODO childIndex
    }

    fun build(): AsmStructure {
        val es = _elementScope
        if (null == es) {
            //do nothing
        } else {
            val scopeFor = es.forTypeName.last
            val nav = (_crossReferenceModel as CrossReferenceModelDefault).identifyingExpressionFor(scopeFor, _element.typeName)
            val elType = _typeModel.findByQualifiedNameOrNull(_element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
            val res = nav?.let { ExpressionsInterpreterOverTypedObject(_typeModel).evaluateExpression(EvaluationContext.ofSelf(_element.toTypedObject(elType)), it) }

            val referableName = when (res) {
                null -> null
                else -> when (res.asmValue) {
                    is AsmPrimitive -> (res.asmValue as AsmPrimitive).value as String
                    else -> error("Evaluation of navigation '$nav' on '$_element' should result in a String, but it is a '${res::class.simpleName}'")
                }
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
    private val _defaultNamespace: TypeNamespace,
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
        val b = ListAsmElementSimpleBuilder(_typeModel, _defaultNamespace, _scopeModel, _scopeMap, _asm, newPath, _parentScope)
        b.init()
        val list = b.build()
        _list.add(list)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _asmPath + (_list.size).toString()
        val b = AsmElementSimpleBuilder(_typeModel, _defaultNamespace, _scopeModel, _scopeMap, this._asm, newPath, typeName, false, _parentScope)
        b.init()
        val el = b.build()
        _list.add(el)
        return el
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val tt = SimpleTypeModelStdLib.TupleType
        return element(tt.qualifiedName.value, init)
    }

    fun build(): AsmList {
        return AsmListSimple(this._list)
    }
}