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
        typeModel("Scope", true, AglBase.typesModel.namespace) {
            namespace("net.akehurst.language.scope.api", listOf("std", "net.akehurst.language.base.api")) {
                interface_("Scope") {
                    typeParameters("ItemInScopeType")

                    propertyOf(setOf(VAR, CMP, STR), "childScopes", "Map", false){
                        typeArgument("String")
                        typeArgument("Scope")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "items", "Map", false){
                        typeArgument("String")
                        typeArgument("Map") {
                            typeArgument("QualifiedName")
                            typeArgument("Pair") {
                                typeArgument("Any", nullable = true)
                                typeArgument("ItemInScopeType")
                            }
                        }
                    }
                }
                data("ItemInScope") {
                    typeParameters("ItemInScopeType")

                    constructor_ {
                        parameter("referableName", "String", false)
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("location", "Any", false)
                        parameter("item", "ItemInScopeType", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "item", "ItemInScopeType", false)
                    propertyOf(setOf(VAL, REF, STR), "location", "Any", false)
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAL, REF, STR), "referableName", "String", false)
                }
            }
            namespace("net.akehurst.language.scope.asm", listOf("net.akehurst.language.scope.api", "std", "net.akehurst.language.base.api")) {
                data("ScopeSimple") {
                    typeParameters("ItemInScopeType")
                    supertype("Scope"){ ref("ItemInScopeType") }
                    constructor_ {
                        parameter("parent", "ScopeSimple", false)
                        parameter("scopeIdentityInParent", "String", false)
                        parameter("forTypeName", "QualifiedName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "childScopes", "Map", false){
                        typeArgument("String")
                        typeArgument("ScopeSimple")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "forTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "items", "Map", false){
                        typeArgument("String")
                        typeArgument("Map") {
                            typeArgument("QualifiedName")
                            typeArgument("Pair") {
                                typeArgument("Any", nullable = true)
                                typeArgument("ItemInScopeType")
                            }
                        }
                    }
                    propertyOf(setOf(VAL, REF, STR), "parent", "ScopeSimple", false){
                        typeArgument("ItemInScopeType")
                    }
                    propertyOf(setOf(VAL, REF, STR), "scopeIdentityInParent", "String", false)
                }
            }
        }
    }

}