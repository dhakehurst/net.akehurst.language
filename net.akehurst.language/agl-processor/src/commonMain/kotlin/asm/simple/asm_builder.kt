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

package net.akehurst.language.asm.builder

import net.akehurst.language.agl.simple.*
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.*
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ObjectGraphAsmSimple
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sppt.api.ParsePath
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeNamespace
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.builder.typeModel

@DslMarker
annotation class AsmSimpleBuilderMarker

fun asmSimple(
    typeModel: TypeModel = typeModel("StdLib", false) {},
    defaultNamespace: QualifiedName = StdLibDefault.qualifiedName,
    crossReferenceModel: CrossReferenceModel = CrossReferenceModelDefault(SimpleName("CrossReference")),
    sentenceId:Any? = null,
    context: ContextWithScope<Any, Any>? = null,
    /** need to pass in a context if you want to resolveReferences */
    resolveReferences: Boolean = true,
    failIfIssues: Boolean = true,
    resolvedReferences: MutableList<ResolvedReference> = mutableListOf(),
    init: AsmSimpleBuilder.() -> Unit
): Asm {
    val defNs = typeModel.findNamespaceOrNull(defaultNamespace) ?: StdLibDefault
    val b = AsmSimpleBuilder(typeModel, defNs, crossReferenceModel, sentenceId, context, resolveReferences, failIfIssues,resolvedReferences)
    b.init()
    return b.build()
}

@AsmSimpleBuilderMarker
class AsmSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _defaultNamespace: TypeNamespace,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _sentenceId:Any?,
    private val _context: ContextWithScope<Any, Any>?,
    private val resolveReferences: Boolean,
    private val failIfIssues: Boolean,
    private val resolvedReferences: MutableList<ResolvedReference>
) {
    private val _sentenceScope = _context?.getScopeForSentenceOrNull(null) as ScopeSimple? //TODO
    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAsmSimple(_typeModel, _issues),_issues)
    private val _asm = AsmSimple()
    private val _scopeMap = mutableMapOf<AsmPath, ScopeSimple<Any>>()
    private val _identifyingValueInFor = { inTypeName: SimpleName, item: AsmStructure ->
        SemanticAnalyserSimple.identifyingValueInFor(_interpreter, _crossReferenceModel, inTypeName, item)
    }

    fun string(value: String) {
        _asm.addRoot(AsmPrimitiveSimple.stdString(value))
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val b = AsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _context, _scopeMap, this._asm, _identifyingValueInFor, path, typeName, true, _sentenceScope)
        b.init()
        return b.build()
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        element(StdLibDefault.TupleType.qualifiedName.value, init)

    fun listOfString(vararg items: String): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val l = items.asList().map { AsmPrimitiveSimple.stdString(it) }
        val list = AsmListSimple(l)
        _asm.addRoot(list)
        return list
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit): AsmList {
        val path = AsmPathSimple.ROOT + (_asm.root.size).toString()
        val b = ListAsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _context, _scopeMap, this._asm, path, _sentenceScope,_identifyingValueInFor)
        b.init()
        val list = b.build()
        _asm.addRoot(list)
        return list
    }

    fun build(): AsmSimple {
        if (resolveReferences && null != _context && null!=_sentenceScope) {
            // Build Scope
            val scopeCreator = ScopeCreator(
                _typeModel,
                _crossReferenceModel as CrossReferenceModelDefault,
                _context,
                _sentenceId,
                false, LanguageIssueKind.ERROR,
                _identifyingValueInFor,
                LocationMapDefault(),
                _issues
            )
            _asm.traverseDepthFirst(scopeCreator)

            // resolve refs
            val resolver = ReferenceResolverSimple(
                _typeModel,
                _crossReferenceModel,
                _context,
                _sentenceId,
                _identifyingValueInFor,
                _context.resolveScopedItem,
                LocationMapDefault(),
                _issues
            )
            _asm.traverseDepthFirst(resolver)
            resolvedReferences.addAll(resolver.resolvedReferences)
        }
        if (failIfIssues && _issues.errors.isNotEmpty()) {
            error("Issues building asm:\n${_issues.all.joinToString(separator = "\n") { "$it" }}")

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
    private val _context: ContextWithScope<Any, Any>?,
    private val _scopeMap: MutableMap<AsmPath, ScopeSimple<Any>>,
    private val _asm: AsmSimple,
    private val _identifyingValueInFor: (inTypeName:SimpleName, item:AsmStructure) -> Any?,
    _asmPath: AsmPath,
    _typeName: String,
    _isRoot: Boolean,
    _parentScope: ScopeSimple<Any>?
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
                    StdLibDefault.TupleType.name -> StdLibDefault.TupleType.qualifiedName
                    //UnionType.NAME -> UnionType.NAME
                    else -> _typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(qtn)?.qualifiedName
                        ?: _defaultNamespace.qualifiedName.append(SimpleName(_typeName))
                }
            }
        }
    }
    private val _element = _asm.createStructure("/",_elementQualifiedTypeName).also {
        it.semanticPath = _asmPath
        if (_isRoot) _asm.addRoot(it)
    }
    private val _elementScope by lazy {
        _parentScope?.let {
            if (null!=_context && _crossReferenceModel.isScopeDefinedFor(_element.typeName)) {
                val refInParent = _identifyingValueInFor(_parentScope.forTypeName.last, _element) as String?
                    ?: _element.typeName.value
                val newScope = _parentScope.createOrGetChildScope(refInParent, _element.qualifiedTypeName)
                _scopeMap[_asmPath] = newScope
                newScope
            } else {
                _parentScope
            }
        }
    }

    /*
    private fun Expression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is NavigationExpression -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val elType = _typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        val v = _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this)
        return when (v.asmValue) {
            is AsmNothing -> null
            is AsmPrimitive -> (v.asmValue as AsmPrimitive).value as String
            else -> TODO()
        }
    }

    private fun NavigationExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): String? {
        val elType = _typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        val res = _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this)
        return when (res.asmValue) {
            is AsmNothing -> null
            is AsmPrimitive -> (res.asmValue as AsmPrimitive).value as String
            else -> error("Evaluation of navigation '$this' on '$element' should result in a String, but it does not!")
        }
    }
    */

    private fun _property(name: String, value: AsmValue) {
        _element.setProperty(PropertyValueName(name), value, 0)//TODO childIndex
    }

    fun propertyUnnamedString(value: String?) = this.propertyString(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, value)
    fun propertyString(name: String, value: String?) = this._property(name, value?.let { AsmPrimitiveSimple.stdString(it) } ?: AsmNothingSimple)
    fun propertyNothing(name: String) = this._property(name, AsmNothingSimple)
    fun propertyUnnamedElement(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure =
        propertyElementExplicitType(Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value, typeName, init)

    fun propertyTuple(name: String, tupleTypeId: Int? = null, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front) ?: error("No namespace '${_elementQualifiedTypeName.front.value}'")
        val tt = tupleTypeId?.let {
            ns.findTupleTypeWithIdOrNull(tupleTypeId)
        } ?: ns.createTupleType()
        return propertyElementExplicitType(name, tt.qualifiedName.value, init)
    }

    fun propertyElement(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure = propertyElementExplicitType(name, name, init)
    fun propertyElementExplicitType(name: String, typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _element.semanticPath!!.plus(name)
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front)
            ?: _defaultNamespace
        val b = AsmElementSimpleBuilder(_typeModel, ns, _crossReferenceModel, _context, _scopeMap, this._asm, _identifyingValueInFor, newPath, typeName, false, _elementScope)
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
        val newPath = _element.semanticPath!! + name
        val ns = _typeModel.findNamespaceOrNull(_elementQualifiedTypeName.front)
            ?: _defaultNamespace
        val b = ListAsmElementSimpleBuilder(_typeModel, ns, _crossReferenceModel,_context, _scopeMap, this._asm, newPath, _elementScope,_identifyingValueInFor)
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
        return _element
    }
}

