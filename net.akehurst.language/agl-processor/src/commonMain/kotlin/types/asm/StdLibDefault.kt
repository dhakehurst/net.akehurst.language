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

package net.akehurst.language.types.asm

import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.*
import net.akehurst.language.types.builder.TypeNamespaceBuilder
import net.akehurst.language.types.builder.typesDomain
import kotlin.time.Instant

object StdLibDefault : TypesNamespaceAbstract(OptionHolderDefault(null, emptyMap()), emptyList()) {

    override val qualifiedName: QualifiedName = QualifiedName("std")

    private val LambdaType_typeName = SimpleName("LambdaType")
    private val TupleType_typeName = SimpleName("TupleType")
    private val Collection_typeName = SimpleName("Collection")
    private val List_typeName = SimpleName("List")
    private val ListSeparated_typeName = SimpleName("ListSeparated")
    private val Set_typeName = SimpleName("Set")
    private val OrderedSet_typeName = SimpleName("OrderedSet")
    private val Map_typeName = SimpleName("Map")

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Any")).type()
    val NothingType = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Nothing")).type()
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("String")).type()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Boolean")).type()

    /**
     * 64 bit Integer (Long)
     */
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Integer")).type()

    /**
     * 64 bit Real (Double)
     */
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Real")).type()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Timestamp")).type()
    val Exception = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Exception")).type()

    val Pair = super.findOwnedOrCreateDataTypeNamed(SimpleName("Pair")).also { td ->
        (td.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("F")))
        (td.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("S")))
        (td as DataTypeSimple).addConstructor(
            listOf(
                ParameterDefinitionSimple(TmParameterName("first"), this.createTypeInstance(td.qualifiedName, SimpleName("F")), null),
                ParameterDefinitionSimple(TmParameterName("second"), this.createTypeInstance(td.qualifiedName, SimpleName("S")), null),
            )
        )
        td.appendPropertyStored(PropertyName("first"), TypeParameterReference(td, SimpleName("F")), setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.COMPOSITE), 0)
        td.appendPropertyStored(PropertyName("second"), TypeParameterReference(td, SimpleName("S")), setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.COMPOSITE), 1)
    }

    val Lambda = super.findOwnedOrCreateSpecialTypeNamed(LambdaType_typeName).type()

    val TupleType = TupleTypeSimple(this, TupleType_typeName)

    val Collection = super.findOwnedOrCreateCollectionTypeNamed(Collection_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))

    }

    val List: CollectionType = super.findOwnedOrCreateCollectionTypeNamed(List_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    // ListSeparated<I,S>
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed(ListSeparated_typeName).also { typeDecl ->
        typeDecl.addSupertype(List.type(listOf(AnyType.asTypeArgument)))
        (typeDecl.typeParameters as MutableList).addAll(listOf(TypeParameterSimple(SimpleName("I")), TypeParameterSimple(SimpleName("S"))))
    }

    // Set<E>
    val Set = super.findOwnedOrCreateCollectionTypeNamed(Set_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed(OrderedSet_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    val Map = super.findOwnedOrCreateCollectionTypeNamed(Map_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).addAll(listOf(TypeParameterSimple(SimpleName("K")), TypeParameterSimple(SimpleName("V"))))
        typeDecl.addSupertype(
            Collection.type(
                listOf(
                    Pair.type(
                        listOf(
                            TypeParameterReference(typeDecl, SimpleName("K")).asTypeArgument,
                            TypeParameterReference(typeDecl, SimpleName("V")).asTypeArgument
                        )
                    ).asTypeArgument
                )
            )
        )
    }

    override fun findInOrCloneTo(other: TypesDomain): TypesNamespace = this

    init {
        createProperties()
        createMethods()
    }

    private fun createProperties() {
        createPropertiesForCollection()
        createPropertiesForList()
        createPropertiesForListSeparated()
    }

    private fun createMethods() {
        createMethodsForString()
        createMethodsForCollection()
        createMethodsForList()
    }

    private fun createMethodsForString() {
        val typeDecl = String.resolvedDeclaration
        typeDecl.appendMethodPrimitive(MethodName("toBoolean"), emptyList(), Boolean, "Convert this String to a Boolean value. 'true' is true, 'false' is false, other values are \$nothing.")
        typeDecl.appendMethodPrimitive(MethodName("toInteger"), emptyList(), Integer, "Convert this String to an Integer value.")
        typeDecl.appendMethodPrimitive(MethodName("toReal"), emptyList(), Real, "Convert this String to a Real value.")
        typeDecl.appendMethodPrimitive(
            MethodName("removeSurrounding"),
            listOf(ParameterDefinitionSimple(TmParameterName("delimiter"), Integer, null)),
            String,
            "Remove the given string value from the start and end of this string, if present."
        )
    }

    private fun createPropertiesForCollection() {
        val typeDecl = Collection
        typeDecl.appendPropertyPrimitive(PropertyName("size"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), "Number of elements in the Collection.")
        typeDecl.appendPropertyPrimitive(PropertyName("isEmpty"), this.createTypeInstance(typeDecl.qualifiedName, Boolean.typeName), "True if the Collection has no elements.")
        typeDecl.appendPropertyPrimitive(PropertyName("isNotEmpty"), this.createTypeInstance(typeDecl.qualifiedName, Boolean.typeName), "True if the Collection has some elements.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("asMap"),
            this.createTypeInstance(
                typeDecl.qualifiedName, QualifiedName("std.Map"),
                listOf(
                    AnyType.asTypeArgument, //TODO: should be type of Pair.first
                    AnyType.asTypeArgument //TODO: should be type of Pair.second
                ),
                false
            ),
            "A Map object with elements being the Pairs of this List."
        )
    }

    private fun createPropertiesForList() {
        val typeDecl = List
        typeDecl.appendPropertyPrimitive(PropertyName("first"), TypeParameterReference(typeDecl, SimpleName("E")), "First element in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("last"), TypeParameterReference(typeDecl, SimpleName("E")), "Last element in the list.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("back"),
            this.createTypeInstance(typeDecl.qualifiedName, List_typeName, listOf(TypeParameterReference(typeDecl, SimpleName("E")).asTypeArgument)),
            "All elements in the List except the first one."
        )
        typeDecl.appendPropertyPrimitive(
            PropertyName("front"),
            this.createTypeInstance(typeDecl.qualifiedName, List_typeName, listOf(TypeParameterReference(typeDecl, SimpleName("E")).asTypeArgument)),
            "All elements in the List except the last one."
        )
        typeDecl.appendPropertyPrimitive(PropertyName("join"), this.createTypeInstance(typeDecl.qualifiedName, String.typeName), "The String value of all elements concatenated.")
    }

    private fun createMethodsForCollection() {
        val typeDecl = Collection
        typeDecl.appendMethodPrimitive(
            MethodName("get"),
            listOf(ParameterDefinitionSimple(TmParameterName("index"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), null)),
            TypeParameterReference(typeDecl, SimpleName("E")),
            "The element at the given index."
        )
        typeDecl.appendMethodPrimitive(
            MethodName("separate"),
            emptyList(),
            this.createTypeInstance(
                typeDecl.qualifiedName, ListSeparated_typeName, listOf(
                    AnyType.asTypeArgument, //TODO: this should be type provided by method typeargs
                    AnyType.asTypeArgument //TODO: this should be type provided by method typeargs
                )
            ),
            ""
        )
        typeDecl.appendMethodPrimitive(
            MethodName("contains"),
            listOf(ParameterDefinitionSimple(TmParameterName("element"), AnyType, null)),
            Boolean,
            "True if this Collection contains the element"
        )
        typeDecl.appendMethodPrimitive(
            MethodName("intersect"),
            listOf(ParameterDefinitionSimple(TmParameterName("other"), this.createTypeInstance(typeDecl.qualifiedName, Collection_typeName, listOf(AnyType.asTypeArgument)), null)),
            Boolean,
            "True if other Collection intersects this Collection."
        )
        typeDecl.appendMethodPrimitive(
            MethodName("map"),
            listOf(ParameterDefinitionSimple(TmParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, LambdaType_typeName), null)),
            List.type(listOf(AnyType.asTypeArgument)), //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
            "A list created by mapping each element using the given lambda expression."
        )
        typeDecl.appendMethodPrimitive(
            MethodName("filter"),
            listOf(ParameterDefinitionSimple(TmParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, LambdaType_typeName), null)),
            List.type(listOf(AnyType.asTypeArgument)), //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
            "A list created by filtering the elements to those for which the given lambda expression evaluates to true."
        )
    }

    private fun createMethodsForList() {
        val typeDecl = List
        typeDecl.appendMethodPrimitive(
            MethodName("get"),
            listOf(ParameterDefinitionSimple(TmParameterName("index"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), null)),
            TypeParameterReference(typeDecl, SimpleName("E")),
            "The element at the given index."
        )
        typeDecl.appendMethodPrimitive(
            MethodName("separate"),
            emptyList(),
            this.createTypeInstance(
                typeDecl.qualifiedName, ListSeparated_typeName, listOf(
                    AnyType.asTypeArgument, //TODO: this should be type provided by method typeargs
                    AnyType.asTypeArgument //TODO: this should be type provided by method typeargs
                )
            ),
            ""
        )
//        typeDecl.appendMethodPrimitive(
//            MethodName("map"),
//            listOf(ParameterDefinitionSimple(TmParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, LambdaType_typeName), null)),
//            List.type(listOf(AnyType.asTypeArgument)), //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
//            "A list created by mapping each element using the given lambda expression."
//        )
//        typeDecl.appendMethodPrimitive(
//            MethodName("filter"),
//            listOf(ParameterDefinitionSimple(TmParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, LambdaType_typeName), null)),
//            List.type(listOf(AnyType.asTypeArgument)), //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
//            "A list created by filtering the elements to those for which the given lambda expression evaluates to true."
//        )
        typeDecl.appendMethodPrimitive(
            MethodName("transitiveClosure"),
            listOf(ParameterDefinitionSimple(TmParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, LambdaType_typeName), null)),
            List.type(listOf(AnyType.asTypeArgument)), //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
            "A list created by performing a transitiveClosure over elements with the given lambda expression."
        )
    }

    private fun createPropertiesForListSeparated() {
        val typeDecl = ListSeparated
        //typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("elements"),
            this.createTypeInstance(
                typeDecl.qualifiedName, List_typeName,
                listOf(AnyType.asTypeArgument)
            ),
            "All elements in the List."
        )
        typeDecl.appendPropertyPrimitive(PropertyName("items"), this.createTypeInstance(typeDecl.qualifiedName, SimpleName("I")), "Items in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("separators"), this.createTypeInstance(typeDecl.qualifiedName, SimpleName("S")), "Separators in the List.")
    }
}

