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

package net.akehurst.language.types.builder

import net.akehurst.language.base.api.*
import net.akehurst.language.expressions.processor.PrimitiveExecutor
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

@DslMarker
annotation class TypeModelDslMarker

fun typesDomain(
    name: String,
    resolveImports: Boolean,
    namespaces: List<TypesNamespace> = listOf(StdLibDefault),
    init: TypeDomainBuilder.() -> Unit
): TypesDomain {
    val b = TypeDomainBuilder(SimpleName(name), resolveImports, namespaces)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeDomainBuilder(
    val name: SimpleName,
    private val resolveImports: Boolean,
    val namespaces: List<TypesNamespace>
) {
    val _domain = TypesDomainSimple(name).also { m ->
        namespaces.forEach {
            m.addNamespace(it)
        }
    }
    private val _assocBuilders = mutableListOf<AssociationBuilder>()

    fun namespace(qualifiedName: String, imports: List<String> = listOf(StdLibDefault.qualifiedName.value), init: TypeNamespaceBuilder.() -> Unit): TypesNamespace {
        val b = TypeNamespaceBuilder(QualifiedName(qualifiedName), imports.map { Import(it) })
        b.init()
        val (ns, assocBuilders) = b.build()
        _domain.addNamespace(ns)
        _assocBuilders.addAll(assocBuilders)
        return ns
    }

    fun build(): TypesDomain {
        if (resolveImports) {
            _domain.resolveImports()
        }
        _assocBuilders.forEach {
            it.build()
        }
        return _domain
    }
}

@TypeModelDslMarker
open class TypeNamespaceBuilder(
    val qualifiedName: QualifiedName,
    imports: List<Import>
) {

    protected open val _namespace: TypesNamespace = TypesNamespaceSimple(qualifiedName, import = imports)
    private val _typeReferences = mutableListOf<TypeInstanceArgBuilder>()
    private val _assocBuilders = mutableListOf<AssociationBuilder>()

    fun imports(vararg imports: String) {
        imports.forEach { _namespace.addImport(Import(it)) }
    }

    fun singleton(typeName: String): SingletonType =
        _namespace.findOwnedOrCreateSingletonTypeNamed(SimpleName(typeName))

    fun primitive(typeName: String): PrimitiveType =
        _namespace.findOwnedOrCreatePrimitiveTypeNamed(SimpleName(typeName))

    fun value(typeName: String, init: ValueTypeBuilder.() -> Unit = {}) {
        val b = ValueTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        b.build()
    }

    fun interface_(typeName: String, init: InterfaceTypeBuilder.() -> Unit = {}): InterfaceType {
        val b = InterfaceTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        return b.build()
    }

    fun enum(typeName: String, literals: List<String>): EnumType =
        _namespace.findOwnedOrCreateEnumTypeNamed(SimpleName(typeName), literals)

    fun collection(typeName: String, typeParams: List<String>): CollectionType =
        _namespace.findOwnedOrCreateCollectionTypeNamed(SimpleName(typeName)).also {
            (it.typeParameters as MutableList).addAll(typeParams.map { tp -> TypeParameterSimple(SimpleName(tp)) })
        }

    /**
     * create a list type of the indicated typeName
     * if typeName is not already defined, it will be defined as an DataType
     */
    /*
        fun listTypeOf(elementTypeName: String): TypeInstance {
            return collectionTypeOf(SimpleTypeModelStdLib.List.name, elementTypeName)
        }

        fun listSeparatedType(itemType: TypeDefinition, separatorType: TypeDefinition): TypeInstance {
            val collType = SimpleTypeModelStdLib.ListSeparated
            return collectionType(collType, listOf(itemType.instance(), separatorType.instance()))
        }

        fun listSeparatedTypeOf(itemTypeName: String, separatorTypeName: String): TypeInstance {
            val collType = SimpleTypeModelStdLib.ListSeparated
            val itemType = _namespace.findTypeNamed(itemTypeName) ?: _namespace.findOrCreateElementTypeNamed(itemTypeName)
            val separatorType = _namespace.findTypeNamed(separatorTypeName) ?: _namespace.findOrCreateElementTypeNamed(separatorTypeName)
            return collectionType(collType, listOf(itemType.instance(), separatorType.instance()))
        }
    */

    fun data(typeName: String, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val b = DataTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        val et = b.build()
        return et
    }

    fun union(typeName: String, init: SubtypeListBuilder.() -> Unit): UnionType {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.findOwnedOrCreateUnionTypeNamed(SimpleName(typeName)) { _ -> }
        stu.forEach { t.addAlternative(it) }
        return t
    }

    fun association(init: AssociationBuilder.() -> Unit) {
        val b = AssociationBuilder(_namespace, _typeReferences)
        b.init()
        _assocBuilders.add(b)
    }

    open fun build(): Pair<TypesNamespace, List<AssociationBuilder>> {
        return Pair(_namespace, _assocBuilders)
    }
}

@TypeModelDslMarker
class AssociationBuilder(
    protected val _namespace: TypesNamespace,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    companion object {
        data class AssocEnd(
            val endTypeName: String,
            val characteristics: Set<PropertyCharacteristic>,
            val endName: String,
            val isNullable: Boolean,
            val collectionTypeName: String?
        )
    }

    val CMP = PropertyCharacteristic.COMPOSITE
    val REF = PropertyCharacteristic.REFERENCE
    val VAL = PropertyCharacteristic.READ_ONLY
    val VAR = PropertyCharacteristic.READ_WRITE

    private val _ends = mutableListOf<AssocEnd>()

    fun end(
        possiblyQualifiedTypeName: String,
        characteristics: Set<PropertyCharacteristic>,
        endName: String,
        isNullable: Boolean = false,
        collectionTypeName: String? = null
    ) {
        _ends.add(AssocEnd(possiblyQualifiedTypeName, characteristics, endName, isNullable, collectionTypeName))
    }

    fun build(): List<PropertyDeclaration> {
        val assocEnds = mutableListOf<AssociationEnd>()
        for (i in _ends.indices) {
            val thisEnd = _ends[i]
            val endName = PropertyName(thisEnd.endName)
            val endType = _namespace.findTypeNamed(thisEnd.endTypeName.asPossiblyQualifiedName) as DataType?
                ?: error("The Association end '${thisEnd.endTypeName}' is not defined")
            val isNullable = thisEnd.isNullable
            val collectionTypeName = thisEnd.collectionTypeName?.asPossiblyQualifiedName
            val characteristics = thisEnd.characteristics
            val navigable = true //TODO
            val ae = AssociationEnd(endName, endType, isNullable, collectionTypeName, characteristics, navigable)
            assocEnds.add(ae)
        }
        val props = _namespace.findOrCreateAssociation(assocEnds)
        return props
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _namespace: TypesNamespace,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    protected abstract val _structuredType: StructuredType

    val CON = PropertyCharacteristic.CONSTRUCTOR
    val IDY = PropertyCharacteristic.IDENTITY

    val CMP = PropertyCharacteristic.COMPOSITE
    val REF = PropertyCharacteristic.REFERENCE

    val VAL = PropertyCharacteristic.READ_ONLY
    val VAR = PropertyCharacteristic.READ_WRITE

    val STR = PropertyCharacteristic.STORED
    val DER = PropertyCharacteristic.DERIVED

    fun propertyOf(
        characteristics: Set<PropertyCharacteristic>,
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        execution: KProperty<Any>? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.init()
        val targs = tab.build()
        val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), ti, characteristics).also {
            (it as PropertyDeclarationStored).execution = execution as KProperty1<Any, Any?>?
        }
    }

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, this._namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, emptyList(), isNullable), childIndex)

    fun propertyListTypeOf(propertyName: String, dataTypeName: String, nullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(propertyName, nullable, childIndex) {
            ref(dataTypeName)
        }

    fun propertyListType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgBuilder.() -> Unit): PropertyDeclaration {
        val collType = StdLibDefault.List.qualifiedName
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, collType, isNullable, _typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)
    }

    // ListSeparated
    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _namespace.findTypeNamed(itemTypeName.asPossiblyQualifiedName) ?: _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(itemTypeName))
        val separatorType = _namespace.findTypeNamed(itemTypeName.asPossiblyQualifiedName) ?: _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(separatorTypeName))
        val collType = StdLibDefault.ListSeparated
        val propType = collType.type(listOf(itemType.type().asTypeArgument, separatorType.type().asTypeArgument), isNullable)
        return property(propertyName, propType, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgBuilder.() -> Unit): PropertyDeclaration {
        val collType = StdLibDefault.ListSeparated.qualifiedName
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, collType, isNullable, _typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)

        // val propType = collType.type(listOf(itemType.type().asTypeArgument, separatorType.type().asTypeArgument), isNullable)
        // return property(propertyName, propType, childIndex)
    }

    // Tuple
    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgNamedBuilder.() -> Unit = {}): PropertyDeclaration {
        val tt = StdLibDefault.TupleType
        val b = TypeInstanceArgNamedBuilder(this._structuredType, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        val collType = StdLibDefault.List
        val t = collType.type(listOf(tti.asTypeArgument))
        return property(propertyName, t, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgNamedBuilder.() -> Unit): PropertyDeclaration {
        val tt = StdLibDefault.TupleType
        val b = TypeInstanceArgNamedBuilder(this._structuredType, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        return property(propertyName, tti, childIndex)
    }

    /*
        fun propertyUnionType(propertyName: String,  typeName: String, isNullable: Boolean, childIndex: Int, init: SubtypeListBuilder.() -> Unit): PropertyDeclaration {
            val b = SubtypeListBuilder(_namespace, _typeReferences)
            b.init()
            val stu = b.build()
            val t = _namespace.findOwnedOrCreateUnionType(SimpleName(typeName))
            stu.forEach { t.addAlternative(it) }
            return property(propertyName, t.type(emptyList(), isNullable), childIndex)
        }
    */
    fun propertyDataTypeOf(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val pqn = typeName.asPossiblyQualifiedName
        val t = _namespace.findTypeNamed(pqn)
            ?: _namespace.findOwnedOrCreateDataTypeNamed(pqn.simpleName)
        val ti = t.type(isNullable = isNullable)  //_namespace.createTypeInstance(null, elementTypeName.asPossiblyQualifiedName, emptyList(), isNullable)
        return property(propertyName, ti, childIndex)
    }

    fun propertyUnionTypeOf(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val pqn = typeName.asPossiblyQualifiedName
        val t = _namespace.findTypeNamed(pqn)
            ?: _namespace.findOwnedOrCreateUnionTypeNamed(pqn.simpleName) { ut ->
                // alternatives added later!
            }
        val ti = t.type(isNullable = isNullable)  //_namespace.createTypeInstance(null, elementTypeName.asPossiblyQualifiedName, emptyList(), isNullable)
        return property(propertyName, ti, childIndex)
    }

    fun propertyByEvaluation(
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        description:String = "",
        typeArguments: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.typeArguments()
        val btargs = tab.build()
        val targs = btargs
        val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        return _structuredType.appendPropertyPrimitive(PropertyName(propertyName), ti, description)
    }

    fun property(propertyName: String, typeUse: TypeInstance, childIndex: Int): PropertyDeclaration {
        check(childIndex >= _structuredType.property.size) { "Incorrect property index" }
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), typeUse, characteristics, childIndex)
    }
}

