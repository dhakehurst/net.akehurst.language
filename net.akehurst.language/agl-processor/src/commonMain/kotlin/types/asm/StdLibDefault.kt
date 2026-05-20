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

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.collections.transitiveClosure
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.FunctionDefinitionFloating
import net.akehurst.language.expressions.api.FunctionParameter
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.FunctionDefinitionAbstract
import net.akehurst.language.expressions.asm.TypeReferenceDefault
import net.akehurst.language.objectgraph.api.FunctionLib
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.*
import net.akehurst.language.types.builder.TypeNamespaceBuilder

@Deprecated("Use StdLibDefault below")
object StdLibDefault1 : TypesNamespaceAbstract(OptionHolderDefault(null, emptyMap()), emptyList()) {

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
    val AnyType get() = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Any")).type()
    val NothingType get() = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Nothing")).type()
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

    val Collection = super.findOwnedOrCreateCollectionTypeNamed(Collection_typeName, listOf(TypeParameterSimple(SimpleName("E"))))

    val List: CollectionType = super.findOwnedOrCreateCollectionTypeNamed(List_typeName, listOf(TypeParameterSimple(SimpleName("E")))).also { typeDecl ->
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    // ListSeparated<I,S>
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed(
        ListSeparated_typeName,
        listOf(TypeParameterSimple(SimpleName("I")), TypeParameterSimple(SimpleName("S")))
    ).also { typeDecl ->
        typeDecl.addSupertype(List.type(listOf(AnyType.asTypeArgument)))
    }

    // Set<E>
    val Set = super.findOwnedOrCreateCollectionTypeNamed(Set_typeName, listOf(TypeParameterSimple(SimpleName("E")))).also { typeDecl ->
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed(OrderedSet_typeName, listOf(TypeParameterSimple(SimpleName("E")))).also { typeDecl ->
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    val Map = super.findOwnedOrCreateCollectionTypeNamed(Map_typeName,listOf(TypeParameterSimple(SimpleName("K")), TypeParameterSimple(SimpleName("V")))).also { typeDecl ->
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
        val typeDecl = String.resolvedDefinition
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

class FunctionDefinitionPrimitive(
    name: SimpleName,
    parameters: List<FunctionParameter>,
    returnTypeReference: TypeReference?,
    body: Expression?
) : FunctionDefinitionFloating, FunctionDefinitionAbstract(name, parameters, returnTypeReference, body) {

}

object StdFunctionLib : FunctionLib {
    override val declaration: Map<String, FunctionDefinitionFloating> = mutableMapOf()

    init {
        (declaration as MutableMap)["Pair"] = FunctionDefinitionPrimitive(
            SimpleName("Pair"),
            listOf(), //TODO
            TypeReferenceDefault(StdLibDefault.Pair.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> Pair(args[0], args[1]) }
        }
        (declaration as MutableMap)["Set"] = FunctionDefinitionPrimitive(
            SimpleName("Set"),
            listOf(), //TODO
            TypeReferenceDefault(StdLibDefault.Pair.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> args.toSet() }
        }
        (declaration as MutableMap)["List"] = FunctionDefinitionPrimitive(
            SimpleName("List"),
            listOf(), //TODO
            TypeReferenceDefault(StdLibDefault.Pair.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> args }
        }
    }

    override fun findFirstFunctionNamed(functionName: String): FunctionDefinitionFloating? {
        return declaration[functionName]
    }
}

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

    init {
        val nsb = TypeNamespaceBuilder(QualifiedName("std"), emptyList(), this)
        nsb.apply {
            special("Any", kotlin.Any::class)
            special("Nothing", kotlin.Unit::class)
            special(LambdaType_typeName.value)

            primitive("String", kotlin.String::class) {
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
            primitive("Boolean", kotlin.Boolean::class)
            primitive("Integer", kotlin.Long::class)
            primitive("Real", kotlin.Double::class)
            primitive("Timestamp", kotlin.time.Instant::class)
            primitive("Exception", kotlin.Throwable::class)

            data("Pair", kotlin.Pair::class)

            collection(Collection_typeName.value, listOf("E"), kotlin.collections.Collection::class) {
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
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' has wrong number of argument, expecting 1, received ${args.size}" }
                        val element = args[0]
                        (self as Collection<*>).contains(element)
                    }
                    // no need for separate suspend version
                }
                methodPrimitive("intersect", "Boolean") {
                    description("Returns true other Collection intersects this Collection.")
                    parameter("other", "Collection") { typeArgument("Any") }
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' has wrong number of argument, expecting 1, received ${args.size}" }
                        check(args[0] is Collection<*>) { "Method '${methodName}' takes an '${StdLibDefault.Collection.qualifiedName.value}' as its argument, received '${args[0]?.let { it::class.simpleName }}'." }
                        val other = (args[0] as Collection<*>)
                        (self as Collection<*>).intersect(other.toSet())
                    }
                    // no need for separate suspend version
                }
                methodPrimitive("filter", "List") {
                    description("A list created by filtering the elements to those for which the given lambda expression evaluates to true.")
                    parameter("lambda", "Lambda") // TODO: lambda must return Boolean
                    returnTypeArgument("E")
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function1<*, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda = args[0] as Function1<Any, Boolean>
                        (self as Collection<Any>).filter {
                            lambda.invoke(it)
                        }
                    }
                    executionSuspend { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let {it::class.simpleName}}'." }
                        val lambda: suspend (Any) -> Boolean = args[0] as suspend (Any) -> Boolean
                        (self as Collection<Any>).filter {
                            lambda.invoke(it)
                        }
                    }
                }
                methodPrimitive("map", "List") {
                    description("Returns true other Collection intersects this Collection.")
                    returnTypeArgument("Any") //TODO: should be return type of lambda !
                    parameter("lambda", "Lambda")
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function1<*, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda = args[0] as Function1<Any, *>
                        (self as Collection<Any>).map {
                            lambda.invoke(it)
                        }
                    }
                    executionSuspend { self, args ->
                        check(self is Collection<*>) { "Method '${methodName}' is not applicable to '${self::class.simpleName}' objects." }
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda: suspend (Any) -> Any = args[0] as suspend (Any) -> Any
                        (self as Collection<Any>).map {
                            lambda.invoke(it)
                        }

                    }
                }
            }
            collection(Set_typeName.value, listOf("E"), kotlin.collections.Set::class) {
                supertype("Collection") { ref("E") }

                methodPrimitive("transitiveClosure", "Set", false) {
                    description("")
                    returnTypeArgument("E")
                    parameter("lambda", "Lambda") // TODO: lambda should return Set<E>
                }
            }
            collection(List_typeName.value, listOf("E"), kotlin.collections.List::class) {
                supertype("Collection") { ref("E") }
                propertyPrimitive("first", "E", false, "First element in the List.", execution = { self -> (self as List<*>).first() })
                propertyPrimitive("last", "E", false, "Last element in the List.", execution = { self -> (self as List<*>).last() })
                propertyPrimitive("front", "List", false, "All elements in the List except the last one.", execution = { self -> (self as List<*>).dropLast(1) }) { typeArgument("E") }
                propertyPrimitive("back", "List", false, "All elements in the List except the first one.", execution = { self -> (self as List<*>).drop(1) }) { typeArgument("E") }
                propertyPrimitive("join", "String", false, "The String value of all elements (toString) concatenated.", execution = { self ->
                    (self as List<*>).joinToString(separator = "") {
                        when (it) {
                            is TypedObject -> it.self.toString()
                            else -> it.toString()
                        }
                    }
                })

                methodPrimitive("get", "E", true) {
                    description("Returns the element at the given index.")
                    parameter("index", "Integer")
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' has wrong number of argument, expecting 1, received ${args.size}" }
                        check(args[0] is Long) { "Method '${methodName}' takes an ${Integer.qualifiedTypeName} as its argument, received ${args[0]?.let { it::class.simpleName }}" }
                        (self as List<*>).get((args[0] as Long).toInt())
                    }
                    executionSuspend { self, args ->
                        check(1 == args.size) { "Method '${methodName}' has wrong number of argument, expecting 1, received ${args.size}" }
                        check(args[0] is Long) { "Method '${methodName}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0]?.let {it::class.simpleName}}" }
                        //check(StdLibDefault.Integer.qualifiedTypeName == args[0].qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                        val idx = args[0] as Long
                        (self as List<*>)[idx.toInt()] as Any
                    }
                }
                methodPrimitive("separate", ListSeparated_typeName.value, true) {
                    description("Returns a ListSeparated version of this List.")
                    parameter("index", "Integer")
                    returnTypeArgument("E")
                    returnTypeArgument("E")
                    returnTypeArgument("E")
                    execution { self, args ->
                        check(args.isEmpty()) { "Method '${methodName}' has wrong number of argument, expecting 0, received ${args.size}" }
                        (self as List<*>).toSeparatedList()
                    }
                }
                methodPrimitive("transitiveClosure", "List", false) {
                    description("")
                    returnTypeArgument("E")
                    parameter("lambda", "Lambda") // TODO: lambda should return List<E>
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function1<*, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda: (Any) -> List<Any> = args[0] as (Any) -> List<Any>
                        (self as List<Any>).transitiveClosure {
                            //val args = mapOf("it" to it)
                            lambda.invoke(it)
                        }

                    }
                    executionSuspend { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        // Lambda has extra paramter for coroutine (because it is a suspend function)
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let {it::class.simpleName}}'." }
                        val lambda: suspend (Any) -> List<Any> = args[0] as suspend (Any) -> List<Any>
                        (self as List<Any>).transitiveClosure {
                            //val args = mapOf("it" to it)
                            lambda.invoke(it)
                        }
                    }
                }
            }
            collection(ListSeparated_typeName.value, listOf("E", "I", "S"), net.akehurst.language.collections.ListSeparated::class) {
                supertype("List") { ref("E") }
                propertyPrimitive("elements", "List", false, "Elements in the ListSeparated.", execution = { self -> (self as ListSeparated<*, *, *>).elements }) { typeArgument("E") }
                propertyPrimitive("items", "List", false, "Items in the ListSeparated.", execution = { self -> (self as ListSeparated<*, *, *>).items }) { typeArgument("I") }
                propertyPrimitive("separators", "List", false, "Separators in the ListSeparated.", execution = { self -> (self as ListSeparated<*, *, *>).separators }) { typeArgument("S") }
            }
            collection(OrderedSet_typeName.value, listOf("E"), net.akehurst.kotlinx.collections.OrderedSet::class) {
                //TODO:
            }
            collection(Map_typeName.value, listOf("K", "V"), kotlin.collections.Map::class) {
                supertype("Collection") { ref("Pair") }
            }
        }
        nsb.build()
    }

    val AnyType by lazy { super.findOwnedSpecialTypeNamedOrNull(SimpleName("Any"))!!.type() }
    val NothingType by lazy { super.findOwnedSpecialTypeNamedOrNull(SimpleName("Nothing"))!!.type() }

    val String by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("String"))!!.type() }
    val Boolean by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Boolean"))!!.type() }

    /**
     * 64 bit Integer (Long)
     */
    val Integer by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Integer"))!!.type() }

    /**
     * 64 bit Real (Double)
     */
    val Real by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Real"))!!.type() }
    val Timestamp by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Timestamp"))!!.type() }
    val Exception by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Exception"))!!.type() }

    val Pair by lazy { super.findOwnedDataTypeNamedOrNull(SimpleName("Pair"))!! }
    val Lambda by lazy { super.findOwnedSpecialTypeNamedOrNull(LambdaType_typeName)!!.type() }
    val TupleType by lazy { TupleTypeSimple(this, TupleType_typeName) }

    val Collection by lazy { super.findOwnedCollectionTypeNamedOrNull(Collection_typeName)!! }
    val Set by lazy { super.findOwnedCollectionTypeNamedOrNull(Set_typeName)!! }
    val List by lazy { super.findOwnedCollectionTypeNamedOrNull(List_typeName)!! }
    val ListSeparated by lazy { super.findOwnedCollectionTypeNamedOrNull(ListSeparated_typeName)!! }
    val OrderedSet by lazy { super.findOwnedCollectionTypeNamedOrNull(OrderedSet_typeName)!! }
    val Map by lazy { super.findOwnedCollectionTypeNamedOrNull(Map_typeName)!! }

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