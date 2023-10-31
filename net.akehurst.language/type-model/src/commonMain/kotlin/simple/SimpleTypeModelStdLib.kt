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

import net.akehurst.language.collections.ListSeparated

object SimpleTypeModelStdLib : TypeNamespaceAbstract(emptyList()) {

    override val qualifiedName: String = "std"

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOrCreateSpecialTypeNamed("Any").instance()
    val NothingType = super.findOrCreateSpecialTypeNamed("Nothing").instance()
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed("String").instance()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed("Boolean").instance()
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed("Integer").instance()
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed("Real").instance()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed("Timestamp").instance()

    val List = super.findOwnedOrCreateCollectionTypeNamed("List").also {
        (it.typeParameters as MutableList).add("E")
        it.appendDerivedProperty("size", this.createTypeInstance(it, "Integer"), "BuiltIn: size of the list")
        it.appendDerivedProperty("first", this.createTypeInstance(it, "E"), "BuiltIn: first element of the list")
        it.appendDerivedProperty("last", this.createTypeInstance(it, "E"), "BuiltIn: last element of the list")
        it.appendDerivedProperty("tail", this.createTypeInstance(it, "E"), "BuiltIn: all but the first element of the list")
        it.appendDerivedProperty("front", this.createTypeInstance(it, "E"), "BuiltIn: all but the last element of the list")
        it.appendDerivedProperty("join", this.createTypeInstance(it, "String"), "BuiltIn: all elements concatenated into a String")
        it.appendMethod(
            "get",
            listOf(ParameterDefinitionSimple("index", this.createTypeInstance(it, "Integer"), null)),
            this.createTypeInstance(it, "E"),
            "BuiltIn: the element at the given index"
        )
    }
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed("ListSeparated").also {
        (it.typeParameters as MutableList).addAll(listOf("E", "I"))
        it.appendDerivedProperty("join", this.createTypeInstance(it, "String"), "BuiltIn: all elements concatenated into a String")
    }
    val Set = super.findOwnedOrCreateCollectionTypeNamed("Set").also { (it.typeParameters as MutableList).add("E") }
    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed("OrderedSet").also { (it.typeParameters as MutableList).add("E") }
    val Map = super.findOwnedOrCreateCollectionTypeNamed("Map").also { (it.typeParameters as MutableList).addAll(listOf("K", "V")) }

    val execute = mapOf(
        List to mapOf(
            "size" to { self: List<*> -> self.size },
            "first" to { self: List<*> -> self.first() }
        ),
        ListSeparated to mapOf(
            "size" to { self: ListSeparated<*, *> -> self.size },
            "first" to { self: List<*> -> self.first() },
            "joined" to { self: List<*> -> self.joinToString() }
        ),
    )
}


