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

package net.akehurst.language.api.semanticAnalyser

interface ScopeModel {

    /**
     * Is the property inTypeName.propertyName a reference ?
     *
     * @param inTypeName name of the asm type in which contains the property
     * @param propertyName name of the property that might be a reference
     */
    fun isReference(inTypeName: String, propertyName: String): Boolean

    /**
     *
     * Find the name of the type referred to by the property inTypeName.referringPropertyName
     *
     * @param inTypeName name of the asm type in which the property is a reference
     * @param referringPropertyName name of the property that is a reference
     */
    fun getReferredToTypeNameFor(inTypeName: String, referringPropertyName: String): List<String>
}

/**
 * E - type of elements in the scope
 */
interface Scope<AsmElementIdType> {

    val items: Map<String, Map<String, AsmElementIdType>>

    val childScopes: Map<String, Scope<AsmElementIdType>>

    fun isMissing(referableName: String, typeName: String): Boolean

    fun findOrNull(referableName: String, typeName: String): AsmElementIdType?

    fun asString(currentIndent: String = "", indentIncrement: String = " "): String
}