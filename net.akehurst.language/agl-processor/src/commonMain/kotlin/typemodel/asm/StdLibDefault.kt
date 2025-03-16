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

package net.akehurst.language.typemodel.asm

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.typemodel.api.*

object StdLibDefault : TypeNamespaceAbstract(OptionHolderDefault(null, emptyMap()), emptyList()) {

    override val qualifiedName: QualifiedName = QualifiedName("std")

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Any")).type()
    val NothingType = super.findOwnedOrCreateSpecialTypeNamed(SimpleName("Nothing")).type()
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("String")).type()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Boolean")).type()
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Integer")).type()
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Real")).type()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Timestamp")).type()
    val Exception = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Exception")).type()

    val Pair = super.findOwnedOrCreateDataTypeNamed(SimpleName("Pair")).also { td ->
        (td.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("F")))
        (td.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("S")))
        (td as DataTypeSimple).addConstructor(
            listOf(
                ParameterDefinitionSimple(net.akehurst.language.typemodel.api.ParameterName("first"), this.createTypeInstance(td.qualifiedName, SimpleName("F")), null),
                ParameterDefinitionSimple(net.akehurst.language.typemodel.api.ParameterName("second"), this.createTypeInstance(td.qualifiedName, SimpleName("S")), null),
            )
        )
        td.appendPropertyStored(PropertyName("first"), TypeParameterReference(td, SimpleName("F")), setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.COMPOSITE), 0)
        td.appendPropertyStored(PropertyName("second"), TypeParameterReference(td, SimpleName("S")), setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.COMPOSITE), 1)
    }

    private val LambdaType_typeName = SimpleName("LambdaType")
    val Lambda = super.findOwnedOrCreateSpecialTypeNamed(LambdaType_typeName).type()

    private val TupleType_typeName = SimpleName("TupleType")
    val TupleType = TupleTypeSimple(this, TupleType_typeName)

    private val Collection_typeName = SimpleName("Collection")
    val Collection = super.findOwnedOrCreateCollectionTypeNamed(Collection_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.appendPropertyPrimitive(
            PropertyName("asMap"),
            this.createTypeInstance(typeDecl.qualifiedName, QualifiedName("std.Map"),
                listOf(
                    StdLibDefault.AnyType.asTypeArgument, //TODO: should be type of Pair.first
                    StdLibDefault.AnyType.asTypeArgument //TODO: should be type of Pair.second
                ),
                false
            ),
            "A Map object with elements being the Pairs of this List."
        )

        typeDecl.appendMethodPrimitive(
            MethodName("map"),
            listOf(ParameterDefinitionSimple(net.akehurst.language.typemodel.api.ParameterName("lambda"), this.createTypeInstance(typeDecl.qualifiedName, Lambda.typeName), null)),
            StdLibDefault.AnyType, //TODO: this should be result of lambda  //TypeParameterReference(typeDecl, SimpleName("E")),
            "A list created by mapping each element using the given lambda expression."
        )
    }

    private val List_typeName = SimpleName("List")
    val List: CollectionType = super.findOwnedOrCreateCollectionTypeNamed(List_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
        typeDecl.appendPropertyPrimitive(PropertyName("size"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), "Number of elements in the List.")
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
        typeDecl.appendMethodPrimitive(
            MethodName("get"),
            listOf(ParameterDefinitionSimple(net.akehurst.language.typemodel.api.ParameterName("index"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), null)),
            TypeParameterReference(typeDecl, SimpleName("E")),
            "The element at the given index."
        )
    }

    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed(SimpleName("ListSeparated")).also { typeDecl ->
        typeDecl.addSupertype(List.type(listOf(AnyType.asTypeArgument)))
        (typeDecl.typeParameters as MutableList).addAll(listOf(TypeParameterSimple(SimpleName("E")), TypeParameterSimple(SimpleName("I"))))
        //typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("elements"),
            this.createTypeInstance(
                typeDecl.qualifiedName, List_typeName,
                listOf(TypeParameterReference(typeDecl, SimpleName("E")).asTypeArgument)
            ),
            "Number of elements in the List."
        )
        typeDecl.appendPropertyPrimitive(PropertyName("items"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("separators"), this.createTypeInstance(typeDecl.qualifiedName, Integer.typeName), "Number of elements in the List.")
    }

    private val Set_typeName = SimpleName("Set")
    val Set = super.findOwnedOrCreateCollectionTypeNamed(Set_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    private val OrderedSet_typeName = SimpleName("OrderedSet")
    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed(OrderedSet_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(TypeParameterSimple(SimpleName("E")))
        typeDecl.addSupertype(Collection.type(listOf(TypeArgumentSimple(TypeParameterReference(typeDecl, SimpleName("E"))))))
    }

    private val Map_typeName = SimpleName("Map")
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

    override fun findInOrCloneTo(other: TypeModel): TypeNamespace = this

}


