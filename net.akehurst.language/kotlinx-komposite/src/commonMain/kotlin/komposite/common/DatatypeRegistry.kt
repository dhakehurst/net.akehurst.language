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
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeModelSimpleAbstract
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.reflect.KClass

class DatatypeRegistry : TypeModelSimpleAbstract() {

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
                collection EmptySet
            }
        """.trimIndent()

        val KOTLIN_STD_MODEL = typeModel("kotlin-std",false, emptyList()) {
            namespace("kotlin", emptyList()) {
                primitive("Boolean")
                primitive("Byte")
                primitive("Short")
                primitive("Int")
                primitive("Long")
                primitive("Float")
                primitive("Double")
                primitive("String")
//                data("Pair") {
//                    constructor_ {
//                        parameter("first", "Any", false)
//                        parameter("second", "Any", false)
//                    }
//                }
                data("Any") {

                }
            }
            namespace("kotlin.collections", emptyList()) {
                collection("Array", listOf("E"))
                collection("Collection", listOf("E"))
                collection("List", listOf("E")).also { it.addSupertype_dep("Collection".asPossiblyQualifiedName) }
                collection("Set", listOf("E")).also { it.addSupertype_dep("Collection".asPossiblyQualifiedName) }
                collection("Map", listOf("K", "V"))
                collection("EmptySet", emptyList()).also { it.addSupertype_dep("Set".asPossiblyQualifiedName) }
                collection("EmptyList", emptyList()).also { it.addSupertype_dep("List".asPossiblyQualifiedName) }
                //TODO: need a java -> kotlin name mapping really, this is class java.util.SingletonList
                collection("SingletonList", emptyList()).also { it.addSupertype_dep("List".asPossiblyQualifiedName) }
                collection("HashSet", emptyList()).also { it.addSupertype_dep("Set".asPossiblyQualifiedName) }
                collection("LinkedHashSet", emptyList()).also { it.addSupertype_dep("Set".asPossiblyQualifiedName) }
            }
        }
        //val TypeDeclaration.isKotlinArray get() = this.qualifiedName.value=="kotlin.collections.Array"
        //val TypeDeclaration.isKotlinList get() = this.qualifiedName.value=="kotlin.collections.List"
        //val TypeDeclaration.isKotlinSet get() = this.qualifiedName.value=="kotlin.collections.Set"
        //val TypeDeclaration.isKotlinMap get() = this.qualifiedName.value=="kotlin.collections.Map"

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

        val KOTLIN_TO_AGL = mapOf(
            "kotlin.Any" to StdLibDefault.AnyType.qualifiedTypeName.value,
            "kotlin.Boolean" to StdLibDefault.Boolean.qualifiedTypeName.value,
            "kotlin.String" to StdLibDefault.String.qualifiedTypeName.value,
            "kotlin.Int" to StdLibDefault.Integer.qualifiedTypeName.value,
            "kotlin.Double" to StdLibDefault.Real.qualifiedTypeName.value,
            "kotlin.Float" to StdLibDefault.Real.qualifiedTypeName.value,
            "kotlin.Pair" to StdLibDefault.Pair.qualifiedName.value,
            "kotlin.collections.Collection" to StdLibDefault.Collection.qualifiedName.value,
            "kotlin.collections.List" to StdLibDefault.List.qualifiedName.value,
            "kotlin.collections.Set" to StdLibDefault.Set.qualifiedName.value,
            "java.util.HashSet" to StdLibDefault.Set.qualifiedName.value,
            "java.util.LinkedHashSet" to StdLibDefault.Set.qualifiedName.value,
            "net.akehurst.language.collections.OrderedSet" to StdLibDefault.OrderedSet.qualifiedName.value,
            "kotlin.collections.Map" to StdLibDefault.Map.qualifiedName.value,
            "java.util.HashMap" to StdLibDefault.Map.qualifiedName.value,
            "java.util.LinkedHashMap" to StdLibDefault.Map.qualifiedName.value,
            "java.lang.Exception" to StdLibDefault.Exception.qualifiedTypeName.value,
            "java.lang.RuntimeException" to StdLibDefault.Exception.qualifiedTypeName.value,
            "kotlin.Throwable" to StdLibDefault.Exception.qualifiedTypeName.value,
            "kotlin.Function1" to StdLibDefault.Lambda.qualifiedTypeName.value,
            "kotlin.Function2" to StdLibDefault.Lambda.qualifiedTypeName.value,
            "kotlin.Function3" to StdLibDefault.Lambda.qualifiedTypeName.value,
            "kotlin.Function4" to StdLibDefault.Lambda.qualifiedTypeName.value,
            "kotlin.Function5" to StdLibDefault.Lambda.qualifiedTypeName.value,
            "kotlin.Function6" to StdLibDefault.Lambda.qualifiedTypeName.value,
        )
    }

    override val name: SimpleName = SimpleName("registry")
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap())

    private val _primitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*, *>>()

    fun registerPrimitiveMapper(mapper: PrimitiveMapper<*, *>) {
        this._primitiveMappers[mapper.primitiveKlass] = mapper
    }

    fun registerFromAglTypesString(kompositeModel: String, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        try {
            val result = Agl.registry.agl.types.processor!!.process(kompositeModel)
            if (null == result.asm) {
                throw KompositeException("Error processing config string", result.allIssues.errors, null)
            } else {
                this.registerFromTypeModel(result.asm!!, primitiveMappers)
            }
        } catch (e: Exception) {
            throw KompositeException("Error trying to register datatypes from config string - ${e.message}", e)
        }
    }

    fun registerFromTypeModel(typeModel: TypeModel, primitiveMappers: Map<KClass<*>, PrimitiveMapper<*, *>>) {
        this._primitiveMappers.putAll(primitiveMappers)
        typeModel.namespace.forEach {
                this.addNamespace(it)
        }
    }

    fun findTypeDeclarationByKClass(cls: KClass<*>): TypeDefinition? {
        //TODO: use qualified name when possible (i.e. when JS reflection supports qualified names)
        //val qname = cls.qualifiedName ?: error("class does not have a qualifiedName!")
        //return this.findByQualifiedNameOrNull(QualifiedName( qname))

        //TODO: use KOTLIN_TO_AGL name mapping
        val qname = cls.simpleName ?: error("class does not have a simple name!")
        return this.findFirstDefinitionByNameOrNull(SimpleName( qname))
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
        return this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) is SingletonType
    }

    fun isPrimitive(value: Any): Boolean {
        return this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) is PrimitiveType
    }

    fun isEnum(value: Any): Boolean {
        return this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) is EnumType
    }

    fun isCollection(value: Any): Boolean {
        //TODO: use type hierachy so we can e.g. register List rather than ArrayList
        return when (value) {
            is List<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("List")) is CollectionType
            is Set<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Set")) is CollectionType
            is Map<*, *> -> this.findFirstDefinitionByNameOrNull(SimpleName("Map")) is CollectionType
            is Collection<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Collection")) is CollectionType
            is Array<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Array")) is CollectionType
            else -> this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) is CollectionType
        }
    }

    fun isDatatype(value: Any): Boolean {
        return this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) is DataType
    }

    fun findCollectionTypeFor(value: Any): CollectionType? {
        //TODO: use qualified name when possible
        return when (value) {
            is List<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("List")) as CollectionType?
            is Set<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Set")) as CollectionType?
            is Map<*, *> -> this.findFirstDefinitionByNameOrNull(SimpleName("Map")) as CollectionType?
            is Collection<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Collection")) as CollectionType?
            is Array<*> -> this.findFirstDefinitionByNameOrNull(SimpleName("Array")) as CollectionType?
            else -> this.findFirstDefinitionByNameOrNull(SimpleName(value::class.simpleName!!)) as CollectionType?
        }
    }

    fun checkPublicAndReflectable() : List<String> {
        val issues = mutableListOf<String>()
        for (ns in super.namespace) {
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