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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.collections.emptyListSeparated
import net.akehurst.language.agl.collections.mutableListSeparated
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.grammar.Choice
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.typeModel.*

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
class SyntaxAnalyserSimple(
    val typeModel: TypeModel?,
    val scopeModel: ScopeModel?
) : SyntaxAnalyser<AsmSimple, ContextSimple> {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"
    }

    private var _asm: AsmSimple? = null
    private var _context: ContextSimple? = null // cached value, provided on call to transform
    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    //private val _scopeMap = mutableMapOf<AsmElementPath, ScopeSimple<AsmElementPath>>()
    private lateinit var _mapToGrammar: (Int, Int) -> RuleItem

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private fun findType(name:String) = this.typeModel?.findType(name)

    override fun clear() {
        this.locationMap.clear()
        this._asm = null
        this._context = null
        this._issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem, context: ContextSimple?): SyntaxAnalysisResult<AsmSimple> {
        this._mapToGrammar = mapToGrammar
        this._context = context
        _asm = AsmSimple()
        val path = AsmElementPath.ROOT + (_asm!!.rootElements.size).toString()
        val value = this.createValue(sppt.root, path, context?.rootScope)
        val rootEl = if (null == value) {
            val el = _asm!!.createElement(path, sppt.root.name)
            val pName = TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
            this.setPropertyOrReference(el, pName, value)
            el
        } else when (value) {
            is String -> {
                val el = _asm!!.createElement(path, sppt.root.name)
                val pName = TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
                this.setPropertyOrReference(el, pName, value)
                el
            }

            is List<*> -> {
                val el = _asm!!.createElement(path, sppt.root.name)
                val pName = TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                this.setPropertyOrReference(el, pName, value)
                el
            }

            is AsmElementSimple -> value
            else -> error("Internal Error: unhandled class ${value::class.simpleName}")
        }
        _asm?.addRoot(rootEl)

        return SyntaxAnalysisResultDefault(_asm, _issues.issues, locationMap)
    }

    private fun createValue(target: SPPTNode, path: AsmElementPath, scope: ScopeSimple<AsmElementPath>?): Any? {
        val elType = this.findType(target.name)
        return when {
            null == elType -> {
                "No Element Type for ${target.name}" //TODO
            }

            else -> createValue(target, path, elType)//, scope)
        }
    }

    private fun createValue(target: SPPTNode, path: AsmElementPath, elType: RuleType): Any? {//, scope: ScopeSimple<AsmElementPath>?): Any? {
        val v = when (target) {
            is SPPTLeaf -> createValueFromLeaf(target)
            is SPPTBranch -> createValueFromBranch(target, path, elType)//, scope)
            else -> error("should never happen!")
        }
        if (v is AsmElementSimple) {
            locationMap[v] = target.location
           // this.addToScope(scope, v)
        }
        return v
    }

    private fun createValueFromLeaf(target: SPPTLeaf): Any? {
        val leaf = target //as SPPTLeafDefault
        val value = when {
            leaf.isEmptyLeaf -> null
            else -> leaf.nonSkipMatchedText
        }
        return value
    }

    private fun createValueFromBranch(target: SPPTBranch, path: AsmElementPath, elType: RuleType):Any?{//, scope: ScopeSimple<AsmElementPath>?): Any? {
        return when (elType) {
            is StringType -> {
                val ch = target.children[0]
                createValue(ch, path, elType)//, scope)
            }

            is AnyType -> {
                val actualType = this.findType(target.name) ?: error("Internal Error: cannot find actual type for ${target.name}")
                when (actualType) {
                    is AnyType -> {// when {
                        //must be a choice in a group
                        val choice = _mapToGrammar(target.runtimeRuleSetNumber, target.runtimeRuleNumber) as Choice
                        when (choice.alternative.size) {
                            1 -> {
                                val ch = target.children[0]
                                val childType = this.findType(ch.name) ?: error("Internal Error: cannot find type for ${ch.name}")
                                val chPath = path
                                //val childsScope = scope
                                createValue(ch, chPath, childType)//, childsScope)
                            }

                            else -> {
                                TODO()
                            }
                        }
                        //else -> error("Internal Error: cannot find actual type for ${target.name}")
                    }

                    else -> createValue(target, path, actualType)//, scope)
                }
            }

            is NothingType -> TODO()

            is UnnamedSuperTypeType -> {
                var actualTarget: SPPTNode = target
                var actualType: RuleType = elType
                while (actualType is WithSubtypes && actualType.subtypes.isNotEmpty()) {
                    actualType = actualType.subtypes[actualTarget.option]
                    actualTarget = actualTarget.asBranch.nonSkipChildren[0]
                }
                this.createValue(actualTarget, path, actualType)//, scope)
            }

            is ListSimpleType -> {
                val el = mutableListOf<Any>()
                //val childsScope = scope
                for (ch in target.children) {
                    TODO()
                }
                el
            }

            is ListSeparatedType -> {
                TODO()
            }

            is TupleType -> {
                val el = _asm!!.createElement(path, elType.name)
                //val childsScope = createScope(scope, el)
                for (propDecl in elType.property.values) {
                    val propType = propDecl.type
                    val childPath = path + propDecl.name
                    val propertyValue = when (propType) {
                        is AnyType -> {
                            val ch = target.asBranch.nonSkipChildren[propDecl.childIndex]
                            this.createValue(ch, childPath, propType)//, childsScope)
                        }

                        is StringType -> {
                            val ch = target.asBranch.nonSkipChildren[propDecl.childIndex]
                            val propValue = when {
                                ch.isLeaf -> this.createValue(ch, childPath, propType)//, childsScope)
                                ch.isEmptyMatch -> null
                                else -> this.createValue(ch.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                            }
                            propValue
                        }

                        is ListSimpleType -> {
                            val ch = target.asBranch.nonSkipChildren[propDecl.childIndex]
                            val propValue = when {
                                target.isList -> when {
                                    target.isEmptyLeaf -> emptyList<Any>()
                                    else -> target.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
                                        val childPath2 = childPath + ci.toString()
                                        if (b.isLeaf && b.asLeaf.isExplicitlyNamed.not()) {
                                            null
                                        } else {
                                            this.createValue(b, childPath2, propType)//, childsScope)
                                        }
                                    }
                                }

                                else -> when {
                                    ch.isEmptyLeaf -> emptyList<Any>()
                                    else -> ch.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
                                        val childPath2 = childPath + ci.toString()
                                        if (b.isLeaf && b.asLeaf.isExplicitlyNamed.not()) {
                                            null
                                        } else {
                                            this.createValue(b, childPath2, propType)//, childsScope)
                                        }
                                    }
                                }
                            }
                            propValue
                        }

                        is ElementType -> {
                            val ch = target.asBranch.nonSkipChildren[propDecl.childIndex]
                            val propValue = when {
                                propDecl.isNullable && ch.isOptional -> when {
                                    ch.isEmptyLeaf -> null
                                    else -> this.createValue(ch.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                                }

                                propType.subtypes.isNotEmpty() && ch.asBranch.nonSkipChildren.size == 1 -> this.createValue(
                                    ch.asBranch.nonSkipChildren[0],
                                    childPath,
                                    propType,
                                    //childsScope
                                )

                                else -> this.createValue(ch, childPath, propType)//, childsScope)
                            }
                            propValue
                        }

                        is TupleType -> {
                            val ch = target.asBranch.nonSkipChildren[propDecl.childIndex]
                            val propValue = when {
                                propDecl.isNullable && ch.isOptional -> when {
                                    ch.isEmptyLeaf -> null
                                    else -> this.createValue(ch, childPath, propType)//, childsScope)
                                }
                                //propType.subType.isNotEmpty() && ch.asBranch.nonSkipChildren.size == 1 -> this.createValue(ch.asBranch.nonSkipChildren[0], childPath, childsScope)
                                else -> this.createValue(ch, childPath, propType)//, childsScope)
                            }
                            propValue
                        }

                        else -> error("Internal Error: type $propType not handled")
                    }
                    setPropertyOrReference(el, propDecl.name, propertyValue)
                }
                el
            }

            is ElementType -> {
                var actualTarget: SPPTNode = target
                var actualType: ElementType = elType
                while (actualType.subtypes.isNotEmpty()) {
                    actualType = actualType.subtypes[actualTarget.option]
                    actualTarget = actualTarget.asBranch.nonSkipChildren[0]
                }
                //val actualTarget = when {
                //    elType.subType.isNotEmpty() -> target.nonSkipChildren[0]
                //    else -> target
                //}
                // val actualType = when {
                //    elType.subType.isNotEmpty() -> elType.subType.first { it.name == actualTarget.name }//.nonSkipChildren[0].name } TODO: use 'option' to get subtype
                //     else -> elType
                //}
                val el = _asm!!.createElement(path, actualType.name)
                //val childsScope = createScope(scope, el)
                for (propDecl in actualType.property.values) {
                    val propTgt = actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                    if(propDecl.isNullable && propTgt.asBranch.nonSkipChildren[0].isEmptyLeaf) {
                        setPropertyOrReference(el, propDecl.name, null)
                    } else {
                        val propType = propDecl.type
                        val childPath = path + propDecl.name
                        val propertyValue = when (propType) {
                            is AnyType -> {
                                this.createValue(propTgt, childPath, propType)//, childsScope)
                            }

                            is StringType -> {
                                val propValue = when {
                                    propTgt.isLeaf -> this.createValue(propTgt, childPath, propType)//, childsScope)
                                    propTgt.isEmptyMatch -> null
                                    else -> this.createValue(propTgt.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                                }
                                propValue
                            }

                            is ListSimpleType -> {
                                val propValue = when {
                                    actualTarget.isList -> when {
                                        actualTarget.isEmptyLeaf -> emptyList<Any>()
                                        else -> actualTarget.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
                                            val childPath2 = childPath + ci.toString()
                                            if (b.isLeaf && b.asLeaf.isExplicitlyNamed.not()) {
                                                null
                                            } else {
                                                this.createValue(b, childPath2, propType.elementType)//, childsScope)
                                            }
                                        }
                                    }

                                    else -> when {
                                        propTgt.isEmptyLeaf -> emptyList<Any>()
                                        else -> propTgt.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
                                            val childPath2 = childPath + ci.toString()
                                            if (b.isLeaf && b.asLeaf.isExplicitlyNamed.not()) {
                                                null
                                            } else {
                                                this.createValue(b, childPath2, propType.elementType)//, childsScope)
                                            }
                                        }
                                    }
                                }
                                propValue
                            }

                            is ListSeparatedType -> {
                                val propValue = when {
                                    actualTarget.isList -> when {
                                        actualTarget.isEmptyLeaf -> emptyListSeparated<Any, Any>()
                                        else -> {
                                            val elements = actualTarget.asBranch.nonSkipChildren
                                            val sList = mutableListSeparated<Any, Any>()
                                            for (ci in 0 until elements.size) {
                                                val cel = elements[ci]
                                                val type = if (ci / 2 == 0) propType.itemType else propType.separatorType
                                                val childPath2 = childPath + ci.toString()
                                                if (cel.isLeaf && cel.asLeaf.isExplicitlyNamed.not()) {
                                                    //do not add iteml
                                                } else {
                                                    val chEl = this.createValue(cel, childPath2, type)//, childsScope)
                                                    sList.add(chEl)
                                                }

                                            }
                                            sList
                                        }
                                    }

                                    else -> when {
                                        propTgt.isEmptyLeaf -> emptyList<Any>()
                                        else -> {
                                            val elements = propTgt.asBranch.nonSkipChildren
                                            val sList = mutableListSeparated<Any, Any>()
                                            for (ci in 0 until elements.size) {
                                                val cel = elements[ci]
                                                val type = if (ci % 2 == 0) propType.itemType else propType.separatorType
                                                val childPath2 = childPath + ci.toString()
                                                if (cel.isLeaf && cel.asLeaf.isExplicitlyNamed.not()) {
                                                    //do not add item
                                                } else {
                                                    val chEl = this.createValue(cel, childPath2, type)//, childsScope)
                                                    sList.add(chEl)
                                                }

                                            }
                                            sList
                                        }
                                    }
                                }
                                propValue

                            }

                            is ElementType -> {
                                val propValue = when {
                                    propDecl.isNullable && propTgt.isOptional -> when {
                                        propTgt.isEmptyLeaf -> null
                                        else -> this.createValue(propTgt.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                                    }

                                    propType.subtypes.isNotEmpty() && propTgt.asBranch.nonSkipChildren.size == 1 -> this.createValue(
                                        propTgt,
                                        childPath,
                                        propType,
                                        //childsScope
                                    )

                                    else -> this.createValue(propTgt, childPath, propType)//, childsScope)
                                }
                                propValue
                            }

                            is TupleType -> {
                                val propValue = when {
                                    propDecl.isNullable && propTgt.isOptional -> when {
                                        propTgt.isEmptyLeaf -> null
                                        else -> this.createValue(propTgt, childPath, propType)//, childsScope)
                                    }
                                    //propType.subType.isNotEmpty() && ch.asBranch.nonSkipChildren.size == 1 -> this.createValue(ch.asBranch.nonSkipChildren[0], childPath, childsScope)
                                    else -> this.createValue(propTgt, childPath, propType)//, childsScope)
                                }
                                propValue
                            }

                            is UnnamedSuperTypeType -> {
                                val actualPropType = propType.subtypes[propTgt.option]
                                val actualPropTgt = propTgt.asBranch.nonSkipChildren[0]
                                this.createValue(actualPropTgt, childPath, actualPropType)//, childsScope)
                            }

                            else -> error("Internal Error: type $propType not handled")
                        }
                        setPropertyOrReference(el, propDecl.name, propertyValue)
                    }
                }
                el
            }

        }
    }

    /*
        private fun createValueFromBranch1(target: SPPTBranch, path: AsmElementPath, scope: ScopeSimple<AsmElementPath>?): Any? {
            val br = target as SPPTBranchFromInputAndGrownChildren //SPPTBranchDefault //TODO: make write thing available on interface
            val elType = typeModel.let { typeModel.findType(target.name) }
            return when (br.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> error("should never happen!")
                RuntimeRuleKind.NON_TERMINAL -> when (br.runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> TODO("Empty rules not yet supported")
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        val value = this.createValue(br.children[0], path, scope)
                        if (null == value) {
                            val el = _asm!!.createElement(path, br.name)
                            val pName = TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME //TODO: maybe a better option
                            this.setPropertyOrReference(el, pName, value)
                            el
                        } else when (value) {
                            is String -> {
                                val el = _asm!!.createElement(path, br.name)
                                val pName = TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME //TODO: maybe a better option
                                this.setPropertyOrReference(el, pName, value)
                                el
                            }

                            is List<*> -> {
                                val el = _asm!!.createElement(path, br.name)
                                val pName = TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME //TODO: maybe a better option
                                this.setPropertyOrReference(el, pName, value)
                                el
                            }

                            else -> value
                        }
                    }

                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        val count = mutableMapOf<String, Int>()
                        val el = _asm!!.createElement(path, br.name)
                        val childsScope = createScope(scope, el)
                        br.runtimeRule.rhs.items.forEachIndexed { index, rr ->
                            //TODO: leave out unnamed literals
                            if (rr.tag == "'${rr.value}'") {
                                // is unnamed literal don't use it as a property
                            } else {
                                val name = createPropertyName(rr)
                                val nname = if (count.containsKey(name)) {
                                    val i = count[name]!! + 1
                                    count[name] = i
                                    name + i
                                } else {
                                    count[name] = 1
                                    name
                                }
                                val childPath = path + nname
                                val value = this.createValue(br.nonSkipChildren[index], childPath, childsScope)
                                this.setPropertyOrReference(el, nname, value)
                            }
                        }

                        if (br.runtimeRule.rhs.items.size == 1) {
                            if (br != br.tree?.root &&
                                br.runtimeRule.rhs.items[0].kind == RuntimeRuleKind.NON_TERMINAL
                                && br.runtimeRule.rhs.items[0].rhs.itemsKind == RuntimeRuleRhsItemsKind.LIST
                            ) {
                                el.properties.values.first().value
                            } else {
                                el
                            }
                        } else {
                            el
                        }
                    }

                    RuntimeRuleRhsItemsKind.LIST -> when (br.runtimeRule.rhs.listKind) {
                        RuntimeRuleListKind.MULTI -> {
                            val list = br.nonSkipChildren.mapIndexedNotNull { i, b ->
                                val childPath = path + i.toString()
                                this.createValue(b, childPath, scope)
                            }
                            val value = if (br.runtimeRule.rhs.multiMax == 1) {
                                if (list.isEmpty()) null else list[0]
                            } else {
                                list
                            }
                            value
                        }

                        RuntimeRuleListKind.SEPARATED_LIST -> {
                            val list = br.nonSkipChildren.mapIndexedNotNull { i, b ->
                                val childPath = path + i.toString()
                                this.createValue(b, childPath, scope)
                            }
                            if (br.runtimeRule.rhs.multiMax == 1) {
                                val value = if (list.isEmpty()) null else list[0]
                                value
                            } else {
                                list
                            }
                        }

                        RuntimeRuleListKind.NONE -> error("Internal Error: should not happen")
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO("Left Associated List not yet supported")
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO("Right Associated List not yet supported")
                        RuntimeRuleListKind.UNORDERED -> TODO("Unordered List not yet supported")
                    }
                }

                RuntimeRuleKind.GOAL -> error("Internal Error: Should never happen")
                RuntimeRuleKind.EMBEDDED -> TODO("Embedded rules not yet supported")
            }
        }

        private fun createPropertyName(runtimeRule: RuntimeRule): String {
            //TODO: think we have to determine if rr is a pseudo rule or not here!
            return when (runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> runtimeRule.tag
                RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.LIST -> when (runtimeRule.rhs.listKind) {
                        RuntimeRuleListKind.MULTI -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleRhs.MULTI__ITEM])
                        RuntimeRuleListKind.SEPARATED_LIST -> runtimeRule.tag
                        RuntimeRuleListKind.NONE -> error("Internal Error: should not happen")
                        RuntimeRuleListKind.LEFT_ASSOCIATIVE_LIST -> TODO("Left Associated List not yet supported")
                        RuntimeRuleListKind.RIGHT_ASSOCIATIVE_LIST -> TODO("Right Associated List not yet supported")
                        RuntimeRuleListKind.UNORDERED -> TODO("Unordered List not yet supported")
                    }

                    else -> runtimeRule.tag
                }

                RuntimeRuleKind.EMBEDDED -> runtimeRule.tag
                RuntimeRuleKind.GOAL -> runtimeRule.tag
            }
        }
    */
    // private fun getReferable(scopeFor: String, el: AsmElementSimple): String? {
    //     val prop = scopeModel.getReferablePropertyNameFor(scopeFor, el.typeName)
    //     return if (null == prop) {
    //         null
    //     } else {
    //         el.getPropertyAsString(prop)
    //     }
    // }
