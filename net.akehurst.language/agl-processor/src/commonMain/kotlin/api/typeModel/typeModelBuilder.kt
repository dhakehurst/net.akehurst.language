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

package net.akehurst.language.api.typemodel

import net.akehurst.language.agl.agl.typemodel.TypeModelSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import kotlin.reflect.KClass

@DslMarker
annotation class TypeModelDslMarker

fun typeModel(namespace: String, name: String, init: TypeModelBuilder.() -> Unit): TypeModel {
    val b = TypeModelBuilder(namespace, name)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder(
    val namespace: String,
    val name: String
) {

    private val _model = TypeModelSimple(namespace, name)
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()


    /*
    private val _types = mutableMapOf<String, RuleType>()
    private fun findOrCreateType(grammarRuleName: String, typeName: String): ElementType {
        val existing = _types[grammarRuleName]
        return if (null == existing) {
            val t = ElementType(_model, typeName)
            _types[grammarRuleName] = t
            t
        } else {
            existing as ElementType
        }
    }

    object : TypeModel {
        override val namespace: String = this@TypeModelBuilder.namespace
        override val name: String = this@TypeModelBuilder.name
        override val allTypes = _types
        override fun findTypeForRule(ruleName: String): RuleType = findOrCreateType(ruleName)
        override fun findTypeNamed(typeName: String): RuleType? = _types.values.firstOrNull { it.name == typeName }
    }
*/
    fun stringTypeFor(name: String, isNullable: Boolean = false) {
        _model.addTypeFor(name, if (isNullable) StringType.useNullable else StringType.use)
    }

    fun listTypeFor(name: String, elementType: RuleType): TypeUsage {
        val t = ListSimpleType.ofType(TypeUsage.ofType(elementType))
        _model.addTypeFor(name, t)
        return t
    }

    fun listTypeOf(name: String, elementTypeName: String): TypeUsage {
        val elementType = _model.findOrCreateTypeNamed(elementTypeName)!!
        return listTypeFor(name, elementType)
    }

    fun listSeparatedTypeFor(name: String, itemType: TypeUsage, separatorType: TypeUsage) {
        val t = ListSeparatedType.ofType(itemType, separatorType)
        _model.addTypeFor(name, t)
    }

    fun listSeparatedTypeFor(name: String, itemType: RuleType, separatorType: RuleType) =
        listSeparatedTypeFor(name, TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType))

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorType: RuleType) {
        val itemType = _model.findOrCreateTypeNamed(itemTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorTypeName: String) {
        val itemType = _model.findOrCreateTypeNamed(itemTypeName)!!
        val separatorType = _model.findOrCreateTypeNamed(separatorTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun elementType(grammarRuleName: String, typeName: String, init: ElementTypeBuilder.() -> Unit = {}): ElementType {
        val b = ElementTypeBuilder(_model, _typeReferences, typeName)
        b.init()
        val et = b.build()
        _model.addTypeFor(grammarRuleName, TypeUsage.ofType(et))
        return et
    }

    fun unnamedSuperTypeTypeFor(name: String, subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _model.findOrCreateTypeNamed(it)!!
                is RuleType -> it
                else -> error("Cannot map to RuleType: $it")
            }
        }
        val t = UnnamedSuperTypeType(sts.map { TypeUsage.ofType(it) }, false)
        _model.addTypeFor(name, TypeUsage.ofType(t))
        return t
    }

    fun build(): TypeModel {
        _typeReferences.forEach {
            it.resolve()
        }
        return _model
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _model: TypeModelSimple,
    private val _typeReferences: MutableList<TypeUsageReferenceBuilder>
) {
    protected abstract val _structuredType: StructuredRuleType

    fun propertyAnyTypeUnnamed(isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, TypeUsage.ofType(AnyType, emptyList(), isNullable), childIndex)

    fun propertyStringTypeUnnamed(isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, TypeUsage.ofType(StringType, emptyList(), isNullable), childIndex)

    fun propertyStringType(propertyName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, TypeUsage.ofType(StringType, emptyList(), isNullable), childIndex)

    // ListSimple
    /*
    fun propertyListTypeUnnamedOf(elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val elementType = _model.findOrCreateTypeNamed(elementTypeName)!!
        return propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, elementType, isNullable, childIndex)
    }

    fun propertyListTypeUnnamed(elementType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, elementType, isNullable, childIndex)
    */
    fun propertyListTypeUnnamedOfUnnamedSuperTypeType(subtypeNames: List<String>, childIndex: Int): PropertyDeclaration =
        propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, false, childIndex) {
            unnamedSuperTypeOf(*subtypeNames.toTypedArray())
        }


    fun propertyListTypeOf(propertyName: String, elementTypeName: String, nullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(propertyName, nullable, childIndex) {
            elementTypeOf(elementTypeName)
        }

    fun propertyListType(propertyName: String, nullable: Boolean, childIndex: Int, init: TypeUsageReferenceBuilder.() -> Unit): PropertyDeclaration {
        val tb = TypeUsageReferenceBuilder(this._model, ListSimpleType, nullable)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)
    }

    // ListSeparated
    fun propertyUnnamedListSeparatedType(itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, ListSeparatedType.ofType(TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType), isNullable), childIndex)

    fun propertyUnnamedListSeparatedTypeOf() {
        TODO()
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findOrCreateTypeNamed(itemTypeName)!!
        val separatorType = _model.findOrCreateTypeNamed(separatorTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findOrCreateTypeNamed(itemTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, ListSeparatedType.ofType(TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType), isNullable), childIndex)

    // Tuple
    fun propertyTupleTypeUnnamed(isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration =
        propertyTupleType(TypeModelFromGrammar.UNNAMED_GROUP_PROPERTY_NAME, isNullable, childIndex, init)

    fun propertyListUnnamedOfTupleType(isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}) =
        propertyListOfTupleType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, isNullable, childIndex, init)

    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}): PropertyDeclaration {
        val b = TupleTypeBuilder(_model, _typeReferences)
        b.init()
        val tt = b.build()
        val t = ListSimpleType.ofType(TypeUsage.ofType(tt))
        return property(propertyName, t, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration {
        val b = TupleTypeBuilder(_model, _typeReferences)
        b.init()
        val tt = b.build()
        return property(propertyName, TypeUsage.ofType(tt), childIndex)
    }

    //
    fun propertyElementTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val t = _model.findOrCreateTypeNamed(elementTypeName)!!
        return property(propertyName, TypeUsage.ofType(t, emptyList(), isNullable), childIndex)
    }

    fun property(propertyName: String, typeUse: TypeUsage, childIndex: Int): PropertyDeclaration {
        return PropertyDeclaration(_structuredType, propertyName, typeUse, childIndex)
    }
}