@AsmSimpleBuilderMarker
class ListAsmElementSimpleBuilder(
    private val _typeModel: TypeModel,
    private val _defaultNamespace: TypeNamespace,
    private val _crossReferenceModel: CrossReferenceModel,
    private val _context: ContextWithScope<Any, Any>?,
    private val _scopeMap: MutableMap<AsmPath, ScopeSimple<Any>>,
    private val _asm: AsmSimple,
    private val _asmPath: AsmPath,
    private val _parentScope: ScopeSimple<Any>?,
    private val _identifyingValueInFor: (inTypeName:SimpleName, item:AsmStructure) -> Any?,
) {

    private val _list = mutableListOf<AsmValue>()

    fun string(value: String) {
        _list.add(AsmPrimitiveSimple.stdString(value))
    }

    fun list(init: ListAsmElementSimpleBuilder.() -> Unit) {
        val newPath = _asmPath + (_list.size).toString()
        val b = ListAsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _context, _scopeMap, _asm, newPath, _parentScope,_identifyingValueInFor)
        b.init()
        val list = b.build()
        _list.add(list)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val newPath = _asmPath + (_list.size).toString()
        val b = AsmElementSimpleBuilder(_typeModel, _defaultNamespace, _crossReferenceModel, _context, _scopeMap, this._asm, _identifyingValueInFor, newPath, typeName, false, _parentScope)
        b.init()
        val el = b.build()
        _list.add(el)
        return el
    }

    fun tuple(init: AsmElementSimpleBuilder.() -> Unit): AsmStructure {
        val tt = StdLibDefault.TupleType
        return element(tt.qualifiedName.value, init)
    }

    fun build(): AsmList {
        return AsmListSimple(this._list)
    }
}