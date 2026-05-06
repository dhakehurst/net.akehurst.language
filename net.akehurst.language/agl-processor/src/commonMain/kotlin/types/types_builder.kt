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
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslMarker
annotation class TypeModelDslMarker

@JvmOverloads //ensure the Java has overloads using the default values
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

private fun String.asTypeParameterReferenceOrNewTypeInstance(namespace: TypesNamespace, typeDef: TypeDefinition?, typeArgs:List<TypeArgument> = emptyList(), nullable:Boolean = false): TypeInstance {
    val pqn = this.asPossiblyQualifiedName
    return when (pqn) {
        is QualifiedName -> namespace.createTypeInstance(typeDef?.qualifiedName, pqn, typeArgs, nullable)
        is SimpleName -> {
            val tp = typeDef?.typeParameters?.firstOrNull { it.name == pqn }
            when {
                null == tp || null == typeDef -> namespace.createTypeInstance(typeDef?.qualifiedName, pqn, typeArgs, nullable)
                else -> TypeParameterReference(typeDef, tp.name)
            }
        }
    }
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

    @JvmOverloads //ensure the Java has overloads using the default values
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
    imports: List<Import>,
    open val _namespace: TypesNamespaceAbstract = TypesNamespaceSimple(qualifiedName, import = imports) //TODO: open for GrammarTypeNamespaceBuilder, remove open when GrammarTypeNamespaceBuilder is gone
) {

    //protected open val _namespace = TypesNamespaceSimple(qualifiedName, import = imports)
    private val _typeReferences = mutableListOf<TypeInstanceArgBuilder>()
    private val _assocBuilders = mutableListOf<AssociationBuilder>()

    fun imports(vararg imports: String) {
        imports.forEach { _namespace.addImport(Import(it)) }
    }

    internal fun special(typeName: String, implementation: KClass<*>? = null) {
        _namespace.findOwnedOrCreateSpecialTypeNamed(SimpleName(typeName))
            .also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }
    }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun singleton(typeName: String, implementation: KClass<*>? = null): SingletonType =
        _namespace.findOwnedOrCreateSingletonTypeNamed(SimpleName(typeName))
            .also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun primitive(typeName: String, implementation: KClass<*>? = null, init: PrimitiveTypeBuilder.() -> Unit = {}): PrimitiveType {
        val bldr = PrimitiveTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        bldr.init()
        return bldr.build().also {
            (it as TypeDefinitionSimpleAbstract).implementation = implementation
        }
    }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun value(typeName: String, implementation: KClass<*>? = null, init: ValueTypeBuilder.() -> Unit = {}) {
        val b = ValueTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        b.build().also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }
    }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun interface_(typeName: String, implementation: KClass<*>? = null, init: InterfaceTypeBuilder.() -> Unit = {}): InterfaceType {
        val b = InterfaceTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        return b.build().also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }
    }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun enum(typeName: String, literals: List<String>, implementation: KClass<*>? = null, init: EnumTypeBuilder.() -> Unit = {}): EnumType {
        val b = EnumTypeBuilder(_namespace, SimpleName(typeName), literals)
        b.init()
        return b.build().also {
            //TODO: literals
            (it as TypeDefinitionSimpleAbstract).implementation = implementation
        }
//        _namespace.findOwnedOrCreateEnumTypeNamed(SimpleName(typeName), literals)
//            .also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }
    }

    @JvmOverloads //ensure the Java has overloads using the default values
    fun collection(typeName: String, typeParams: List<String>, implementation: KClass<*>? = null, init: CollectionTypeBuilder.() -> Unit = {}): CollectionType {
       val tParams = typeParams.map { tp -> TypeParameterSimple(SimpleName(tp)) }
        val b = CollectionTypeBuilder(_namespace, _typeReferences, SimpleName(typeName), tParams)
        b.init()
        return b.build().also {
            (it as TypeDefinitionSimpleAbstract).implementation = implementation
        }
//        _namespace.findOwnedOrCreateCollectionTypeNamed(SimpleName(typeName)).also {
//            (it as TypeDefinitionSimpleAbstract).implementation = implementation
//            (it.typeParameters as MutableList).addAll(typeParams.map { tp -> TypeParameterSimple(SimpleName(tp)) })
//        }
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

    @JvmOverloads //ensure the Java has overloads using the default values
    fun data(typeName: String, implementation: KClass<*>? = null, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val b = DataTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        val et = b.build().also { (it as TypeDefinitionSimpleAbstract).implementation = implementation }
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
            val collectionTypeName: String?,
            val byEvaluation: ((PropertyDeclaration) -> ((Any) -> Any?)?)? = null,
            val byEvaluationSuspend: ((PropertyDeclaration) -> (suspend (Any) -> Any?)?)? = null
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
        collectionTypeName: String? = null,
        byEvaluation: ((PropertyDeclaration) -> ((Any) -> Any?)?)? = null
    ) {
        _ends.add(AssocEnd(possiblyQualifiedTypeName, characteristics, endName, isNullable, collectionTypeName, byEvaluation))
    }

    fun endSuspend(
        possiblyQualifiedTypeName: String,
        characteristics: Set<PropertyCharacteristic>,
        endName: String,
        isNullable: Boolean = false,
        collectionTypeName: String? = null,
        byEvaluation: ((PropertyDeclaration) -> (suspend (Any) -> Any?)?)? = null
    ) {
        _ends.add(AssocEnd(possiblyQualifiedTypeName, characteristics, endName, isNullable, collectionTypeName, byEvaluationSuspend = byEvaluation))
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
            ae.byEvaluation = thisEnd.byEvaluation
            ae.byEvaluationSuspend = thisEnd.byEvaluationSuspend
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

    @JvmOverloads //ensure the Java has overloads using the default values
    fun propertyOf(
        characteristics: Set<PropertyCharacteristic>,
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        execution: KProperty1<*, *>? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.init()
        val targs = tab.build()
        //val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _structuredType, targs, isNullable)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), ti, characteristics).also {
            (it as PropertyDeclarationStored).execution = execution as KProperty1<Any, out Any?>?
        }
    }

    fun propertyPrimitive(
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        description: String = "",
        execution: ((self: Any?) -> Any?)? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.init()
        val targs = tab.build()
        //val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _structuredType, targs, isNullable)
        return _structuredType.appendPropertyPrimitive(PropertyName(propertyName), ti, description).also {
            (it as PropertyDeclarationPrimitive).execution = execution
        }
    }

    fun propertyPrimitiveSuspend(
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        description: String = "",
        execution: (suspend (self: Any?) -> Any?)? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.init()
        val targs = tab.build()
        //val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _structuredType, targs, isNullable)
        return _structuredType.appendPropertyPrimitive(PropertyName(propertyName), ti, description).also {
            (it as PropertyDeclarationPrimitive).executionSuspend = execution
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
        description: String = "",
        typeArguments: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.typeArguments()
        val btargs = tab.build()
        val targs = btargs
        //val ti = _namespace.createTypeInstance(_structuredType.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _structuredType, targs, isNullable)
        return _structuredType.appendPropertyPrimitive(PropertyName(propertyName), ti, description)
    }

    fun property(propertyName: String, typeUse: TypeInstance, childIndex: Int): PropertyDeclaration {
        check(childIndex >= _structuredType.property.size) { "Incorrect property index" }
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), typeUse, characteristics, childIndex)
    }

    fun methodPrimitive(
        methodName: String,
        returnTypeName: String,
        isNullable: Boolean = false,
        //returnTypeTypeArguments: TypeArgumentBuilder.() -> Unit = {},
        init: MethodParameterBuilder.() -> Unit = {}
    ): MethodDefinitionPrimitive {
        val pb = MethodParameterBuilder(_namespace, _structuredType, methodName, returnTypeName, isNullable)
        pb.init()
        return pb.build()
    }

    fun methodPrimitiveSuspend(
        methodName: String,
        returnTypeName: String,
        isNullable: Boolean = false,
        //returnTypeTypeArguments: TypeArgumentBuilder.() -> Unit = {},
        init: MethodParameterBuilder.() -> Unit = {}
    ): MethodDefinitionPrimitive {
        val bldr = MethodParameterBuilder(_namespace, _structuredType, methodName, returnTypeName, isNullable)
        bldr.init()
        return bldr.build()
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
class PrimitiveTypeBuilder(
    private val _namespace: TypesNamespace,
    private val _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) {

    private val _type = _namespace.findOwnedOrCreatePrimitiveTypeNamed(_name)

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
        val tb = TypeInstanceArgBuilder(this._type, this._namespace, pqn, false, _typeReferences)
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

    fun derivedPropertyOf(
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        execution: ((self: Any?) -> Any?)? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_type, _namespace)
        tab.init()
        val targs = tab.build()
        //val ti = _namespace.createTypeInstance(_type.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, targs, isNullable)
        return _type.appendPropertyDerived(PropertyName(propertyName), ti, "", "").also {
            (it as PropertyDeclarationDerived).execution = execution
        }
    }

    fun derivedPropertyOfSuspend(
        propertyName: String,
        typeName: String,
        isNullable: Boolean = false,
        execution: (suspend (self: Any?) -> Any?)? = null,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_type, _namespace)
        tab.init()
        val targs = tab.build()
       // val ti = _namespace.createTypeInstance(_type.qualifiedName, typeName.asPossiblyQualifiedName, targs, isNullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, targs, isNullable)
        return _type.appendPropertyDerived(PropertyName(propertyName), ti, "", "").also {
            (it as PropertyDeclarationDerived).executionSuspend = execution
        }
    }

    fun methodPrimitive(
        methodName: String,
        returnTypeName: String,
        isNullable: Boolean = false,
       // returnTypeTypeArguments: TypeArgumentBuilder.() -> Unit = {},
        init: MethodParameterBuilder.() -> Unit = {}
    ): MethodDefinitionPrimitive {
        val pb = MethodParameterBuilder(_namespace, _type, methodName, returnTypeName, isNullable)
        pb.init()
        return pb.build()
    }

    fun methodPrimitiveSuspend(
        methodName: String,
        returnTypeName: String,
        isNullable: Boolean = false,
       // returnTypeTypeArguments: TypeArgumentBuilder.() -> Unit = {},
        init: MethodParameterBuilder.() -> Unit = {}
    ): MethodDefinitionPrimitive {
        val bldr = MethodParameterBuilder(_namespace, _type, methodName, returnTypeName, isNullable)
        bldr.init()
        return bldr.build()
    }

    fun build(): PrimitiveType {
        return _type
    }

}


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
            val supType = _namespace.findTypeNamed(_type.qualifiedName)!!.type()
            (_namespace.findTypeNamed(pqn) as DataType?)?.addSupertype(supType)
        }
    }

    fun build(): InterfaceType {
        return _type
    }

}

