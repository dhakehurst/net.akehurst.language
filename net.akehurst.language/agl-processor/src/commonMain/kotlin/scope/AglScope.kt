/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.scope.processor

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.typeModel

object AglScope {

    const val komposite = """namespace net.akehurst.language.scope.api
interface Scope {
    cmp scopeMap
    cmp childScopes
    cmp items
}
"""

    val typeModel: TypeModel by lazy {
        //TODO: NamespaceAbstract._definition wrongly generated with net.akehurst.language.base.asm.NamespaceAbstract.DT
        typeModel("Scope", true, AglBase.typeModel.namespace) {
            namespace("net.akehurst.language.scope.api", listOf("std", "net.akehurst.language.base.api")) {
                interfaceType("Scope") {
                    typeParameters("ItemType")

                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "childScopes", "Map", false) {
                        typeArgument("String")
                        typeArgument("Scope")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "items", "Map", false) {
                        typeArgument("String")
                        typeArgument("Map") {
                            typeArgument("QualifiedName")
                            typeArgument("ItemType")
                        }
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "scopeMap", "Map", false) {
                        typeArgument("ItemType")
                        typeArgument("Scope")
                    }
                }
                dataType("ScopedItem") {
                    typeParameters("ItemType")

                    constructor_ {
                        parameter("referableName", "String", false)
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("item", "ItemType", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "item", "ItemType", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "referableName", "String", false)
                }
            }
            namespace("net.akehurst.language.scope.asm", listOf("net.akehurst.language.scope.api", "std", "net.akehurst.language.base.api")) {
                dataType("ScopeSimple") {
                    typeParameters("ItemType")
                    supertype("Scope") { ref("ItemType") }
                    constructor_ {
                        parameter("parent", "ScopeSimple", false)
                        parameter("scopeIdentityInParent", "String", false)
                        parameter("forTypeName", "QualifiedName", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "childScopes", "Map", false) {
                        typeArgument("String")
                        typeArgument("ScopeSimple")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "forTypeName", "QualifiedName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "items", "Map", false) {
                        typeArgument("String")
                        typeArgument("Map") {
                            typeArgument("QualifiedName")
                            typeArgument("ItemType")
                        }
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "parent", "ScopeSimple", false) {
                        typeArgument("ItemType")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "scopeIdentity", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "scopeIdentityInParent", "String", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "scopeMap", "Map", false) {
                        typeArgument("ItemType")
                        typeArgument("ScopeSimple")
                    }
                }
            }
        }
    }

}