//In work
object StdLibDefault2 : TypesNamespaceAbstract(OptionHolderDefault(null, emptyMap()), emptyList()) {

    init {
        val nsb = TypeNamespaceBuilder(QualifiedName("std"), emptyList(), this)
        nsb.apply {
            special("Any", Any::class)
            special("Nothing", Unit::class)
            special("Lambda")

            primitive("String", String::class) {
                methodPrimitive("toBoolean", "Boolean", true) {
                    execution { self, args -> (self as String).toBooleanStrictOrNull() }
                }
                methodPrimitive("toInteger", "Integer", true) {
                    execution { self, args -> (self as String).toLongOrNull() }
                }
                methodPrimitive("toReal", "Real", true) {
                    execution { self, args -> (self as String).toDoubleOrNull() }
                }
                methodPrimitive("removeSurrounding", "String", true) {
                    parameter("delimiter", "String")
                    description("Remove the given string value from the start and end of this string, if present.")
                    execution { self, args -> (self as String).removeSurrounding(args[0] as CharSequence) }
                }
            }
            primitive("Boolean", Boolean::class)
            primitive("Integer", Long::class)
            primitive("Real", Double::class)
            primitive("Timestamp", Instant::class)
            primitive("Exception", Throwable::class)

            data("Pair", Pair::class)

            collection("Collection", listOf("E"), Collection::class) {
                propertyPrimitive("size", "Integer", false, "Number of elements in the Collection.", execution = { self -> (self as Collection<*>).size })
                propertyPrimitive("isEmpty", "Boolean", false, "True if the Collection has no elements.", execution = { self -> (self as Collection<*>).isEmpty() })
                propertyPrimitive("isNotEmpty", "Boolean", false, "True if the Collection has some elements.", execution = { self -> (self as Collection<*>).isNotEmpty() })
                propertyPrimitive(
                    "asMap", "Map", true,
                    "Returns a Map object with elements being the Pairs of this Collection. If elements are not Pairs, or Map with 'key' and 'value' entry, then returns nothing.",
                    execution = { self -> self?.let { Collection_asMap(self) } }
                ) {
                    typeArgument("Any")
                    typeArgument("Any")
                }
                propertyPrimitive("separate", "ListSeparated") {
                    typeArgument("Any")
                    typeArgument("Any")
                }

                methodPrimitive("contains", "Boolean") {
                    description("Returns true if this Collection contains the element")
                    parameter("element", "E")
                }
                methodPrimitive("intersect", "Boolean") {
                    description("Returns true other Collection intersects this Collection.")
                    parameter("other", "Collection") { typeArgument("Any") }
                }
                methodPrimitive("filter", "List") {
                    description("A list created by filtering the elements to those for which the given lambda expression evaluates to true.")
                    parameter("lambda", "Lambda") // TODO: lambda must return Boolean
                }
                methodPrimitive("map", "List") {
                    description("Returns true other Collection intersects this Collection.")
                    returnTypeArgument("Any") //TODO: should be return type of lambda !
                    parameter("lambda", "Lambda")
                }
            }
            collection("Set", listOf("E"), Set::class) {
                supertype("Collection"){ ref("E") }

                methodPrimitive("transitiveClosure","Set", false) {
                    description("")
                    returnTypeArgument("E")
                    parameter("lambda", "Lambda") // TODO: lambda should return Set<E>
                }
            }
            collection("List", listOf("E"), List::class) {
                supertype("Collection"){ ref("E") }
                propertyPrimitive("first", "E", false, "First element in the List.", execution = { self -> (self as Collection<*>).size })
                propertyPrimitive("last", "E", false, "Last element in the List.", execution = { self -> (self as Collection<*>).size })
                propertyPrimitive("front", "List<E>", false, "All elements in the List except the last one.", execution = { self -> (self as Collection<*>).size })
                propertyPrimitive("back", "List<E>", false, "All elements in the List except the first one.", execution = { self -> (self as Collection<*>).size })
                propertyPrimitive("join", "String", false, "The String value of all elements (toString) concatenated.", execution = { self -> (self as Collection<*>).size })

                methodPrimitive("get", "E", true) {
                    description("Returns the element at the given index.")
                    parameter("index", "Integer")
                }
                methodPrimitive("transitiveClosure","List", false) {
                    description("")
                    returnTypeArgument("E")
                    parameter("lambda", "Lambda") // TODO: lambda should return List<E>
                }
            }
            collection("ListSeparated", listOf("E", "I", "S"), ListSeparated::class) {
                supertype("List") { ref("E") }
            }
            collection("Map", listOf("K", "V"), Map::class) {
                supertype("Collection") { ref("Pair") }
            }
        }
        nsb.build()
    }

    override val qualifiedName: QualifiedName = QualifiedName("std")
    override fun findInOrCloneTo(other: TypesDomain): TypesNamespace = this


    private fun Collection_asMap(self: Any): Map<Any, Any>? {
        check(self is Collection<*>) { "Property 'asMap' is not applicable to '${self::class.simpleName}' objects." }
        return self.associate {
            val el = when (it) {
                is TypedObject -> it.self
                else -> it
            }
            when (el) {
                is Pair<*, *> -> el as Pair<Any, Any>
                is Map<*, *> -> when {
                    el.containsKey("key") && el.containsKey("value") -> Pair(el["key"]!!, el["value"]!!)
                    else -> error("To convert a Collection<Map> via 'asMap' there must be a 'key' and a 'value' entry")
                }

                else -> error("To convert a Collection via 'asMap' the elements must be either Pairs or Maps with key and value entries")
            }
        }
    }
}