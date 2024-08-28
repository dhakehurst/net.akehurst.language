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

import net.akehurst.language.agl.api.language.base.QualifiedName
import net.akehurst.language.agl.api.language.base.QualifiedName.Companion.asQualifiedName

object SimpleTypeModelStdLib : TypeNamespaceAbstract(emptyList()) {

    override val qualifiedName: QualifiedName = "std".asQualifiedName

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOrCreateSpecialTypeNamed("Any").type()
    val NothingType = super.findOrCreateSpecialTypeNamed("Nothing").type()
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed("String").type()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed("Boolean").type()
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed("Integer").type()
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed("Real").type()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed("Timestamp").type()

    val List = super.findOwnedOrCreateCollectionTypeNamed("List").also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add("E")
        typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive("first", this.createTypeInstance(typeDecl, "E"), "First element in the List.")
        typeDecl.appendPropertyPrimitive("last", this.createTypeInstance(typeDecl, "E"), "Last element in the list.")
        typeDecl.appendPropertyPrimitive(
            "back",
            this.createTypeInstance(typeDecl, "List", listOf(this.createTypeInstance(typeDecl, "E"))),
            "All elements in the List except the first one."
        )
        typeDecl.appendPropertyPrimitive(
            "front",
            this.createTypeInstance(typeDecl, "List", listOf(this.createTypeInstance(typeDecl, "E"))),
            "All elements in the List except the last one."
        )
        typeDecl.appendPropertyPrimitive("join", this.createTypeInstance(typeDecl, "String"), "The String value of all elements concatenated.")
        typeDecl.appendMethodPrimitive(
            "get",
            listOf(ParameterDefinitionSimple("index", this.createTypeInstance(typeDecl, "Integer"), null)),
            this.createTypeInstance(typeDecl, "E"), "The element at the given index."
        ) { it, arguments ->
            check(it is List<*>) { "Method 'get' is only applicably to List objects." }
            check(1 == arguments.size) { "Method 'get' should only have 1 (Integer) argument." }
            check(arguments[0] is Int)
            val self = it as List<Any>
            val arg1 = arguments[0] as Int
            self[arg1]
        }
    }
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed("ListSeparated").also { typeDecl ->
        typeDecl.addSupertype(List.qualifiedName)
        (typeDecl.typeParameters as MutableList).addAll(listOf("E", "I"))
        //typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive(
            "elements",
            this.createTypeInstance(
                typeDecl, "List",
                listOf(this.createTypeInstance(typeDecl, "E"))
            ),
            "Number of elements in the List."
        )
        typeDecl.appendPropertyPrimitive("items", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
        typeDecl.appendPropertyPrimitive("separators", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.")
    }
    val Set = super.findOwnedOrCreateCollectionTypeNamed("Set").also { (it.typeParameters as MutableList).add("E") }
    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed("OrderedSet").also { (it.typeParameters as MutableList).add("E") }
    val Map = super.findOwnedOrCreateCollectionTypeNamed("Map").also { (it.typeParameters as MutableList).addAll(listOf("K", "V")) }

}