@TypeModelDslMarker
class EnumTypeBuilder(
    _namespace: TypesNamespace,
    _name: SimpleName,
    _literals: List<String>
) {
    private val _type = _namespace.findOwnedOrCreateEnumTypeNamed(_name, _literals) as EnumType

    fun build(): EnumType {
        return _type
    }
}

@TypeModelDslMarker
class CollectionTypeBuilder(
    _namespace: TypesNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName,
    _typeParams: List<TypeParameter>
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateCollectionTypeNamed(_name, _typeParams)
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

    fun constructor_(init: ConstructorBuilder.() -> Unit) {
        val b = ConstructorBuilder(_namespace, _type, _typeReferences)
        b.init()
        val params = b.build()
        (_type as DataTypeSimple).addConstructor(params)
    }

    fun build(): CollectionType {
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

    fun parameter(characteristic: Set<PropertyCharacteristic>, name: String, typeName: String, nullable: Boolean = false, propertyExecution:KProperty1<*, *>? = null, typeArguments: TypeArgumentBuilder.() -> Unit = {}) {
        val tab = TypeArgumentBuilder(_type, _namespace)
        tab.typeArguments()
        val targs = tab.build()
        //val ty = _namespace.createTypeInstance(_type.qualifiedName, typeName.asPossiblyQualifiedName, targs, nullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, targs, nullable)
        _paramList.add(ParameterDefinitionSimple(TmParameterName(name), ti, null))
        if (characteristic.isNotEmpty()) {
            when(_type) {
                is DataType, is ValueType -> _type.appendPropertyStored(PropertyName(name),ti, characteristic+ PropertyCharacteristic.CONSTRUCTOR)
            }
        }
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

    fun ref(possiblyQualifiedTypeDeclarationName: String, init: TypeInstanceArgBuilder.() -> Unit = {}) {
        val pqn = possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName
        val tb = TypeInstanceArgBuilder(context, _namespace, pqn, false, _typeReferences)
        tb.init()
        val ti = tb.build()

//        val ti = when (pqn) {
//            is QualifiedName -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, emptyList(), nullable)
//            is SimpleName -> {
//                val tp = context?.typeParameters?.firstOrNull { it.name == pqn }
//                when {
//                    null == tp || null == context -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, emptyList(), nullable)
//                    else -> TypeParameterReference(context, tp.name)
//                }
//            }
//        }
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
        return when (possiblyQualifiedName) {
            is QualifiedName -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedName, _args, nullable)
            is SimpleName -> {
                val tp = context?.typeParameters?.firstOrNull { it.name == possiblyQualifiedName }
                when {
                    null == tp -> _namespace.createTypeInstance(context?.qualifiedName, possiblyQualifiedName, _args, nullable)
                    else -> TypeParameterReference(context, tp.name)
                }
            }
        }

 //       return _namespace.createTypeInstance(context?.qualifiedName, this.possiblyQualifiedName, _args, nullable)
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

        val ti = possiblyQualifiedTypeDeclarationName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _context, typeArgs, nullable)

//        val pqn = possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName
//        val ti = when (pqn) {
//            is QualifiedName -> _namespace.createTypeInstance(_context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
//            is SimpleName -> {
//                val tp = _context?.typeParameters?.firstOrNull { it.name == pqn }
//                when {
//                    null == tp || null == _context -> _namespace.createTypeInstance(_context?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
//                    else -> TypeParameterReference(_context, tp.name)
//                }
//            }
//        }
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

@TypeModelDslMarker
class MethodParameterBuilder(
    val _namespace: TypesNamespace,
    private val _type: TypeDefinition,
     val methodName: String,
    private val _returnTypeName: String,
    private val _returnTypeIsNullable: Boolean
) {
    private var _description: String = ""
    private val _returnTypeArgs = mutableListOf<TypeArgument>()
    private val _paramList = mutableListOf<ParameterDeclaration>()
    private var _execution: ((self: Any, args: List<*>) -> Any?)? = null
    private var _executionSuspend: (suspend (self: Any, args: List<*>) -> Any?)? = null

    fun returnTypeArgument(possiblyQualifiedTypeDeclarationName: String, nullable: Boolean = false, typeArguments: TypeArgumentBuilder.() -> Unit = {}) {
        val tab = TypeArgumentBuilder(_type, _namespace)
        tab.typeArguments()
        val typeArgs = tab.build()
        //val tref = _namespace.createTypeInstance(_context, qualifiedTypeName.asPossiblyQualifiedName, typeArgs, false)

        val ti = possiblyQualifiedTypeDeclarationName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, typeArgs, nullable)
//        val pqn = possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName
//        val ti = when (pqn) {
//            is QualifiedName -> _namespace.createTypeInstance(_type?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
//            is SimpleName -> {
//                val tp = _type?.typeParameters?.firstOrNull { it.name == pqn }
//                when {
//                    null == tp || null == _type -> _namespace.createTypeInstance(_type?.qualifiedName, possiblyQualifiedTypeDeclarationName.asPossiblyQualifiedName, typeArgs, nullable)
//                    else -> TypeParameterReference(_type, tp.name)
//                }
//            }
//        }
        _returnTypeArgs.add(ti.asTypeArgument)
    }

    fun parameter(name: String, typeName: String, nullable: Boolean = false, typeArguments: TypeArgumentBuilder.() -> Unit = {}) {
        val tab = TypeArgumentBuilder(_type, _namespace)
        tab.typeArguments()
        val targs = tab.build()
        //val ty = _namespace.createTypeInstance(_type.qualifiedName, typeName.asPossiblyQualifiedName, targs, nullable)
        val ti = typeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, targs, nullable)
        _paramList.add(ParameterDefinitionSimple(TmParameterName(name), ti, null))
    }

    fun description(value: String) {
        _description = value
    }

    fun execution(value: (self: Any, args: List<*>) -> Any?) {
        _execution = value
    }

    fun executionSuspend(value: suspend (self: Any, args: List<*>) -> Any?) {
        _executionSuspend = value
    }

    fun build(): MethodDefinitionPrimitive {
        val rti = _returnTypeName.asTypeParameterReferenceOrNewTypeInstance(_namespace, _type, _returnTypeArgs, _returnTypeIsNullable)
//        val ti = _namespace.createTypeInstance(
//            _type.qualifiedName,
//            _returnTypeName.asPossiblyQualifiedName,
//            _returnTypeArgs,
//            _returnTypeIsNullable
//        )

        return _type.appendMethodPrimitive(
            MethodName(methodName),
            _paramList,
            rti,
            _description
        ).also {
            (it as MethodDefinitionAbstract).execution = _execution
            (it as MethodDefinitionAbstract).executionSuspend = _executionSuspend
        }
    }
}