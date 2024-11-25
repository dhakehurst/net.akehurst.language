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

package net.akehurst.language.reference.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.reference.api.*

class CrossReferenceModelDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<CrossReferenceNamespace> = emptyList()
) : ModelAbstract<CrossReferenceNamespace, DeclarationsForNamespace>(namespace,options), CrossReferenceModel {
    companion object {
        val ROOT_SCOPE_TYPE_NAME = QualifiedName("§root")
        val IDENTIFY_BY_NOTHING = "§nothing"

        fun fromString(context: ContextFromTypeModel?, crossReferenceString: CrossReferenceString): ProcessResult<CrossReferenceModel> {
            val proc = Agl.registry.agl.crossReference.processor ?: error("Agl CrossReference language not found!")
            return proc.process(
                sentence = crossReferenceString.value,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override val declarationsForNamespace get() = namespace.associate { Pair(it.qualifiedName, it.declarationsForNamespace) }

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

    override fun referenceForProperty(typeQualifiedName: QualifiedName, propertyName: String): List<QualifiedName> =
        _referenceForProperty[Pair(typeQualifiedName, propertyName)] ?: emptyList()

    fun addRecordReferenceForProperty(typeQualifiedName: QualifiedName, propertyName: String, refersToQualifiedTypeNames: List<QualifiedName>) {
        _referenceForProperty[Pair(typeQualifiedName, propertyName)] = refersToQualifiedTypeNames
    }

    private val _referenceForProperty = mutableMapOf<Pair<QualifiedName, String>, List<QualifiedName>>()
}

class CrossReferenceNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : NamespaceAbstract<DeclarationsForNamespace>(options, import), CrossReferenceNamespace {

    override val declarationsForNamespace get() = this.definition.first()
}

data class DeclarationsForNamespaceDefault(
    override val namespace: CrossReferenceNamespace,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap())
) : DeclarationsForNamespace, DefinitionAbstract<DeclarationsForNamespace>() {

    override val name = SimpleName("Declarations")

    override val importedNamespaces: List<Import> get() = namespace.import

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

    override fun referenceForPropertyOrNull(typeName: SimpleName, propertyName: String): ReferenceExpression? {
        val refs = referencesFor(typeName)
        return refs.firstOrNull { it is ReferenceExpressionPropertyDefault }
    }

    override fun identifyingExpressionFor(scopeForTypeName: SimpleName, typeName: SimpleName): Expression? {
        val scope = scopeDefinition[scopeForTypeName]
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.identifiedBy
    }

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val scps = scopeDefinition.values.joinToString(separator = "\n") {
            "$indent${it.asString(indent)}"
        }
        sb.append(scps)
        return sb.toString()
    }
}

data class ScopeDefinitionDefault(
    override val scopeForTypeName: SimpleName
) : ScopeDefinition {
    override val identifiables = mutableListOf<Identifiable>()

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("scope ${scopeForTypeName.value} {")
        if (this.identifiables.isEmpty()) {
            sb.append(" }")
        } else {
            sb.append("\n")
            for (id in this.identifiables) {
                sb.append(id.asString(indent.inc))
            }
            sb.append("\n$indent}")
        }
        return sb.toString()
    }
}

data class IdentifiableDefault(
    override val typeName: SimpleName,
    override val identifiedBy: Expression
) : Identifiable {
    override fun asString(indent: Indent): String {
        return "${indent}identify ${typeName.value} by ${identifiedBy.asString(indent, emptyList())}"
    }
}

data class ReferenceDefinitionDefault(
    /**
     * name of the asm type in which the property is a reference
     */
    override val inTypeName: SimpleName,

    override val referenceExpressionList: List<ReferenceExpression>
) : ReferenceDefinition {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("in ${inTypeName.value} {\n")
        for (re in referenceExpressionList) {
            sb.append(re.asString(indent.inc))
        }
        return sb.toString()
    }
}

abstract class ReferenceExpressionAbstract : ReferenceExpression

data class ReferenceExpressionPropertyDefault(
    override val referringPropertyNavigation: NavigationExpression,
    override val refersToTypeName: List<PossiblyQualifiedName>,
    override val fromNavigation: NavigationExpression?
) : ReferenceExpressionAbstract(), ReferenceExpressionProperty {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val rp = referringPropertyNavigation.asString(indent, emptyList())
        val rt = refersToTypeName.joinToString(separator = " | ") { it.value }
        val fr = when (fromNavigation) {
            null -> ""
            else -> " " + fromNavigation.asString(indent, emptyList())
        }
        sb.append("${indent}property $rp refers-to $rt$fr")
        return sb.toString()
    }

    /*    override fun isReference(propertyName: String): Boolean {
            return this.referringPropertyName == propertyName
        }

        override fun referredToTypeNameFor(propertyName: String): List<String> = when (propertyName) {
            this.referringPropertyName -> this.refersToTypeName
            else -> emptyList()
        }*/
}

data class ReferenceExpressionCollectionDefault(
    override val expression: Expression,
    override val ofType: PossiblyQualifiedName?,
    override val referenceExpressionList: List<ReferenceExpressionAbstract>
) : ReferenceExpressionAbstract(), ReferenceExpressionCollection {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val ex = expression.asString(indent, emptyList())
        val ot = when (ofType) {
            null -> ""
            else -> " of-type" + ofType.value
        }
        sb.append("${indent}forall $ex$ot refers-to {\n")
        when {
            referenceExpressionList.isEmpty() -> sb.append(" }")
            else -> {
                for(re in referenceExpressionList) {
                    sb.append(re.asString(indent.inc))
                }
                sb.append("\n${indent}}")
            }
        }
        return sb.toString()
    }

    /*    override fun isReference(propertyName: String): Boolean {
            return this.referenceExpressionList.any { it.isReference(propertyName) }
        }

        override fun referredToTypeNameFor(propertyName: String): List<String> = emptyList()*/

}