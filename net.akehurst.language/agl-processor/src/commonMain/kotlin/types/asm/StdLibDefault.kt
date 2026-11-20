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
import net.akehurst.kotlinx.collections.ListSeparated
import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.kotlinx.collections.transitiveClosure
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.FunctionDefinitionFloating
import net.akehurst.language.expressions.api.FunctionParameter
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.FunctionDefinitionAbstract
import net.akehurst.language.expressions.asm.TypeReferenceDefault
import net.akehurst.language.objectgraph.api.FunctionLib
import net.akehurst.language.objectgraph.api.TypedLambda
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

    val Map = super.findOwnedOrCreateCollectionTypeNamed(Map_typeName, listOf(TypeParameterSimple(SimpleName("K")), TypeParameterSimple(SimpleName("V")))).also { typeDecl ->
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
            TypeReferenceDefault(StdLibDefault.Set.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> args.toSet() }
        }
        (declaration as MutableMap)["List"] = FunctionDefinitionPrimitive(
            SimpleName("List"),
            listOf(), //TODO
            TypeReferenceDefault(StdLibDefault.List.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> args }
        }
        (declaration as MutableMap)["Map"] = FunctionDefinitionPrimitive(
            SimpleName("Map"),
            listOf(), //TODO
            TypeReferenceDefault(StdLibDefault.Map.qualifiedName, emptyList(), false),
            null
        ).also {
            it.execution = { args -> args.associate { a -> a as Pair<*, *> } }
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
    private val Type_typeName = SimpleName("Type")
    private val Property_typeName = SimpleName("Property")

    init {
        val nsb = TypeNamespaceBuilder(QualifiedName("std"), emptyList(), this)
        nsb.apply {
            special("Any", kotlin.Any::class)
            special("Nothing", kotlin.Unit::class)
            special(LambdaType_typeName.value)

            primitive("String", kotlin.String::class) {
                derivedPropertyOf("length", "Integer", false, accessor = { self -> (self as String).length.toLong() })
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
            primitive("Binary", kotlin.ByteArray::class)
            primitive("Exception", kotlin.Throwable::class)

            data("Pair", kotlin.Pair::class) {
                typeParameters("F", "S")
                constructor_ {
                    execution { Pair(it[0], it[1]) }
                    parameter(setOf(CMP, VAL), "first", "F", accessor = kotlin.Pair<*, *>::first)
                    parameter(setOf(CMP, VAL), "second", "S", accessor = kotlin.Pair<*, *>::second)
                }
            }

            //TODO: future enhancement (agreed 2026-11-13): a lazy std type, Flow (suspend) / Sequence (sync), so that
            // map/filter chains run in one pass without making intermediate Lists, with 'toList' to materialise.
            collection(Collection_typeName.value, listOf("E"), kotlin.collections.Collection::class) {
                propertyPrimitive<Collection<*>, Int>("size", "Integer", false, "Number of elements in the Collection.", accessor = { self -> self.size })
                propertyPrimitive<Collection<*>, Boolean>("isEmpty", "Boolean", false, "True if the Collection has no elements.", accessor = { self -> self.isEmpty() })
                propertyPrimitive<Collection<*>, Boolean>("isNotEmpty", "Boolean", false, "True if the Collection has some elements.", accessor = { self -> self.isNotEmpty() })
                propertyPrimitive<Collection<*>, List<*>>(
                    "flatten", "List", false,
                    "A List of the elements of the Collections that are the elements of this Collection.",
                    accessor = { self -> self.flatMap { Collection_flatMapElements("flatten", it) } }
                ) {
                    typeArgument("Any")
                }
                propertyPrimitive<Collection<*>, Map<*, *>?>(
                    "asMap", "Map", true,
                    "Returns a Map object with elements being the Pairs of this Collection. If elements are not Pairs, or Map with 'key' and 'value' entry, then returns nothing.",
                    accessor = { self -> self?.let { Collection_asMap(self) } }
                ) {
                    typeArgument("Any")
                    typeArgument("Any")
                }
                propertyPrimitive<Collection<*>, ListSeparated<*, *, *>>(
                    "separate", "ListSeparated",
                    accessor = { self -> self.toSeparatedList() }
                ) {
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
                    executionTyped { self, args -> Collection_filterTyped(methodName, self, args) { l, el -> l.invokeTyped(el) } }
                    executionTypedSuspend { self, args -> Collection_filterTyped(methodName, self, args) { l, el -> l.invokeTypedSuspend(el) } }
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
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
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
                    executionTyped { self, args -> Collection_mapTyped(methodName, self, args) { l, el -> l.invokeTyped(el) } }
                    executionTypedSuspend { self, args -> Collection_mapTyped(methodName, self, args) { l, el -> l.invokeTypedSuspend(el) } }
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
                methodPrimitive("flatMap", "List") {
                    description("A list of the elements of the Collections that the given lambda expression gives for each element.")
                    returnTypeArgument("Any") //TODO: should be element type of the return type of lambda !
                    parameter("lambda", "Lambda") // TODO: lambda must return a Collection
                    executionTyped { self, args -> Collection_flatMapTyped(methodName, self, args) { l, el -> l.invokeTyped(el) } }
                    executionTypedSuspend { self, args -> Collection_flatMapTyped(methodName, self, args) { l, el -> l.invokeTypedSuspend(el) } }
                    execution { self, args ->
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function1<*, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda = args[0] as Function1<Any, *>
                        (self as Collection<Any>).flatMap {
                            Collection_flatMapElements(methodName, lambda.invoke(it))
                        }
                    }
                    executionSuspend { self, args ->
                        check(self is Collection<*>) { "Method '${methodName}' is not applicable to '${self::class.simpleName}' objects." }
                        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda: suspend (Any) -> Any = args[0] as suspend (Any) -> Any
                        (self as Collection<Any>).flatMap {
                            Collection_flatMapElements(methodName, lambda.invoke(it))
                        }
                    }
                }
                methodPrimitive("plus", "Collection") {
                    description("This Collection followed by the elements of the other Collection, as the '+' operator. A Set gives a Set (union), otherwise a List.")
                    returnTypeArgument("Any")
                    parameter("other", "Collection") { typeArgument("Any") }
                    execution { self, args -> Collection_plus(methodName, self, args) }
                    executionSuspend { self, args -> Collection_plus(methodName, self, args) }
                }
                methodPrimitive("union", "Set") {
                    description("A Set of the distinct elements of this Collection and the other Collection, in order.")
                    returnTypeArgument("Any")
                    parameter("other", "Collection") { typeArgument("Any") }
                    execution { self, args -> Collection_union(methodName, self, args) }
                    executionSuspend { self, args -> Collection_union(methodName, self, args) }
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
                propertyPrimitive<List<*>, Any?>("first", "E", false, "First element in the List.", accessor = { self -> self.firstOrNull() })
                propertyPrimitive<List<*>, Any?>("last", "E", false, "Last element in the List.", accessor = { self -> (self as List<*>).last() })
                propertyPrimitive<List<*>, Any?>("front", "List", false, "All elements in the List except the last one.", accessor = { self -> (self as List<*>).dropLast(1) }) { typeArgument("E") }
                propertyPrimitive<List<*>, Any?>("back", "List", false, "All elements in the List except the first one.", accessor = { self -> (self as List<*>).drop(1) }) { typeArgument("E") }
                propertyPrimitive<List<*>, Any?>("join", "String", false, "The String value of all elements (toString) concatenated.", accessor = { self ->
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
                        check(args[0] is Long) { "Method '${methodName}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0]?.let { it::class.simpleName }}" }
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
                    executionTyped { self, args -> List_transitiveClosureTyped(methodName, self, args) { l, el -> l.invokeTyped(el) } }
                    executionTypedSuspend { self, args -> List_transitiveClosureTyped(methodName, self, args) { l, el -> l.invokeTypedSuspend(el) } }
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
                        check(args[0] is Function2<*, *, *>) { "Method '${methodName}' first argument must be a lambda, got '${args[0]?.let { it::class.simpleName }}'." }
                        val lambda: suspend (Any) -> List<Any> = args[0] as suspend (Any) -> List<Any>
                        (self as List<Any>).transitiveClosure {
                            //val args = mapOf("it" to it)
                            lambda.invoke(it)
                        }
                    }
                }
                methodPrimitive("foldRight", "Any", true) {
                    description("Accumulates a value starting with 'initial', applying the lambda '{ element, acc -> ... }' to each element (from the last to the first) and the value accumulated so far, as Kotlin List.foldRight.")
                    parameter("initial", "Any")
                    parameter("lambda", "Lambda") // a lambda with two parameters: the element and the accumulated value
                    executionTyped { self, args -> List_foldRightTyped(methodName, self, args) { l, el, acc -> l.invokeTyped(typedList(self, AnyType, listOf(el, acc))) } }
                    executionTypedSuspend { self, args -> List_foldRightTyped(methodName, self, args) { l, el, acc -> l.invokeTypedSuspend(typedList(self, AnyType, listOf(el, acc))) } }
                    execution { self, args ->
                        check(2 == args.size) { "Method '${methodName}' takes 2 arguments (initial value, lambda) got ${args.size} arguments." }
                        check(args[1] is Function1<*, *>) { "Method '${methodName}' second argument must be a lambda, got '${args[1]?.let { it::class.simpleName }}'." }
                        val lambda = args[1] as (Any?) -> Any?
                        // the lambda's two parameters are given as one list of the two values
                        (self as List<Any?>).foldRight(args[0]) { el, acc -> lambda.invoke(listOf(el, acc)) }
                    }
                    executionSuspend { self, args ->
                        check(2 == args.size) { "Method '${methodName}' takes 2 arguments (initial value, lambda) got ${args.size} arguments." }
                        check(args[1] is Function2<*, *, *>) { "Method '${methodName}' second argument must be a lambda, got '${args[1]?.let { it::class.simpleName }}'." }
                        val lambda = args[1] as suspend (Any?) -> Any?
                        var acc = args[0]
                        for (el in (self as List<Any?>).asReversed()) acc = lambda.invoke(listOf(el, acc))
                        acc
                    }
                }
            }
            collection(ListSeparated_typeName.value, listOf("E", "I", "S"), net.akehurst.kotlinx.collections.ListSeparated::class) {
                supertype("List") { ref("E") }
                propertyPrimitive<ListSeparated<*, *, *>, Any?>("elements", "List", false, "Elements in the ListSeparated.", accessor = { self -> (self as ListSeparated<*, *, *>).elements }) { typeArgument("E") }
                propertyPrimitive<ListSeparated<*, *, *>, Any?>("items", "List", false, "Items in the ListSeparated.", accessor = { self -> (self as ListSeparated<*, *, *>).items }) { typeArgument("I") }
                propertyPrimitive<ListSeparated<*, *, *>, Any?>("separators", "List", false, "Separators in the ListSeparated.", accessor = { self -> (self as ListSeparated<*, *, *>).separators }) { typeArgument("S") }
            }
            collection(OrderedSet_typeName.value, listOf("E"), net.akehurst.kotlinx.collections.OrderedSet::class) {
                //TODO:
            }
            collection(Map_typeName.value, listOf("K", "V"), kotlin.collections.Map::class) {
                supertype("Collection") { ref("Pair") }

                propertyPrimitive<Map<*, *>, Any?>("keys", "Set", false, "Keys in the Map.", accessor = { self -> (self as Map<*, *>).keys }) { typeArgument("K") }
                propertyPrimitive<Map<*, *>, Any?>("values", "Collection", false, "Values in the Map.", accessor = { self -> (self as Map<*, *>).values }) { typeArgument("V") }
            }

            // --- reflection: the type of a value ('.\$type') and its properties ---
            // interfaces, values of them cannot be constructed in expressions, they are given by '.\$type'
            interface_(Type_typeName.value, TypeInstance::class) {
                propertyPrimitive<TypeInstance, String>("name", "String", false, "Simple name of the type.", accessor = { self -> self.typeName.value })
                propertyPrimitive<TypeInstance, String>("qualifiedName", "String", false, "Qualified name of the type.", accessor = { self -> self.qualifiedTypeName.value })
                propertyPrimitive<TypeInstance, Boolean>("isNullable", "Boolean", false, "True if the type is nullable.", accessor = { self -> self.isNullable })
                propertyPrimitive<TypeInstance, List<TypeInstance>>("typeArgument", "List", false, "Type arguments of the type.", accessor = { self -> self.typeArguments.map { it.type } }) {
                    typeArgument(Type_typeName.value)
                }
                propertyPrimitive<TypeInstance, List<TypeInstance>>("supertype", "List", false, "Direct supertypes of the type.", accessor = { self -> self.resolvedDefinitionOrNull?.supertypes ?: emptyList() }) {
                    typeArgument(Type_typeName.value)
                }
                propertyPrimitive<TypeInstance, List<TypeInstance>>("allSupertype", "List", false, "All (transitive) supertypes of the type.", accessor = { self -> self.resolvedDefinitionOrNull?.allSuperTypes ?: emptyList() }) {
                    typeArgument(Type_typeName.value)
                }
                propertyPrimitive<TypeInstance, List<PropertyDeclaration>>(
                    "property", "List", false, "Properties of the type, own and inherited, with type arguments resolved.",
                    accessor = { self -> Type_properties(self) }
                ) {
                    typeArgument(Property_typeName.value)
                }
                propertyPrimitive<TypeInstance, Boolean>("isSpecial", "Boolean", false, "True if the type is a special type (e.g. Any, Nothing).", accessor = { self -> self.resolvedDefinitionOrNull is SpecialType })
                propertyPrimitive<TypeInstance, Boolean>("isSingleton", "Boolean", false, "True if the type is a singleton type.", accessor = { self -> self.resolvedDefinitionOrNull is SingletonType })
                propertyPrimitive<TypeInstance, Boolean>("isPrimitive", "Boolean", false, "True if the type is a primitive type.", accessor = { self -> self.resolvedDefinitionOrNull is PrimitiveType })
                propertyPrimitive<TypeInstance, Boolean>("isEnum", "Boolean", false, "True if the type is an enum type.", accessor = { self -> self.resolvedDefinitionOrNull is EnumType })
                propertyPrimitive<TypeInstance, Boolean>("isValue", "Boolean", false, "True if the type is a value type.", accessor = { self -> self.resolvedDefinitionOrNull is ValueType })
                propertyPrimitive<TypeInstance, Boolean>("isUnion", "Boolean", false, "True if the type is a union type.", accessor = { self -> self.resolvedDefinitionOrNull is UnionType })
                propertyPrimitive<TypeInstance, Boolean>("isTuple", "Boolean", false, "True if the type is a tuple type.", accessor = { self -> self.resolvedDefinitionOrNull is TupleType })
                propertyPrimitive<TypeInstance, Boolean>("isCollection", "Boolean", false, "True if the type is a collection type.", accessor = { self -> self.resolvedDefinitionOrNull is CollectionType })
                propertyPrimitive<TypeInstance, Boolean>("isData", "Boolean", false, "True if the type is a data type.", accessor = { self -> self.resolvedDefinitionOrNull is DataType })
                propertyPrimitive<TypeInstance, Boolean>("isInterface", "Boolean", false, "True if the type is an interface type.", accessor = { self -> self.resolvedDefinitionOrNull is InterfaceType })
                methodPrimitive("findPropertyByName", Property_typeName.value, true) {
                    parameter("name", "String")
                    description("The property of this type with the given name, own or inherited, \$nothing if there is none.")
                    execution { self, args -> Type_properties(self as TypeInstance).firstOrNull { it.name.value == args[0] } }
                }
                methodPrimitive("conformsTo", "Boolean", false) {
                    parameter("other", Type_typeName.value)
                    description("True if this type conforms to (is the same as, or a subtype of) the other type.")
                    execution { self, args -> (self as TypeInstance).conformsTo(args[0] as TypeInstance) }
                }
            }
            interface_(Property_typeName.value, PropertyDeclaration::class) {
                propertyPrimitive<PropertyDeclaration, String>("name", "String", false, "Name of the property.", accessor = { self -> self.name.value })
                propertyPrimitive<PropertyDeclaration, TypeInstance>("type", Type_typeName.value, false, "Type of the property.", accessor = { self -> self.typeInstance })
                //TODO: future enhancement (David 2026-11-20): 'owner' of a property inherited from a generic type has no type arguments
                // (e.g. 'List', not 'List<Item>'), as PropertyDeclaration.owner is a TypeDefinition. To fix, a Property value would carry
                // the type it was read from, and 'owner' would be the matching (resolved) supertype of that type.
                propertyPrimitive<PropertyDeclaration, TypeInstance>(
                    "owner", Type_typeName.value, false, "The type that declares the property (without type arguments).",
                    accessor = { self -> ((self as? PropertyDeclarationResolved)?.original?.owner ?: self.owner).type() }
                )
                propertyPrimitive<PropertyDeclaration, Boolean>("isComposite", "Boolean", false, "True if the property is composite (the owner owns the value).", accessor = { self -> self.isComposite })
                propertyPrimitive<PropertyDeclaration, Boolean>("isReference", "Boolean", false, "True if the property is a reference.", accessor = { self -> self.isReference })
                propertyPrimitive<PropertyDeclaration, Boolean>("isReadOnly", "Boolean", false, "True if the property is read only.", accessor = { self -> self.isReadOnly })
                propertyPrimitive<PropertyDeclaration, Boolean>("isDerived", "Boolean", false, "True if the property is derived.", accessor = { self -> self.isDerived })
                //TODO: future enhancement (David 2026-11-20): 'setValue(obj, value)', not wanted for now
                methodPrimitive("getValue", "Any", true) {
                    parameter("obj", "Any")
                    description("The value of this property of the given object, \$nothing if the object has no such property.")
                    executionTyped { self, args -> Property_getValueTyped(self, args) }
                    executionTypedSuspend { self, args -> Property_getValueTypedSuspend(self, args) }
                }
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

    /**
     * a sequence of bytes (ByteArray)
     */
    val Binary by lazy { super.findOwnedPrimitiveTypeNamedOrNull(SimpleName("Binary"))!!.type() }
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

    /** reflection: the type of a value, given by '.\$type' */
    val Type by lazy { super.findOwnedInterfaceTypeNamedOrNull(Type_typeName)!! }

    /** reflection: a property of a type */
    val Property by lazy { super.findOwnedInterfaceTypeNamedOrNull(Property_typeName)!! }

    /** the properties, own and inherited (with type arguments resolved), of the type */
    private fun Type_properties(self: TypeInstance): List<PropertyDeclaration> = when (self.resolvedDefinitionOrNull) {
        null -> emptyList()
        else -> self.allResolvedProperty.values.toList()
    }

    private fun Property_getValueArg(methodName: String, args: List<TypedObject>): TypedObject {
        check(1 == args.size) { "Method '${methodName}' takes 1 argument got ${args.size} arguments." }
        return args[0]
    }

    private fun Property_getValueTyped(self: TypedObject, args: List<TypedObject>): TypedObject {
        val obj = Property_getValueArg("getValue", args)
        val prop = self.untyped as PropertyDeclaration
        return when {
            obj.isNothing -> obj
            else -> obj.getProperty(prop.name.value)
        }
    }

    private suspend fun Property_getValueTypedSuspend(self: TypedObject, args: List<TypedObject>): TypedObject {
        val obj = Property_getValueArg("getValue", args)
        val prop = self.untyped as PropertyDeclaration
        return when {
            obj.isNothing -> obj
            else -> obj.getPropertySuspend(prop.name.value)
        }
    }

    override fun findInOrCloneTo(other: TypesDomain): TypesNamespace = this

    // --- typed executions: elements and lambda results are kept as TypedObjects (each with its own type and accessor-mutator) ---

    private fun typedLambdaArgument(methodName: String, args: List<TypedObject>): TypedLambda {
        check(1 == args.size) { "Method '${methodName}' takes 1 lambda argument got ${args.size} arguments." }
        val lambda = args[0]
        return when {
            lambda is TypedLambda -> lambda
            // a lambda value not made by createLambdaValue, e.g. typedAs({ it -> ... }, Lambda)
            lambda.self is Function1<*, *> || lambda.self is Function2<*, *, *> -> UntypedLambdaAsTypedLambda(lambda)
            else -> error("Method '${methodName}' first argument must be a lambda, got '${lambda.type.qualifiedTypeName.value}'.")
        }
    }

    /** invokes an untyped lambda with the untyped argument, the result typed by the lambda's accessor-mutator */
    private class UntypedLambdaAsTypedLambda(val lambda: TypedObject) : TypedLambda, TypedObject by lambda {
        override fun invokeTyped(arg: TypedObject): TypedObject {
            val fn = lambda.self
            check(fn is Function1<*, *>) { "Lambda can only be invoked from suspending evaluation" }
            return lambda.accessor.toTypedObject((fn as (Any) -> Any?).invoke(arg.untyped), AnyType)
        }

        override suspend fun invokeTypedSuspend(arg: TypedObject): TypedObject = when (val fn = lambda.self) {
            is Function1<*, *> -> invokeTyped(arg)
            else -> lambda.accessor.toTypedObject((fn as suspend (Any) -> Any?).invoke(arg.untyped), AnyType)
        }
    }

    /** one pass over the elements, as they are */
    private fun typedElements(collection: TypedObject): List<TypedObject> {
        val elements = mutableListOf<TypedObject>()
        collection.forEachIndexed { _, el -> elements.add(el) }
        return elements
    }

    private fun typedList(owner: TypedObject, elementType: TypeInstance, elements: List<TypedObject>): TypedObject =
        owner.accessor.createCollection(StdLibDefault.List.type(listOf(elementType.asTypeArgument)), elements)

    private fun elementTypeOf(collection: TypedObject): TypeInstance = collection.type.typeArguments.getOrNull(0)?.type ?: AnyType

    private inline fun Collection_mapTyped(methodName: String, self: TypedObject, args: List<TypedObject>, invoke: (TypedLambda, TypedObject) -> TypedObject): TypedObject {
        val lambda = typedLambdaArgument(methodName, args)
        val result = typedElements(self).map { invoke(lambda, it) }
        return typedList(self, AnyType, result) //TODO: element type should be return type of lambda
    }

    private inline fun Collection_filterTyped(methodName: String, self: TypedObject, args: List<TypedObject>, invoke: (TypedLambda, TypedObject) -> TypedObject): TypedObject {
        val lambda = typedLambdaArgument(methodName, args)
        val result = typedElements(self).filter { true == invoke(lambda, it).self }
        return typedList(self, elementTypeOf(self), result)
    }

    private inline fun Collection_flatMapTyped(methodName: String, self: TypedObject, args: List<TypedObject>, invoke: (TypedLambda, TypedObject) -> TypedObject): TypedObject {
        val lambda = typedLambdaArgument(methodName, args)
        val result = typedElements(self).flatMap { el ->
            val res = invoke(lambda, el)
            when {
                res.isNothing -> emptyList()
                res.type.resolvedDefinition.conformsTo(Collection) -> typedElements(res)
                else -> error("Method '${methodName}': the lambda must give a Collection, got '${res.type.qualifiedTypeName.value}'.")
            }
        }
        return typedList(self, AnyType, result) //TODO: element type should be element type of return type of lambda
    }

    private inline fun List_transitiveClosureTyped(methodName: String, self: TypedObject, args: List<TypedObject>, invoke: (TypedLambda, TypedObject) -> TypedObject): TypedObject {
        val lambda = typedLambdaArgument(methodName, args)
        val result = typedElements(self).transitiveClosure { el ->
            val res = invoke(lambda, el)
            when {
                res.isNothing -> emptyList()
                else -> typedElements(res)
            }
        }
        return typedList(self, elementTypeOf(self), result)
    }

    private inline fun List_foldRightTyped(methodName: String, self: TypedObject, args: List<TypedObject>, invoke: (TypedLambda, TypedObject, TypedObject) -> TypedObject): TypedObject {
        check(2 == args.size) { "Method '${methodName}' takes 2 arguments (initial value, lambda) got ${args.size} arguments." }
        val lambda = typedLambdaArgument(methodName, listOf(args[1]))
        var acc = args[0]
        for (el in typedElements(self).asReversed()) acc = invoke(lambda, el, acc)
        return acc
    }

    private fun Collection_otherArgument(methodName: String, args: List<*>): Collection<*> {
        check(1 == args.size) { "Method '${methodName}' has wrong number of argument, expecting 1, received ${args.size}" }
        check(args[0] is Collection<*>) { "Method '${methodName}' takes a '${Collection.qualifiedName.value}' as its argument, received '${args[0]?.let { it::class.simpleName }}'." }
        return args[0] as Collection<*>
    }

    /** as Kotlin: a Set gives a Set (union), otherwise a List of all elements of self then all of other */
    private fun Collection_plus(methodName: String, self: Any, args: List<*>): Collection<*> {
        val other = Collection_otherArgument(methodName, args)
        return when (self) {
            is Set<*> -> self + other
            else -> (self as Collection<*>).toList() + other
        }
    }

    /** as Kotlin: a Set of the distinct elements of self and other, in order */
    private fun Collection_union(methodName: String, self: Any, args: List<*>): Collection<*> =
        (self as Collection<*>).union(Collection_otherArgument(methodName, args))

    /** the elements of the value a 'flatMap' lambda gave for one element; the value must be a Collection (or $nothing, giving no elements) */
    private fun Collection_flatMapElements(methodName: String, lambdaResult: Any?): Collection<Any?> {
        val res = when (lambdaResult) {
            is TypedObject -> lambdaResult.self
            else -> lambdaResult
        }
        return when (res) {
            null, Unit -> emptyList()
            is Collection<*> -> res
            else -> error("Method '${methodName}': the lambda must give a Collection, got '${res::class.simpleName}'.")
        }
    }

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