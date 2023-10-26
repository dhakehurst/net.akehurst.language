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

package net.akehurst.language.agl.language.scopes

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.semanticAnalyser.*

class ScopeModelAgl
    : ScopeModel {
    companion object {
        val ROOT_SCOPE_TYPE_NAME = "§root"
        val IDENTIFY_BY_NOTHING = "§nothing"

        fun fromString(context: SentenceContext<String>?, aglScopeModelSentence: String): ProcessResult<ScopeModelAgl> {
            val proc = Agl.registry.agl.scopes.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglScopeModelSentence,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override val declarationsForNamespace = mutableMapOf<String, DeclarationsForNamespace>()

    override fun isScopeDefinedFor(possiblyQualifiedTypeName: String): Boolean {
        return when {
            possiblyQualifiedTypeName.contains(".") -> {
                val qName = possiblyQualifiedTypeName.substringBeforeLast(".")
                val tName = possiblyQualifiedTypeName.substringAfterLast(".")
                val ns = this.declarationsForNamespace[qName]
                return ns?.isScopeDefinedFor(tName) ?: false
            }

            else -> {
                declarationsForNamespace.values.any {
                    it.isScopeDefinedFor(possiblyQualifiedTypeName)
                }
            }
        }
    }

    override fun referencesFor(possiblyQualifiedTypeName: String): List<ReferenceExpression> {
        return when {
            possiblyQualifiedTypeName.contains(".") -> {
                val qName = possiblyQualifiedTypeName.substringBeforeLast(".")
                val tName = possiblyQualifiedTypeName.substringAfterLast(".")
                val ns = this.declarationsForNamespace[qName]
                return ns?.referencesFor(tName) ?: emptyList()
            }

            else -> {
                declarationsForNamespace.values.flatMap {
                    it.referencesFor(possiblyQualifiedTypeName)
                }
            }
        }
    }

    fun identifyingExpressionFor(scopeForTypeName: String, possiblyQualifiedTypeName: String): Expression? {
        return when {
            possiblyQualifiedTypeName.contains(".") -> {
                val qName = possiblyQualifiedTypeName.substringBeforeLast(".")
                val tName = possiblyQualifiedTypeName.substringAfterLast(".")
                val ns = this.declarationsForNamespace[qName]
                return ns?.identifyingExpressionFor(scopeForTypeName, tName)
            }

            else -> {
                declarationsForNamespace.values.firstNotNullOfOrNull {
                    it.identifyingExpressionFor(scopeForTypeName, possiblyQualifiedTypeName)
                }
            }
        }
    }
}

data class DeclarationsForNamespaceDefault(
    override val qualifiedName: String
) : DeclarationsForNamespace {
    override val scopes = mutableMapOf<String, ScopeDefinitionDefault>()
    override val references = mutableListOf<ReferenceDefinitionDefault>()

    init {
        scopes[ScopeModelAgl.ROOT_SCOPE_TYPE_NAME] = ScopeDefinitionDefault(ScopeModelAgl.ROOT_SCOPE_TYPE_NAME)
    }

    override fun isScopeDefinedFor(typeName: String): Boolean {
        return scopes.containsKey(typeName)
    }

    override fun referencesFor(typeName: String): List<ReferenceExpressionAbstract> {
        return references.filter {
            it.inTypeName == typeName
        }.flatMap {
            it.referenceExpressionList
        }
    }

    override fun identifyingExpressionFor(scopeForTypeName: String, typeName: String): Expression? {
        val scope = scopes[scopeForTypeName]
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.identifiedBy
    }
}

data class ScopeDefinitionDefault(
    override val scopeForTypeName: String
) : ScopeDefinition {
    override val identifiables = mutableListOf<IdentifiableDefault>()
}

data class IdentifiableDefault(
    override val typeName: String,
    override val identifiedBy: ExpressionAbstract
) : Identifiable

abstract class ExpressionAbstract : Expression {
}

data class NavigationDefault(
    override val value: List<String>
) : ExpressionAbstract(), Navigation {
    constructor(vararg values: String) : this(values.toList())

    override fun toString(): String = value.joinToString(separator = ".")
}

data class RootExpressionDefault(
    val value: String
) : ExpressionAbstract(), RootExpression {
    companion object {
        const val NOTHING = "§nothing"
        const val SELF = "§self"
    }

    override val isNothing: Boolean get() = NOTHING == this.value
    override val isSelf: Boolean get() = SELF == this.value
}

data class ReferenceDefinitionDefault(
    /**
     * name of the asm type in which the property is a reference
     */
    val inTypeName: String,

    val referenceExpressionList: List<ReferenceExpressionAbstract>
) : ReferenceDefinition {
}

abstract class ReferenceExpressionAbstract : ReferenceExpression

data class PropertyReferenceExpressionDefault(
    /**
     * navigation to the property that is a reference
     */
    val referringPropertyNavigation: NavigationDefault,

    /**
     * type of the asm element referred to
     */
    val refersToTypeName: List<String>,

    val fromNavigation: NavigationDefault?
) : ReferenceExpressionAbstract() {

    /*    override fun isReference(propertyName: String): Boolean {
            return this.referringPropertyName == propertyName
        }

        override fun referredToTypeNameFor(propertyName: String): List<String> = when (propertyName) {
            this.referringPropertyName -> this.refersToTypeName
            else -> emptyList()
        }*/
}

data class CollectionReferenceExpressionDefault(
    val navigation: NavigationDefault,
    val ofType: String?,
    val referenceExpressionList: List<ReferenceExpressionAbstract>
) : ReferenceExpressionAbstract() {
    /*    override fun isReference(propertyName: String): Boolean {
            return this.referenceExpressionList.any { it.isReference(propertyName) }
        }

        override fun referredToTypeNameFor(propertyName: String): List<String> = emptyList()*/

}