/*
@TypeModelDslMarker
class TupleTypeBuilder(
    protected val _namespace: TypeNamespace,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, this._namespace.createTypeInstance(_structuredType, typeName.asPossiblyQualifiedName, emptyList(), isNullable), childIndex)


    fun build(): TupleType {
        return _structuredType
    }
}
*/

@TypeModelDslMarker
class ValueTypeBuilder(
    _namespace: TypesNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateValueTypeNamed(_name) as ValueType
    override val _structuredType: StructuredType get() = _type

    /*
    fun supertypes(vararg superTypes: String) {
        superTypes.forEach {
            val pqn = it.asPossiblyQualifiedName
            val ti = _namespace.createTypeInstance(_type, pqn, emptyList(), false)
            _type.addSupertype(ti)
        }
    }
*/
    fun supertype(typeDeclarationName: String, init: TypeInstanceArgBuilder.() -> Unit = {}) {
        val pqn = typeDeclarationName.asPossiblyQualifiedName
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, pqn, false, _typeReferences)
        tb.init()
        val ti = tb.build()
        _type.addSupertype(ti)
    }

    fun constructor_(init: ConstructorBuilder.() -> Unit) {
        val b = ConstructorBuilder(_namespace, _type, _typeReferences)
        b.init()
        val params = b.build()
        (_type as ValueTypeSimple).addConstructor(params)
    }

    fun build(): ValueType {
        return _type
    }

}

