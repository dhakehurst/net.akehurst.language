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

import net.akehurst.language.api.analyser.AnalyserIssue
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.ScopeSimple
import net.akehurst.language.api.asm.children

class ContextSimple(
    val scopes: Map<String, String>,
    val references:Map<Pair<String,String>,Any>  // TypeName,propertyName  -> ??
) {

    private val _issues = mutableListOf<AnalyserIssue>()
    private val _scopeAsm = ScopeSimple<AsmElementSimple>(null)
    private val _scopes = mutableMapOf<AsmElementSimple, ScopeSimple<AsmElementSimple>>()

    fun resolveReferences(asm: AsmSimple) : List<AnalyserIssue> {
        _issues.clear()
        asm.rootElements.forEach {
            createScopes(_scopeAsm, it)
        }
        asm.rootElements.forEach {
            resolveReferencesElement(it)
        }
        return _issues
    }

    fun isReference(el: AsmElementSimple, name: String): Boolean {
        return references.containsKey(Pair(el.typeName,name))
    }

    private fun createScopes(parentScope:ScopeSimple<AsmElementSimple>, el: AsmElementSimple) {
        if (isReferable(el)) {
            val referable = getReferable(el)
            parentScope.items[referable] = el
        }
        //TODO: could save an asm iteration if this was done as we build the asm!
        if (scopes.containsKey(el.typeName)) {
            val newScope = ScopeSimple<AsmElementSimple>(parentScope)
            _scopes[el] = newScope
            el.children.forEach {
                createScopes(newScope, it)
            }
        } else {
            el.children.forEach {
                createScopes(parentScope, it)
            }
        }

    }

    private fun isReferable(el: AsmElementSimple) :Boolean {

    }

    private fun getReferable(el: AsmElementSimple) :String {

    }

    private fun resolveReferencesElement(el: AsmElementSimple) {
        el.properties.forEach {
            if (it.isReference) {

            } else {
                // no need to resolve
            }
        }
    }
}