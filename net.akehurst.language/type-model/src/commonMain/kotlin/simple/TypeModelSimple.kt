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

import net.akehurst.language.typemodel.api.*

object SimpleTypeModelStdLib : TypeModelAbstract("simple", "std", null, emptySet<TypeModel>()) {
    val String = TypeUsage.ofType(super.findOrCreatePrimitiveTypeNamed("String"))
    val Boolean = TypeUsage.ofType(super.findOrCreatePrimitiveTypeNamed("Boolean"))
    val Integer = TypeUsage.ofType(super.findOrCreatePrimitiveTypeNamed("Integer"))
    val Real = TypeUsage.ofType(super.findOrCreatePrimitiveTypeNamed("Real"))
    val Timestamp = TypeUsage.ofType(super.findOrCreatePrimitiveTypeNamed("Timestamp"))
}

class TypeModelSimple(
    namespace: String,
    name: String,
    rootTypeName: String?,
    imports: Set<TypeModel> = setOf(SimpleTypeModelStdLib)
) : TypeModelAbstract(namespace, name, rootTypeName, imports) {

}

abstract class TypeModelAbstract(
    override val namespace: String,
    override val name: String,
    override val rootTypeName: String?,
    override val imports: Set<TypeModel>
) : TypeModel {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeUsage>, UnnamedSuperTypeType>()

    private var _nextTupleTypeId = 0
    private val _unnamedTupleTypes = hashMapOf<List<TypeUsage>, TupleType>()


    override val qualifiedName get() = if (namespace.isBlank()) name else "$namespace.$name"

    override val allTypesByName = mutableMapOf<String, TypeDefinition>()

    override val allTypes: Collection<TypeDefinition> get() = allTypesByName.values

    override val elementType: Set<ElementType> get() = allTypesByName.values.filterIsInstance<ElementType>().toSet()

    override val primitiveType: Set<PrimitiveType> get() = allTypesByName.values.filterIsInstance<PrimitiveType>().toSet()

    override fun findTypeNamed(typeName: String): TypeDefinition? = allTypesByName[typeName] ?: imports.firstNotNullOfOrNull { it.findTypeNamed(typeName) }

    override fun findOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType {
        val existing = findTypeNamed(typeName) ?: imports.firstNotNullOfOrNull { it.findOrCreatePrimitiveTypeNamed(typeName) }
        return if (null == existing) {
            val t = PrimitiveType(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOrCreateElementTypeNamed(typeName: String): ElementType {
        val existing = findTypeNamed(typeName) //?: imports.firstNotNullOfOrNull { it.findOrCreatePrimitiveTypeNamed(typeName) }
        return if (null == existing) {
            val t = ElementType(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as ElementType
        }
    }

    override fun createUnnamedSuperTypeType(subtypes: List<TypeUsage>): UnnamedSuperTypeType {
        val existing = _unnamedSuperTypes[subtypes]
        return if (null == existing) {
            val t = UnnamedSuperTypeType(_nextUnnamedSuperTypeTypeId++, subtypes)
            _unnamedSuperTypes[subtypes] = t
            t
        } else {
            existing
        }
    }

//    override fun createTupleType(): TupleType {
//        val existing = _unnamedSuperTypes[subtypes]
//        return if (null == existing) {
//            val t = TupleType(_nextUnnamedSuperTypeTypeId++, subtypes)
//            _unnamedSuperTypes[subtypes] = t
//            t
//        } else {
//            existing
//        }
//    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeModel -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = "${namespace}.$name"
}