/*
    private fun createScope(scope: ScopeSimple<AsmElementPath>?, el: AsmElementSimple): ScopeSimple<AsmElementPath>? {
        return if (null == scope) {
            null
        } else {
            if (scopeModel.isScopeDefinition(el.typeName)) {
                val refInParent = scopeModel.createReferenceLocalToScope(scope, el)
                if (null != refInParent) {
                    val newScope = scope.createOrGetChildScope(refInParent, el.typeName, el.asmPath)
                    //_scopeMap[el.asmPath] = newScope
                    newScope
                } else {
                    _issues.error(
                        this.locationMap[el],
                        "Trying to create child scope but cannot create a reference for $el"
                    )
                    scope
                }
            } else {
                scope
            }
        }
    }

    private fun addToScope(scope: ScopeSimple<AsmElementPath>?, el: AsmElementSimple) {
        if (null == scope) {
            //do nothing
        } else {
            if (scopeModel.shouldCreateReference(scope.forTypeName, el.typeName)) {
                //val reference = scopeModel.createReferenceFromRoot(scope, el)
                val scopeLocalReference = scopeModel.createReferenceLocalToScope(scope, el)
                if (null != scopeLocalReference) {
                    val contextRef = el.asmPath
                    scope.addToScope(scopeLocalReference, el.typeName, contextRef)
                } else {
                    _issues.error(this.locationMap[el], "Cannot create a local reference in '$scope' for $el")
                }
            } else {
                // no need to add it to scope
            }
        }
    }
*/
    private fun isReference(el: AsmElementSimple, name: String): Boolean {
        return scopeModel?.isReference(el.typeName, name) ?: false
    }

    private fun setPropertyOrReference(el: AsmElementSimple, name: String, value: Any?) {
        val isRef = this.isReference(el, name)
        when {
            isRef -> el.setProperty(name, value, true)
            else -> el.setProperty(name, value, false)
        }

    }
}