/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.asm

import net.akehurst.language.agl.syntaxAnalyser.ScopeSimple

/**
 * E - type of elements in the scope
 */
interface Scope<AsmElementIdType> {

    val items: Map<String, Map<String, AsmElementIdType>>

    val childScopes: Map<String, ScopeSimple<AsmElementIdType>>

    fun isMissing(referableName: String, typeName: String): Boolean

    fun findOrNull(referableName: String, typeName: String): AsmElementIdType?

}