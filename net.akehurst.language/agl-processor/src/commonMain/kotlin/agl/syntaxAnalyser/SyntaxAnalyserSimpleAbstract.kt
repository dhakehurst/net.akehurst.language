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

import net.akehurst.language.agl.collections.mutableListSeparated
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
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
import net.akehurst.language.api.typemodel.*

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
abstract class SyntaxAnalyserSimpleAbstract<A : AsmSimple>(
    val typeModel: TypeModel,
    val scopeModel: ScopeModel
) : SyntaxAnalyser<A> {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"
    }

    private var _asm: AsmSimple? = null
    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    //private val _scopeMap = mutableMapOf<AsmElementPath, ScopeSimple<AsmElementPath>>()
    private lateinit var _mapToGrammar: (Int, Int) -> RuleItem

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private fun findTypeForRule(ruleName: String) = this.typeModel.findTypeUsageForRule(ruleName)

    override fun clear() {
        this.locationMap.clear()
        this._asm = null
        this._issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<A> {
        this._mapToGrammar = mapToGrammar
        _asm = AsmSimple()
        val path = AsmElementPath.ROOT + (_asm!!.rootElements.size).toString()
        val value = this.createValue(sppt.root, path)
        /*
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
         */
        if (null == value) {
            _asm?.addRoot("<null>")
        } else {
            _asm?.addRoot(value)
        }

        return SyntaxAnalysisResultDefault(_asm as A?, _issues, locationMap)
    }

    private fun createValue(target: SPPTNode, path: AsmElementPath): Any? {
        val elType = this.findTypeForRule(target.name)
        return when {
            null == elType -> {
                "No Element Type for ${target.name}" //TODO
            }

            else -> createValue(target, path, elType)//, scope)
        }
    }

    private fun createValue(target: SPPTNode, path: AsmElementPath, elType: TypeUsage): Any? {//, scope: ScopeSimple<AsmElementPath>?): Any? {
        val v = when (target) {
            is SPPTLeaf -> createValueFromLeaf(target)
            is SPPTBranch -> createValueFromBranch(target, path, elType)//, scope)
            else -> error("should never happen!")
        }
        if (v is AsmElementSimple) {
            locationMap[v] = target.location
        }
        return v
    }

    private fun createValueFromLeaf(target: SPPTLeaf): String? = when {
        target.isEmptyLeaf -> null
        else -> target.nonSkipMatchedText
    }

    private fun createValueFromBranch(target: SPPTBranch, path: AsmElementPath, argType: TypeUsage): Any? {//, scope: ScopeSimple<AsmElementPath>?): Any? {
        val type = typeModel.findTypeUsageForRule(target.name) ?: argType
        return when (type.type) {
            is StringType -> createStringValueFromBranch(target)

            is AnyType -> {
                val actualType = this.findTypeForRule(target.name) ?: error("Internal Error: cannot find actual type for ${target.name}")
                when (actualType.type) {
                    is AnyType -> {// when {
                        //must be a choice in a group
                        val choice = _mapToGrammar(target.runtimeRuleSetNumber, target.runtimeRuleNumber) as Choice
                        when (choice.alternative.size) {
                            1 -> {
                                val ch = target.children[0]
                                val childType = this.findTypeForRule(ch.name) ?: error("Internal Error: cannot find type for ${ch.name}")
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
                var actualType = type
                while ((actualType.type is UnnamedSuperTypeType) && (actualType.type as UnnamedSuperTypeType).subtypes.isNotEmpty()) {
                    val consume = when {
                        actualType.type is UnnamedSuperTypeType -> when {
                            (actualType.type as UnnamedSuperTypeType).consumeNode -> true
                            else -> false
                        }

                        else -> true
                    }
                    actualType = (actualType.type as UnnamedSuperTypeType).subtypes[actualTarget.option]
                    actualTarget = when {
                        consume -> actualTarget.asBranch.nonSkipChildren[0]
                        else -> actualTarget
                    }

                }
                this.createValue(actualTarget, path, actualType)//, scope)
            }

            is ListSimpleType -> createListSimpleValueFromBranch(target, path, type)

            is ListSeparatedType -> createListSeparatedValueFromBranch(target, path, type)

            is TupleType -> {
                val el = _asm!!.createElement(path, type.type.name)
                //val childsScope = createScope(scope, el)
                for (propDecl in type.type.property.values) {
                    val propTgt = target.asBranch.nonSkipChildren[propDecl.childIndex]
                    val propType = propDecl.typeUse
                    val propPath = path + propDecl.name
                    val propValue = when {
                        propTgt.isOptional -> when {
                            propDecl.typeUse.nullable -> createValue(propTgt.asBranch.nonSkipChildren[0], propPath, propType)
                            else -> error("Internal Error: '$propDecl' is not nullable !")
                        }

                        propTgt.isEmbedded -> {
                            val embTgt = propTgt.asBranch.nonSkipChildren[0]
                            createValue(embTgt, propPath, propType)
                        }

                        else -> createValue(propTgt, propPath, propType)
                    }
                    /*
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
                                    propTgt.isEmptyLeaf -> emptyList<Any>()
                                    else -> propTgt.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
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
                            val propValue = when {
                                propDecl.isNullable && propTgt.isOptional -> when {
                                    propTgt.isEmptyLeaf -> null
                                    else -> this.createValue(propTgt.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                                }

                                propType.subtypes.isNotEmpty() && propTgt.asBranch.nonSkipChildren.size == 1 -> this.createValue(
                                    propTgt.asBranch.nonSkipChildren[0],
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
                                    else -> this.createValue(propTgt.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
                                }
                                //propType.subType.isNotEmpty() && ch.asBranch.nonSkipChildren.size == 1 -> this.createValue(ch.asBranch.nonSkipChildren[0], childPath, childsScope)
                                else -> this.createValue(propTgt, childPath, propType)//, childsScope)
                            }
                            propValue
                        }

                        else -> error("Internal Error: type $propType not handled")
                    }
                     */
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                }
                el
            }

            is ElementType -> {
                var actualTarget: SPPTNode = target
                var actualType: ElementType = type.type
                while (actualType.subtypes.isNotEmpty()) {
                    actualType = actualType.subtypes[actualTarget.option]
                    actualTarget = actualTarget.asBranch.nonSkipChildren[0]
                }
                val el = _asm!!.createElement(path, actualType.name)
                for (propDecl in actualType.property.values) {
                    val propPath = path + propDecl.name
                    val propType = propDecl.typeUse
                    val propValue = when {
                        propType.type is ListSimpleType -> {
                            val listNode = when {
                                actualTarget.isList -> actualTarget
                                else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            }
                            createListSimpleValueFromBranch(listNode, path, propType)
                        }

                        propType.type is ListSeparatedType -> {
                            val listNode = when {
                                actualTarget.isList -> actualTarget
                                else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            }
                            createListSeparatedValueFromBranch(listNode, path, propType)
                        }

                        else -> {
                            val propTgt = actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            when {
                                propTgt.isOptional -> when {
                                    propDecl.typeUse.nullable -> createValue(propTgt.asBranch.nonSkipChildren[0], propPath, propType)
                                    else -> error("Internal Error: '$propDecl' is not nullable !")
                                }

                                propTgt.isEmbedded -> {
                                    val embTgt = propTgt.asBranch.nonSkipChildren[0]
                                    createValue(embTgt, propPath, propType)
                                }

                                else -> createValue(propTgt, propPath, propType)
                            }
                        }
                    }
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                    /*
                    if (propDecl.isNullable && propTgt.asBranch.nonSkipChildren[0].isEmptyLeaf) {
                        setPropertyOrReference(el, propDecl.name, null)
                    } else {

                        val propertyValue = when (propType) {
                            is AnyType -> this.createValue(propTgt, childPath, propType)
                            is StringType -> createStringValueFromBranch(propTgt)

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
                                        else -> this.createValue(propTgt.asBranch.nonSkipChildren[0], childPath, propType)//, childsScope)
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
                     */
                }
                el
            }
        }
    }

    private fun createStringValueFromBranch(target: SPPTNode): String? = when {
        target.isEmptyMatch -> null
        else -> target.nonSkipMatchedText
    }

    private fun createListSimpleValueFromBranch(target: SPPTNode, path: AsmElementPath, type: TypeUsage): List<*> {
        if (Debug.CHECK) check(type.type is ListSimpleType)
        return when {
            target.isEmptyLeaf -> emptyList<Any>()
            target.isList -> target.asBranch.nonSkipChildren.mapIndexedNotNull { ci, b ->
                val childPath2 = path + ci.toString()
                if (b.isLeaf && b.asLeaf.isExplicitlyNamed.not()) {
                    null
                } else {
                    this.createValue(b, childPath2, type.arguments[0])//, childsScope)
                }
            }
            // a concatenation rule that contains a list maps the list direct to a property
            target.isBranch && target.asBranch.nonSkipChildren[0].isList -> createListSimpleValueFromBranch(target.asBranch.nonSkipChildren[0], path, type)
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createListSeparatedValueFromBranch(target: SPPTNode, path: AsmElementPath, type: TypeUsage): List<*> {
        if (Debug.CHECK) check(type.type is ListSeparatedType)
        return when {
            target.isEmptyLeaf -> emptyList<Any>()
            target.isList -> {
                val elements = target.asBranch.nonSkipChildren
                val sList = mutableListSeparated<Any, Any>()
                for (ci in 0 until elements.size) {
                    val el = elements[ci]
                    val elType = if (ci % 2 == 0) type.arguments[0] else type.arguments[1]
                    val elPath = path + ci.toString()
                    if (el.isLeaf && el.asLeaf.isExplicitlyNamed.not()) {
                        //do not add item
                    } else {
                        val chEl = this.createValue(el, elPath, elType)
                        sList.add(chEl)
                    }

                }
                sList
            }
            // a concatenation rule that contains a list maps the list direct to a property
            target.isBranch && target.asBranch.nonSkipChildren[0].isList -> createListSeparatedValueFromBranch(target.asBranch.nonSkipChildren[0], path, type)
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun isReference(el: AsmElementSimple, name: String): Boolean {
        return scopeModel?.isReference(el.typeName, name) ?: false
    }

    private fun setPropertyOrReferenceFromDeclaration(el: AsmElementSimple, declaration: PropertyDeclaration, value: Any?) {
        val isRef = this.isReference(el, declaration.name)
        when {
            isRef -> el.setPropertyFromDeclaration(declaration, value, true)
            else -> el.setPropertyFromDeclaration(declaration, value, false)
        }

    }
    /*
    private fun setPropertyOrReference(el: AsmElementSimple, name:String, value: Any?) {
        val isRef = this.isReference(el, name)
        when {
            isRef -> el.setProperty(name, value, true)
            else -> el.setProperty(name, value, false)
        }

    }
     */
}