@TypeModelDslMarker
class InterfaceTypeBuilder(
    _namespace: TypesNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateInterfaceTypeNamed(_name) as InterfaceType
    override val _structuredType: StructuredType get() = _type

    fun typeParameters(vararg parameters: String) {
        (_type.typeParameters as MutableList).addAll(parameters.map { TypeParameterSimple(SimpleName(it)) })
    }

    /*
        fun supertypes(vararg superTypes: String) {
            superTypes.forEach {
                val pqn = it.asPossiblyQualifiedName
                val ti = _namespace.createTypeInstance(_type, pqn, emptyList(), false)
                _type.addSupertype(ti)
            }
        }
    */
    fun supertype(typeDeclarationName: String, init: TypeInstanceArgBuilder.() -> Unit = {}) {
        val pqn = typeDeclarationName.asPossiblyQualifiedName
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, pqn, false, _typeReferences)
        tb.init()
        val ti = tb.build()
        _type.addSupertype(ti)
    }

    fun subtypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val pqn = it.asPossiblyQualifiedName
            val ti = _namespace.createTypeInstance(_type.qualifiedName, pqn, emptyList(), false)
            _type.addSubtype(ti)
            (_namespace.findTypeNamed(pqn) as DataType?)?.addSupertype_dep(_type.qualifiedName)
        }
    }

    fun build(): InterfaceType {
        return _type
    }

}

