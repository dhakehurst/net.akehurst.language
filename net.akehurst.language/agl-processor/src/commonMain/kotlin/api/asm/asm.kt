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

import net.akehurst.language.collections.ListSeparated

interface AsmPath {
    val value: String
    val segments: List<String>
    val parent: AsmPath?

    val isExternal: Boolean

    operator fun plus(segment: String): AsmPath
}

interface Asm {
    val root: List<AsmValue>
    val elementIndex: Map<AsmPath, AsmStructure>

    fun traverseDepthFirst(callback: AsmTreeWalker)

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
}

interface AsmValue {

    val qualifiedTypeName: String

    /**
     * last segment of the qualifiedTypeName
     */
    val typeName: String

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
    fun equalTo(other: AsmValue): Boolean
}

interface AsmNothing : AsmValue

interface AsmPrimitive : AsmValue {
    val value: Any
}

interface AsmReference {
    val reference: String
    val value: AsmStructure?
}

interface AsmStructure : AsmValue {
    val path: AsmPath

    val property: Map<String, AsmStructureProperty>

    val propertyOrdered: List<AsmStructureProperty>

    /**
     * The value of the properties that are not references
     */
    val children: List<AsmValue>

    /**
     * true if the value has the named property
     */
    fun hasProperty(name: String): Boolean

    /**
     * the value of the named property, AsmNothing if no property with that name
     */
    fun getProperty(name: String): AsmValue

    fun setProperty(name: String, value: AsmValue, childIndex: Int)
}

interface AsmStructureProperty {
    val name: String
    val value: AsmValue
    val index: Int

    val isReference: Boolean

    fun convertToReferenceTo(referredValue: AsmStructure?)

    fun equalTo(other: AsmStructureProperty): Boolean
}

interface AsmList : AsmValue {
    val elements: List<AsmValue>

    val isEmpty: Boolean
    val isNotEmpty: Boolean
}

interface AsmListSeparated : AsmList {
    override val elements: ListSeparated<AsmValue, AsmValue, AsmValue>
}

interface AsmTreeWalker {
    fun beforeRoot(root: AsmValue)
    fun afterRoot(root: AsmValue)
    fun onNothing(owningProperty: AsmStructureProperty?, value: AsmNothing)
    fun onPrimitive(owningProperty: AsmStructureProperty?, value: AsmPrimitive)
    fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure)
    fun onProperty(owner: AsmStructure, property: AsmStructureProperty)
    fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure)
    fun beforeList(owningProperty: AsmStructureProperty?, value: AsmList)
    fun afterList(owningProperty: AsmStructureProperty?, value: AsmList)
}
