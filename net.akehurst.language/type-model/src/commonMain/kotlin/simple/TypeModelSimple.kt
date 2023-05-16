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

package net.akehurst.language.typemodel.simple

import net.akehurst.language.typemodel.api.ElementType
import net.akehurst.language.typemodel.api.PrimitiveType
import net.akehurst.language.typemodel.api.TypeDefinition
import net.akehurst.language.typemodel.api.TypeModel

class TypeModelSimple(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace, name) {

}

abstract class TypeModelAbstract(
    override val namespace: String,
    override val name: String
) : TypeModel {

    override val qualifiedName get() = "$namespace.$name"

    override val allTypesByName = mutableMapOf<String, TypeDefinition>()

    override fun findTypeNamed(typeName: String): TypeDefinition? = allTypesByName[typeName]

    override fun findOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType {
        val existing = findTypeNamed(typeName)
        return if (null == existing) {
            val t = PrimitiveType(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOrCreateElementTypeNamed(typeName: String): ElementType {
        val existing = findTypeNamed(typeName)
        return if (null == existing) {
            val t = ElementType(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as ElementType
        }
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeModel -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = "${namespace}.$name"
}