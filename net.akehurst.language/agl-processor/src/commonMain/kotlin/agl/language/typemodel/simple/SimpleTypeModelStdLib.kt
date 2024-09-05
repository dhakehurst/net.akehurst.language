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

import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.CollectionType
import net.akehurst.language.typemodel.api.MethodName
import net.akehurst.language.typemodel.api.ParameterName
import net.akehurst.language.typemodel.api.PropertyName

object  SimpleTypeModelStdLib : TypeNamespaceAbstract(QualifiedName("std"), emptyList()) {

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOrCreateSpecialTypeNamed(SimpleName("Any")).type()
    val NothingType = super.findOrCreateSpecialTypeNamed(SimpleName("Nothing")).type()
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("String")).type()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Boolean")).type()
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Integer")).type()
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Real")).type()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("Timestamp")).type()

    private val List_typeName = SimpleName("List")
    val List: CollectionType = super.findOwnedOrCreateCollectionTypeNamed(List_typeName).also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add(SimpleName("E"))
        typeDecl.appendPropertyPrimitive(PropertyName("size"), this.createTypeInstance(typeDecl, Integer.typeName), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("first"), this.createTypeInstance(typeDecl, SimpleName("E")), "First element in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("last"), this.createTypeInstance(typeDecl, SimpleName("E")), "Last element in the list.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("back"),
            this.createTypeInstance(typeDecl, List_typeName, listOf(this.createTypeInstance(typeDecl, SimpleName("E")))),
            "All elements in the List except the first one."
        )
        typeDecl.appendPropertyPrimitive(
            PropertyName("front"),
            this.createTypeInstance(typeDecl, List_typeName, listOf(this.createTypeInstance(typeDecl, SimpleName("E")))),
            "All elements in the List except the last one."
        )
        typeDecl.appendPropertyPrimitive(PropertyName("join"), this.createTypeInstance(typeDecl, String.typeName), "The String value of all elements concatenated.")
        typeDecl.appendMethodPrimitive(
            MethodName("get"),
            listOf(ParameterDefinitionSimple(ParameterName("index"), this.createTypeInstance(typeDecl, Integer.typeName), null)),
            this.createTypeInstance(typeDecl, SimpleName("E")), "The element at the given index."
        ) { it, arguments ->
            check(it is List<*>) { "Method 'get' is only applicably to List objects." }
            check(1 == arguments.size) { "Method 'get' should only have 1 (Integer) argument." }
            check(arguments[0] is Int)
            val self = it as List<Any>
            val arg1 = arguments[0] as Int
            self[arg1]
        }
    }
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed(SimpleName("ListSeparated")).also { typeDecl ->
        typeDecl.addSupertype(List.qualifiedName)
        (typeDecl.typeParameters as MutableList).addAll(listOf(SimpleName("E"), SimpleName("I")))
        //typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(
            PropertyName("elements"),
            this.createTypeInstance(
                typeDecl, List_typeName,
                listOf(this.createTypeInstance(typeDecl, SimpleName("E")))
            ),
            "Number of elements in the List."
        )
        typeDecl.appendPropertyPrimitive(PropertyName("items"), this.createTypeInstance(typeDecl, Integer.typeName), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(PropertyName("separators"), this.createTypeInstance(typeDecl, Integer.typeName), "Number of elements in the List.")
    }
    private val Set_typeName = SimpleName("Set")
    val Set = super.findOwnedOrCreateCollectionTypeNamed(Set_typeName).also {
        (it.typeParameters as MutableList).add(SimpleName("E"))
    }
    private val OrderedSet_typeName = SimpleName("OrderedSet")
    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed(OrderedSet_typeName).also {
        (it.typeParameters as MutableList).add(SimpleName("E"))
    }
    private val Map_typeName = SimpleName("Map")
    val Map = super.findOwnedOrCreateCollectionTypeNamed(Map_typeName).also {
        (it.typeParameters as MutableList).addAll(listOf(SimpleName("K"), SimpleName("V")))
    }

}


