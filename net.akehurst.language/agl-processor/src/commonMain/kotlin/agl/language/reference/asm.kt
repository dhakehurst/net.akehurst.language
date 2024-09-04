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

package net.akehurst.language.agl.language.reference.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.NavigationExpression
import net.akehurst.language.api.language.reference.*
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.PropertyName

class CrossReferenceModelDefault
    : CrossReferenceModel {
    companion object {
        val ROOT_SCOPE_TYPE_NAME = QualifiedName("§root")
        val IDENTIFY_BY_NOTHING = "§nothing"

        fun fromString(context: ContextFromTypeModel?, aglScopeModelSentence: String): ProcessResult<CrossReferenceModel> {
            val proc = Agl.registry.agl.crossReference.processor ?: error("Agl CrossReference language not found!")
            return proc.process(
                sentence = aglScopeModelSentence,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override val declarationsForNamespace = mutableMapOf<QualifiedName, DeclarationsForNamespace>()

    override val isEmpty: Boolean get() = declarationsForNamespace.isEmpty() || declarationsForNamespace.values.all { it.isEmpty }

    override fun isScopeDefinedFor(possiblyQualifiedTypeName: PossiblyQualifiedName): Boolean {
        return when (possiblyQualifiedTypeName) {
            is QualifiedName -> {
                val qn = possiblyQualifiedTypeName
                val qName = qn.front
                val tName = qn.last
                val ns = this.declarationsForNamespace[qName]
                return ns?.isScopeDefinedFor(tName) ?: false
            }

            is SimpleName -> {
                declarationsForNamespace.values.any {
                    it.isScopeDefinedFor(possiblyQualifiedTypeName)
                }
            }

            else -> error("Unsupported")
        }
    }

    override fun referencesFor(possiblyQualifiedTypeName: PossiblyQualifiedName): List<ReferenceExpression> {
        return when (possiblyQualifiedTypeName) {
            is QualifiedName -> {
                val qn = possiblyQualifiedTypeName
                val qName = qn.front
                val tName = qn.last
                val ns = this.declarationsForNamespace[qName]
                return ns?.referencesFor(tName) ?: emptyList()
            }

            is SimpleName -> {
                declarationsForNamespace.values.flatMap {
                    it.referencesFor(possiblyQualifiedTypeName)
                }
            }

            else -> error("Unsupported")
        }
    }

    fun identifyingExpressionFor(scopeForTypeName: SimpleName, possiblyQualifiedTypeName: PossiblyQualifiedName): Expression? {
        return when (possiblyQualifiedTypeName) {
            is QualifiedName -> {
                val qn = possiblyQualifiedTypeName
                val qName = qn.front
                val tName = qn.last
                val ns = this.declarationsForNamespace[qName]
                return ns?.identifyingExpressionFor(scopeForTypeName, tName)
            }

            is SimpleName -> {
                declarationsForNamespace.values.firstNotNullOfOrNull {
                    it.identifyingExpressionFor(scopeForTypeName, possiblyQualifiedTypeName)
                }
            }

            else -> error("Unsupported")
        }
    }

    override fun referenceForProperty(typeQualifiedName: QualifiedName, propertyName: PropertyName): List<QualifiedName> =
        _referenceForProperty[Pair(typeQualifiedName, propertyName)] ?: emptyList()

    fun addRecordReferenceForProperty(typeQualifiedName: QualifiedName, propertyName: PropertyName, refersToQualifiedTypeNames: List<QualifiedName>) {
        _referenceForProperty[Pair(typeQualifiedName, propertyName)] = refersToQualifiedTypeNames
    }

    private val _referenceForProperty = mutableMapOf<Pair<QualifiedName, PropertyName>, List<QualifiedName>>()
}

data class DeclarationsForNamespaceDefault(
    override val qualifiedName: QualifiedName,
    override val importedNamespaces: List<Import>
) : DeclarationsForNamespace {
    override val scopeDefinition = mutableMapOf<SimpleName, ScopeDefinition>()
    override val references = mutableListOf<ReferenceDefinition>()

    init {
        scopeDefinition[CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME.last] = ScopeDefinitionDefault(CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME.last)
    }

    override val isEmpty: Boolean get() = scopeDefinition.isEmpty() && references.isEmpty()

    override fun isScopeDefinedFor(typeName: SimpleName): Boolean {
        return scopeDefinition.containsKey(typeName)
    }

    override fun referencesFor(typeName: SimpleName): List<ReferenceExpression> {
        return references.filter {
            it.inTypeName == typeName
        }.flatMap {
            it.referenceExpressionList
        }
    }

    override fun referenceForPropertyOrNull(typeName: SimpleName, propertyName: PropertyName): ReferenceExpression? {
        val refs = referencesFor(typeName)
        return refs.firstOrNull { it is PropertyReferenceExpressionDefault }
    }

    override fun identifyingExpressionFor(scopeForTypeName: SimpleName, typeName: SimpleName): Expression? {
        val scope = scopeDefinition[scopeForTypeName]
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.identifiedBy
    }
}

data class ScopeDefinitionDefault(
    override val scopeForTypeName: SimpleName
) : ScopeDefinition {
    override val identifiables = mutableListOf<Identifiable>()
}

data class IdentifiableDefault(
    override val typeName: SimpleName,
    override val identifiedBy: Expression
) : Identifiable

data class ReferenceDefinitionDefault(
    /**
     * name of the asm type in which the property is a reference
     */
    override val inTypeName: SimpleName,

    override val referenceExpressionList: List<ReferenceExpression>
) : ReferenceDefinition {
}

abstract class ReferenceExpressionAbstract : ReferenceExpression

data class PropertyReferenceExpressionDefault(
    /**
     * navigation to the property that is a reference
     */
    val referringPropertyNavigation: NavigationExpression,

    /**
     * type of the asm element referred to
     */
    val refersToTypeName: List<PossiblyQualifiedName>,

    val fromNavigation: NavigationExpression?
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
    val expression: Expression,
    val ofType: PossiblyQualifiedName?,
    val referenceExpressionList: List<ReferenceExpressionAbstract>
) : ReferenceExpressionAbstract() {
    /*    override fun isReference(propertyName: String): Boolean {
            return this.referenceExpressionList.any { it.isReference(propertyName) }
        }

        override fun referredToTypeNameFor(propertyName: String): List<String> = emptyList()*/

}