@TypeModelDslMarker
class DataTypeBuilder(
    _namespace: TypesNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateDataTypeNamed(_name) as DataType
    override val _structuredType: StructuredType get() = _type

    fun typeParameters(vararg parameters: String) {
        (_type.typeParameters as MutableList).addAll(parameters.map { TypeParameterSimple(SimpleName(it)) })
    }

    fun supertypes(vararg superTypes: String) {
        superTypes.forEach {
            val pqn = it.asPossiblyQualifiedName
            val ti = _namespace.createTypeInstance(_type.qualifiedName, pqn, emptyList(), false)
            _type.addSupertype(ti)
        }
    }

    fun supertype(typeDeclarationName: String, init: TypeInstanceArgBuilder.() -> Unit = {}) {
        val pqn = typeDeclarationName.asPossiblyQualifiedName
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, pqn, false, _typeReferences)
        tb.init()
        val ti = tb.build()
        _type.addSupertype(ti)
    }

    fun subtypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val pqn = it.asPossiblyQualifiedName
            val ti = _namespace.createTypeInstance(_type.qualifiedName, pqn, emptyList(), false)
            _type.addSubtype(ti)
        }
    }

    fun constructor_(init: ConstructorBuilder.() -> Unit) {
        val b = ConstructorBuilder(_namespace, _type, _typeReferences)
        b.init()
        val params = b.build()
        (_type as DataTypeSimple).addConstructor(params)
    }

    fun build(): DataType {
        return _type
    }

}

