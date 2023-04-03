/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.grammar

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarExeception
import net.akehurst.language.api.grammar.GrammarRegistry

/*
object GrammarRegistryDefault : GrammarRegistry {

    // <namespace, <grammar.name, Grammar>>
    private val registry = mutableMapOf<String, MutableMap<String, Grammar>>()

    override fun register(grammar: Grammar) {
        var ns = this.registry[grammar.namespace.qualifiedName]
        if (null == ns) {
            ns = mutableMapOf<String, Grammar>()
            this.registry[grammar.namespace.qualifiedName] = ns
        }
        ns[grammar.name] = grammar
    }

    override fun find(localNamespace: String, qualifiedName: String): Grammar {
        val split = qualifiedName.split(".")
        val ns_name = if (qualifiedName.contains(".")) {
            Pair(qualifiedName.substringBefore("."), qualifiedName.substringAfterLast("."))
        } else {
            Pair(localNamespace, qualifiedName)
        }
        val regNs = this.registry[ns_name.first]
        return if (null == regNs) {
            throw GrammarExeception("Grammar ${qualifiedName} not found in GrammarRegistry", null)
        } else {
            regNs[ns_name.second] ?: throw GrammarExeception("Grammar '${qualifiedName}' not found in GrammarRegistry", null)
        }

    }

}
 */