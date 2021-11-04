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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.grammar.scopes.ScopeModel
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.ScopeSimple
import net.akehurst.language.api.processor.SentenceContext

class ContextSimple() : SentenceContext {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<AsmElementPath>(null, "", ScopeModel.ROOT_SCOPE_TYPE_NAME)
}



fun ScopeModel.createReferenceLocalToScope(scope: ScopeSimple<AsmElementPath>, element: AsmElementSimple): String? {
    val prop = this.getReferablePropertyNameFor(scope.forTypeName, element.typeName)
    return when (prop) {
        null -> null
        ScopeModel.IDENTIFY_BY_NOTHING -> ""
        else -> element.getPropertyAsString(prop)
    }
}

//fun ScopeModel.createReferenceFromRoot(scope: ScopeSimple<AsmElementPath>, element: AsmElementSimple): AsmElementPath {
//    return element.asmPath
//}

//fun ScopeModel.resolveReference(asm:AsmSimple, rootScope: ScopeSimple<AsmElementPath>, reference: AsmElementPath): AsmElementSimple? {
//    return asm.index[reference]
//}