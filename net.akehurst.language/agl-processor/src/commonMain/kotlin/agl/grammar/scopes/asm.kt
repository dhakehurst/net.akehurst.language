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
package net.akehurst.language.agl.grammar.scopes

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.syntaxAnalyser.ScopeSimple
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementReference
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.children
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.processor.SentenceContext

class ScopeModelAgl
    : ScopeModel
{
    companion object {
        val ROOT_SCOPE_TYPE_NAME = "§root"
        val IDENTIFY_BY_NOTHING = "§nothing"

        fun fromString(context: SentenceContext<GrammarItem>, aglScopeModelSentence:String): ProcessResult<ScopeModelAgl> {
            val proc = Agl.registry.agl.scopes.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglScopeModelSentence,
                Agl.options {
                    syntaxAnalysis {
                        context(context)
                    }
                }
            )
        }
    }

    val scopes = mutableMapOf<String,ScopeDefinition>()
    val references = mutableListOf<ReferenceDefinition>()

    init {
        scopes[ROOT_SCOPE_TYPE_NAME]=ScopeDefinition(ROOT_SCOPE_TYPE_NAME)
    }

    fun isScopeDefinition(scopeFor: String): Boolean {
        return scopes.containsKey(scopeFor)
    }

    override fun isReference(inTypeName: String, propertyName: String): Boolean {
        return references.any {
            it.inTypeName == inTypeName
                    && it.referringPropertyName == propertyName
        }
    }

    fun getReferablePropertyNameFor(scopeFor: String, typeName: String): String? {
        val scope = scopes[scopeFor]
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.propertyName
    }

    override fun getReferredToTypeNameFor(inTypeName: String, referringPropertyName: String): List<String> {
        val def = references.firstOrNull { it.inTypeName == inTypeName && it.referringPropertyName==referringPropertyName }
        return def?.refersToTypeName ?: emptyList()
    }

    fun shouldCreateReference(scopeFor: String, typeName: String): Boolean {
        return null!=getReferablePropertyNameFor(scopeFor,typeName)
    }

    internal fun resolveReferencesElement(
        issues: IssueHolder,
        el: AsmElementSimple,
        locationMap: Map<Any, InputLocation>?,
        parentScope: ScopeSimple<AsmElementPath>?
    ) {
        val elScope = parentScope?.rootScope?.scopeMap?.get(el.asmPath) ?: parentScope
        if (null != elScope) {
            el.properties.forEach { e ->
                val prop = e.value
                if (prop.isReference) {
                    val v = prop.value
                    if (null == v) {
                        //can't set reference, but no issue
                    } else if (v is AsmElementReference) {
                        val typeNames = this.getReferredToTypeNameFor(el.typeName, prop.name)
                        val referreds: List<AsmElementPath> = typeNames.mapNotNull {
                            elScope.findOrNull(v.reference, it)
                        }
                        if (1 < referreds.size) {
                            issues.warn(
                                locationMap?.get(el),//TODO: should be property location
                                "Multiple options for '${v.reference}' as reference for '${el.typeName}.${prop.name}'"
                            )
                        }
                        val referred = referreds.firstOrNull()
                        if (null == referred) {
                            val location = locationMap?.get(el) //TODO: should be property location
                            issues.error(
                                location,
                                "Cannot find '${v.reference}' as reference for '${el.typeName}.${prop.name}'"
                            )
                        } else {
                            val rel = el.asm.index[referred]
                            el.getPropertyAsReferenceOrNull(prop.name)?.value = rel
                        }

                    } else {
                        val location = locationMap?.get(el) //TODO: should be property location
                        issues.error(
                            location,
                            "Cannot resolve reference property '${el.typeName}.${prop.name}' because it is not defined as a reference"
                        )
                    }
                } else {
                    // no need to resolve
                }
            }
            el.children.forEach {
                resolveReferencesElement(issues, it, locationMap, elScope)
            }
        }
    }
}

data class ScopeDefinition(
    val scopeFor: String
) {
    val identifiables = mutableListOf<Identifiable>()
}

data class Identifiable(
    val typeName: String,
    val propertyName: String
)

data class ReferenceDefinition(
    /**
     * name of the asm type in which the property is a reference
     */
    val inTypeName: String,
    /**
     * name of the property that is a reference
     */
    val referringPropertyName: String,
    /**
     * type of the asm element referred to
     */
    val refersToTypeName: List<String>
)