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

package net.akehurst.language.api.typeModel

import net.akehurst.language.agl.collections.MutableOrderedSet
import net.akehurst.language.agl.collections.OrderedSet
import net.akehurst.language.agl.collections.mutableOrderedSetOf
import net.akehurst.language.agl.util.Debug

interface TypeModel {

    val types: Map<String, ElementType>

    fun findType(name: String): RuleType?
}

interface RuleType {
    val name: String
}

interface StructuredRuleType : RuleType {
    val property : MutableMap<String,PropertyDeclaration>
    fun getPropertyByIndex(i:Int):PropertyDeclaration
    fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration)
}

class BuiltInType private constructor(override val name: String) : RuleType {
    companion object {
        val NOTHING = BuiltInType("\$Nothing")
        val STRING = BuiltInType("\$String")
        //val LIST = BuiltInType("\$List")
        val ANY = BuiltInType("\$Any")
    }

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is BuiltInType -> other.name == this.name
        else -> false
    }

    override fun toString(): String = name
}

data class ListType(val elementType:RuleType) : RuleType {
    override val name: String = "List<$elementType>"
}

class TupleType() : StructuredRuleType {
    companion object {
        const val INSTANCE_NAME = "Tuple"
    }
    override val name: String = INSTANCE_NAME
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableListOf<PropertyDeclaration>()

    override fun getPropertyByIndex(i:Int):PropertyDeclaration = _propertyIndex[i]
    override fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration)  {
        if (Debug.CHECK) check(propertyDeclaration.owner==this)
        if (Debug.CHECK) check(this.property.containsKey(name).not())
        this.property[name]=propertyDeclaration
        this._propertyIndex.add(propertyDeclaration)
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when(other) {
        !is TupleType -> false
        else ->this._propertyIndex==other._propertyIndex
    }

    override fun toString(): String = "Tuple<${this._propertyIndex.joinToString { it.type.name }}>"
}

data class ElementType(override val name: String) : StructuredRuleType {
    val superType:OrderedSet<ElementType> = mutableOrderedSetOf<ElementType>()
    val subType:Set<ElementType> = mutableSetOf<ElementType>()
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableListOf<PropertyDeclaration>()

    fun addSuperType(type:ElementType) {
        (this.superType as MutableOrderedSet).add(type)
        (type.subType as MutableSet).add(this)
    }

    override fun getPropertyByIndex(i:Int):PropertyDeclaration = _propertyIndex[i]
    override fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration) {
        if (Debug.CHECK) check(propertyDeclaration.owner==this)
        if (Debug.CHECK) check(this.property.containsKey(name).not())
        this.property[name]=propertyDeclaration
        this._propertyIndex.add(propertyDeclaration)
    }
}

data class PropertyDeclaration(
    val owner: StructuredRuleType,
    val name: String,
    val type: RuleType,
    val isNullable: Boolean,
    val childIndex: Int // to indicate the child number in an SPPT
) {
    init {
        owner.appendProperty(name, this)
    }

    override fun toString(): String  {
        val nullable = if (isNullable) "?" else ""
        return "${owner.name}.$name: ${type.name} $nullable [$childIndex]"
    }
}