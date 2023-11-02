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

    val List = super.findOwnedOrCreateCollectionTypeNamed("List").also { typeDecl ->
        (typeDecl.typeParameters as MutableList).add("E")
        typeDecl.appendPropertyPrimitive("size", this.createTypeInstance(typeDecl, "Integer"), "Number of elements in the List.") { it ->
            check(it is List<*>) { "Property 'size' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.size
        }
        typeDecl.appendPropertyPrimitive("first", this.createTypeInstance(typeDecl, "E"), "First element in the List.") { it ->
            check(it is List<*>) { "Property 'first' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.first()
        }
        typeDecl.appendPropertyPrimitive("last", this.createTypeInstance(typeDecl, "E"), "Last element in the list.") { it ->
            check(it is List<*>) { "Property 'last' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.last()
        }
        typeDecl.appendPropertyPrimitive(
            "back",
            this.createTypeInstance(typeDecl, "List", listOf(this.createTypeInstance(typeDecl, "E"))),
            "All elements in the List except the first one."
        ) { it ->
            check(it is List<*>) { "Property 'back' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.drop(1)
        }
        typeDecl.appendPropertyPrimitive(
            "front",
            this.createTypeInstance(typeDecl, "List", listOf(this.createTypeInstance(typeDecl, "E"))),
            "All elements in the List except the last one."
        ) { it ->
            check(it is List<*>) { "Property 'front' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.dropLast(1)
        }
        typeDecl.appendPropertyPrimitive("join", this.createTypeInstance(typeDecl, "String"), "The String value of all elements concatenated.") { it ->
            check(it is List<*>) { "Property 'join' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as List<Any>
            self.joinToString(separator = "")
        }
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
        (typeDecl.typeParameters as MutableList).addAll(listOf("E", "I"))
        typeDecl.appendPropertyPrimitive("join", this.createTypeInstance(typeDecl, "String"), "The String value of all elements (items & separators) concatenated.") { it ->
            check(it is ListSeparated<*, *>) { "Property 'join' is not applicable to '${it::class.simpleName}' objects." }
            val self = it as ListSeparated<Any, Any>
            self.joinToString(separator = "")
        }
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