@TypeModelDslMarker
class ConstructorBuilder(
    val _namespace: TypesNamespace,
    private val _type: TypeDefinition,
    private val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    val CMP = PropertyCharacteristic.COMPOSITE
    val REF = PropertyCharacteristic.REFERENCE
    val VAL = PropertyCharacteristic.READ_ONLY
    val VAR = PropertyCharacteristic.READ_WRITE
    val DER = PropertyCharacteristic.DERIVED
    val STR = PropertyCharacteristic.STORED

    private val _paramList = mutableListOf<ParameterDeclaration>()

    fun parameter(name: String, typeName: String, nullable: Boolean = false) {
        val ty = _namespace.createTypeInstance(_type.qualifiedName, typeName.asPossiblyQualifiedName, emptyList(), nullable)
        _paramList.add(ParameterDefinitionSimple(TmParameterName(name), ty, null))
    }

    fun build(): List<ParameterDeclaration> = _paramList
}

@TypeModelDslMarker
class TypeInstanceArgBuilder(
    val context: TypeDefinition?,
    val _namespace: TypesNamespace,
    val possiblyQualifiedName: PossiblyQualifiedName,
    val nullable: Boolean,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    private val _args = mutableListOf<TypeArgument>()

    fun ref(possiblyQualifiedTypeDeclarationName: String) {
        val pqn = possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName
        val ti = when (pqn) {
            is QualifiedName -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, emptyList(), nullable)
            is SimpleName -> {
                val tp = context?.typeParameters?.firstOrNull { it.name == pqn }
                when {
                    null == tp || null == context -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, emptyList(), nullable)
                    else -> TypeParameterReference(context, tp.name)
                }
            }
        }
        _args.add(ti.asTypeArgument)
    }

    /*
        fun unnamedSuperTypeOf(vararg subtypeNames: String) {
            val subtypes = subtypeNames.map { _namespace.createTypeInstance(context, it.asPossiblyQualifiedName, emptyList(), false) }
            val t = _namespace.findOwnedOrCreateUnionType(subtypes)
            _args.add(t.type(emptyList(), nullable).asTypeArgument)
        }

        fun unnamedSuperTypeOf(init: SubtypeListBuilder.() -> Unit) {
            val b = SubtypeListBuilder(_namespace, _typeReferences)
            b.init()
            val stu = b.build()
            val t = _namespace.findOwnedOrCreateUnionType(stu)
            _args.add(t.type(emptyList(), nullable).asTypeArgument)
        }
    */
    fun build(): TypeInstance {
        return _namespace.createTypeInstance(context?.qualifiedName, this.possiblyQualifiedName, _args, nullable)
//        return type.type(_args, false)
    }
}

