/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlinx.komposite.common

import net.akehurst.kotlinx.komposite.api.KompositeException
import net.akehurst.kotlinx.komposite.processor.Komposite
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.TypeModelSimpleAbstract
import net.akehurst.language.typemodel.asm.typeModel
import kotlin.reflect.KClass

class DatatypeRegistry : TypeModelSimpleAbstract(SimpleName("registry")) {

    companion object {
        val KOTLIN_STD_STRING = """
            namespace kotlin {
                primitive Boolean
                primitive Byte
                primitive Short
                primitive Int
                primitive Long
                primitive Float
                primitive Double
                primitive String
            }
            namespace kotlin.collections {
                collection Array<E>
                collection Collection<E>
                collection List<E>
                collection Set<E>
                collection Map<K,V>
            }
        """.trimIndent()

        val KOTLIN_STD_MODEL = typeModel("kotlin-std",false, emptyList()) {
            namespace("kotlin", emptyList()) {
                primitiveType("Boolean")
                primitiveType("Byte")
                primitiveType("Short")
                primitiveType("Int")
                primitiveType("Long")
                primitiveType("Float")
                primitiveType("Double")
                primitiveType("String")
                dataType("Any") {

                }
            }
            namespace("kotlin.collections", emptyList()) {
                collectionType("Array", listOf("E"))
                collectionType("Collection", listOf("E"))
                collectionType("List", listOf("E")).also { it.addSupertype("Collection".asPossiblyQualifiedName) }
                collectionType("Set", listOf("E")).also { it.addSupertype("Collection".asPossiblyQualifiedName) }
                collectionType("Map", listOf("K", "V"))
            }
        }
        val TypeDeclaration.isKotlinArray get() = this.qualifiedName.value=="kotlin.collections.Array"
        val TypeDeclaration.isKotlinList get() = this.qualifiedName.value=="kotlin.collections.List"
        val TypeDeclaration.isKotlinSet get() = this.qualifiedName.value=="kotlin.collections.Set"
        val TypeDeclaration.isKotlinMap get() = this.qualifiedName.value=="kotlin.collections.Map"

        val JAVA_STD = """
            namespace java.lang {
                primitive Boolean
                primitive Integer
                primitive Long
                primitive Float
                primitive Double
                primitive String
            }
    
            namespace java.util {
                collection Array<T>
				collection Collection<T>
				collection List<T>
                collection Set<T>
                collection Map<K,V>
            }
        """.trimIndent()
    }

    private val _primitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*, *>>()

    fun registerPrimitiveMapper(mapper: PrimitiveMapper<*, *>) {
        this._primitiveMappers[mapper.primitiveKlass] = mapper
    }

    fun registerFromConfigString(kompositeModel: String, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        try {
            val result = Komposite.process(kompositeModel)
            if (null == result.asm) {
                throw KompositeException("Error processing config string", result.issues.errors, null)
            } else {
                this.registerFromTypeModel(result.asm!!, primitiveMappers)
            }
        } catch (e: Exception) {
            throw KompositeException("Error trying to register datatypes from config string - ${e.message}", e)
        }
    }

    fun registerFromTypeModel(typeModel: TypeModel, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        this._primitiveMappers.putAll(primitiveMappers)
        typeModel.allNamespace.forEach {
                this.addNamespace(it)
        }
    }

    fun findTypeDeclarationByKClass(cls: KClass<*>): TypeDeclaration? {
        //TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
        //val qname = cls.qualifiedName ?: error("class does not have a qualifiedName!")
        //return this.findByQualifiedNameOrNull(QualifiedName( qname))
        val qname = cls.simpleName ?: error("class does not have a simple name!")
        return this.findFirstByNameOrNull(SimpleName( qname))
    }

    fun findPrimitiveMapperByKClass(cls: KClass<*>): PrimitiveMapper<*, *>? {
        return this._primitiveMappers[cls]
    }

    fun findPrimitiveMapperBySimpleName(clsName: String): PrimitiveMapper<*, *>? {
        return this._primitiveMappers.values.firstOrNull {
            it.primitiveKlass.simpleName == clsName //FIXME: use qualified name when JS supports it!
        }
    }

    fun isSingleton(value: Any): Boolean {
        return this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) is SingletonType
    }

    fun isPrimitive(value: Any): Boolean {
        return this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) is PrimitiveType
    }

    fun isEnum(value: Any): Boolean {
        return this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) is EnumType
    }

    fun isCollection(value: Any): Boolean {
        //TODO: use type hierachy so we can e.g. register List rather than ArrayList
        return when (value) {
            is List<*> -> this.findFirstByNameOrNull(SimpleName("List")) is CollectionType
            is Set<*> -> this.findFirstByNameOrNull(SimpleName("Set")) is CollectionType
            is Map<*, *> -> this.findFirstByNameOrNull(SimpleName("Map")) is CollectionType
            is Collection<*> -> this.findFirstByNameOrNull(SimpleName("Collection")) is CollectionType
            is Array<*> -> this.findFirstByNameOrNull(SimpleName("Array")) is CollectionType
            else -> this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) is CollectionType
        }
    }

    fun isDatatype(value: Any): Boolean {
        return this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) is DataType
    }

    fun findCollectionTypeFor(value: Any): CollectionType? {
        //TODO: use qualified name when possible
        return when (value) {
            is List<*> -> this.findFirstByNameOrNull(SimpleName("List")) as CollectionType?
            is Set<*> -> this.findFirstByNameOrNull(SimpleName("Set")) as CollectionType?
            is Map<*, *> -> this.findFirstByNameOrNull(SimpleName("Map")) as CollectionType?
            is Collection<*> -> this.findFirstByNameOrNull(SimpleName("Collection")) as CollectionType?
            is Array<*> -> this.findFirstByNameOrNull(SimpleName("Array")) as CollectionType?
            else -> this.findFirstByNameOrNull(SimpleName(value::class.simpleName!!)) as CollectionType?
        }
    }

    fun checkPublicAndReflectable() : List<String> {
        val issues = mutableListOf<String>()
        for (ns in super.allNamespace) {
            when(ns.qualifiedName.value) {
                "kotlin" -> Unit //don't check kotlin namespace
                else -> {
                    for (t in ns.dataType) {
                        when {
                            //cls.reflect().exists -> Unit //OK
                            KotlinxReflect.registeredClasses.containsKey(t.qualifiedName.value) -> Unit // OK gegistered
                            else -> issues.add("Type '${t.qualifiedName}' is not registered with kotlinxReflect")
                        }
                    }
                }
            }
        }
        return issues
    }
}