@TypeModelDslMarker
class TupleTypeBuilder(
    _model: TypeModelSimple,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>
) : StructuredTypeBuilder(_model, _typeReferences) {

    override val _structuredType = TupleType()

    fun build(): TupleType {
        return _structuredType
    }
}

@TypeModelDslMarker
class ElementTypeBuilder(
    _model: TypeModelSimple,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>,
    _elementName: String
) : StructuredTypeBuilder(_model, _typeReferences) {

    private val _elementType = _model.findOrCreateTypeNamed(_elementName) as ElementType
    override val _structuredType: StructuredRuleType get() = _elementType

    //fun superType(superTypeName: String) {
    //    val st = _model.findOrCreateType(superTypeName)
    //    _elementType.addSuperType(st as ElementType)
    //}

    fun subTypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val st = _model.findOrCreateTypeNamed(it) as ElementType
            st.addSuperType(_elementType)
        }
    }

    fun build(): ElementType {
        return _elementType
    }

}

class TypeUsageReferenceBuilder(
    val _model: TypeModelSimple,
    val type: RuleType,
    val nullable: Boolean
) {
    private val _refs = mutableListOf<String>()
    private val _args = mutableListOf<TypeUsage>()
    private lateinit var _refClass: KClass<*>

    fun stringType() {
        _refClass = StringType::class
    }

    fun elementTypeOf(typeName: String) {
        _refClass = ElementType::class
        _refs.add(typeName)
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        _refClass = UnnamedSuperTypeType::class
        _refs.addAll(subtypeNames)
    }

    fun build(): TypeUsage {
        return TypeUsage.ofType(type, _args, false)
    }

    fun resolve() {
        when (_refClass) {
            StringType::class -> {
                _args.add(TypeUsage.ofType(StringType, emptyList(), nullable))
            }

            ElementType::class -> {
                val ref = _refs.first()
                val t = _model.allTypesByName[ref] ?: error("Type not found: '$ref'")
                _args.add(TypeUsage.ofType(t, emptyList(), nullable))
            }

            UnnamedSuperTypeType::class -> {
                val subtypes = _refs.map { _model.allTypesByName[it] ?: error("Type not found: '$it'") }
                val stu = subtypes.map { TypeUsage.ofType(it) }
                val t = UnnamedSuperTypeType(stu, false)
                _args.add(TypeUsage.ofType(t, emptyList(), nullable))
            }

            else -> error("Not handled: ${_refClass.simpleName}")
        }
    }
}