@TypeModelDslMarker
class TypeInstanceArgNamedBuilder(
    val context: TypeDefinition?,
    val _namespace: TypesNamespace,
    val type: TypeDefinition,
    val instanceIsNullable: Boolean,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    private val _args = mutableListOf<TypeArgumentNamed>()

    fun typeRef(name: String, typeName: String, isNullable: Boolean) {
        val t = _namespace.findTypeNamed(typeName.asPossiblyQualifiedName)?.type(emptyList(), isNullable)
            ?: _namespace.createTypeInstance(context?.qualifiedName, typeName.asPossiblyQualifiedName, emptyList(), isNullable)
        val ta = TypeArgumentNamedSimple(PropertyName(name), t)
        _args.add(ta)
    }

    /*
        fun unnamedSuperTypeOf(name: String, isNullable: Boolean, init: SubtypeListBuilder.() -> Unit) {
            val b = SubtypeListBuilder(_namespace, _typeReferences)
            b.init()
            val stu = b.build()
            val t = _namespace.findOwnedOrCreateUnionType(stu).type(emptyList(), isNullable)
            val ta = TypeArgumentNamedSimple(PropertyName(name), t)
            _args.add(ta)
        }
    */
    fun tupleType(name: String, isNullable: Boolean, init: TypeInstanceArgNamedBuilder.() -> Unit) {
        val tt = StdLibDefault.TupleType
        val b = TypeInstanceArgNamedBuilder(context, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        val ta = TypeArgumentNamedSimple(PropertyName(name), tti)
        _args.add(ta)
    }

    fun build(): TypeInstance {
        return type.type(_args, instanceIsNullable)
    }
}

@TypeModelDslMarker
class TypeArgumentBuilder(
    private val _context: TypeDefinition?,
    private val _namespace: TypesNamespace
) {
    private val list = mutableListOf<TypeArgument>()
    fun typeArgument(possiblyQualifiedTypeDeclarationName: String, nullable: Boolean = false, typeArguments: TypeArgumentBuilder.() -> Unit = {}) {
        val tab = TypeArgumentBuilder(_context, _namespace)
        tab.typeArguments()
        val typeArgs = tab.build()
        //val tref = _namespace.createTypeInstance(_context, qualifiedTypeName.asPossiblyQualifiedName, typeArgs, false)

        val pqn = possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName
        val ti = when (pqn) {
            is QualifiedName -> _namespace.createTypeInstance(_context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
            is SimpleName -> {
                val tp = _context?.typeParameters?.firstOrNull { it.name == pqn }
                when {
                    null == tp || null == _context -> _namespace.createTypeInstance(_context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
                    else -> TypeParameterReference(_context, tp.name)
                }
            }
        }
        list.add(ti.asTypeArgument)
    }

    fun build(): List<TypeArgument> {
        return list
    }
}

@TypeModelDslMarker
class SubtypeListBuilder(
    val _namespace: TypesNamespace,
    private val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    val _subtypeList = mutableListOf<TypeInstance>()

    fun typeRef(typeName: String, isNullable: Boolean = false) {
        val pqn = typeName.asPossiblyQualifiedName
        val ti = //_namespace.findTypeNamed(pqn)?.type() ?:
            _namespace.createTypeInstance(null, pqn, emptyList(), isNullable)
        _subtypeList.add(ti)
    }

    fun listType(isNullable: Boolean = false, init: TypeInstanceArgBuilder.() -> Unit) {
        val collType = StdLibDefault.List.qualifiedName
        val tb = TypeInstanceArgBuilder(null, this._namespace, collType, isNullable, _typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun listSeparatedType(isNullable: Boolean = false, init: TypeInstanceArgBuilder.() -> Unit) {
        val collType = StdLibDefault.ListSeparated.qualifiedName
        val tb = TypeInstanceArgBuilder(null, this._namespace, collType, isNullable, _typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun tupleType(isNullable: Boolean = false, init: TypeInstanceArgNamedBuilder.() -> Unit) {
        val tt = StdLibDefault.TupleType
        val b = TypeInstanceArgNamedBuilder(null, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        _subtypeList.add(tti)
    }

    /*
        fun unionTypeOf(vararg subtypeNames: String) {
            val sts = subtypeNames.map { _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(it))!! }
            val t = _namespace.findOwnedOrCreateUnionType(sts.map { it.type() })
            _subtypeList.add(t.type())
        }

        fun unnamedSuperType(isNullable: Boolean = false, init: SubtypeListBuilder.() -> Unit) {
            val b = SubtypeListBuilder(_namespace, _typeReferences)
            b.init()
            val stu = b.build()
            val t = _namespace.findOwnedOrCreateUnionType(stu)
            _subtypeList.add(t.type(emptyList(), isNullable))
        }
    */
    fun build(): List<TypeInstance> = _subtypeList
}