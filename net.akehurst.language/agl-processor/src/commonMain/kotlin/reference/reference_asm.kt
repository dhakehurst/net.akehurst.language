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
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.base.api.*
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.DomainAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.reference.api.*
import net.akehurst.language.types.api.TypesDomain

class CrossReferenceDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<CrossReferenceNamespace> = emptyList()
) : DomainAbstract<CrossReferenceNamespace, DeclarationsForNamespace>(namespace,options), CrossReferenceDomain {
    companion object {
        val ROOT_SCOPE_TYPE_NAME = QualifiedName("§root")
        val IDENTIFY_BY_NOTHING = "§nothing"

        fun fromString(context: SentenceContext?, crossReferenceString: CrossReferenceString): ProcessResult<CrossReferenceDomain> {
            val proc = Agl.registry.agl.crossReference.processor ?: error("Agl CrossReference language not found!")
            return proc.process(
                sentence = crossReferenceString.value,
                Agl.options {
                    semanticAnalysis { sentenceContext(context) }
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
        }
    }

    override fun identifyingExpressionFor(scopeForTypeName: SimpleName, possiblyQualifiedTypeName: PossiblyQualifiedName): Expression? {
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
        }
    }

    override fun isScopeDefinedFor(typesDomain: TypesDomain, typeQualifiedName: QualifiedName): Boolean =
        lookupFor(typesDomain).isScopeDefinedFor(typeQualifiedName)

    override fun identifyingExpressionFor(typesDomain: TypesDomain, scopeForTypeQualifiedName: QualifiedName, typeQualifiedName: QualifiedName): CrossReferenceMatch<Identifiable> =
        lookupFor(typesDomain).identifyingExpressionFor(scopeForTypeQualifiedName, typeQualifiedName)

    override fun referencesFor(typesDomain: TypesDomain, typeQualifiedName: QualifiedName): List<CrossReferenceMatch<ReferenceExpression>> =
        lookupFor(typesDomain).referencesFor(typeQualifiedName)

    override fun scopeImportsFor(typesDomain: TypesDomain, typeQualifiedName: QualifiedName): List<ScopeImport> =
        lookupFor(typesDomain).scopeImportsFor(typeQualifiedName)

    private var _lookup: CrossReferenceLookup? = null

    /**
     * The lookup is cached for the last used TypesDomain.
     * The declarations must not change after the first lookup.
     */
    fun lookupFor(typesDomain: TypesDomain): CrossReferenceLookup {
        val lu = _lookup
        return if (null != lu && lu.typesDomain === typesDomain) {
            lu
        } else {
            CrossReferenceLookup(this, typesDomain).also { _lookup = it }
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

    // no trailing indent when there are no declarations
    override fun asString(indent: Indent, imports: List<Import>): String = super.asString(indent, imports).trimEnd()
}

data class DeclarationsForNamespaceDefault(
    override val namespace: CrossReferenceNamespace,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap())
) : DeclarationsForNamespace, DefinitionAbstract<DeclarationsForNamespace>() {

    override val name = SimpleName("Declarations")

    override val importedNamespaces: List<Import> get() = namespace.import

    override val scopeDefinition = mutableMapOf<PossiblyQualifiedName, ScopeDefinition>()
    override val references = mutableListOf<ReferenceDefinition>()

    init {
        namespace.addDefinition(this)
        scopeDefinition[CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last] = ScopeDefinitionDefault(CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last)
    }

    override val isEmpty: Boolean get() = scopeDefinition.isEmpty() && references.isEmpty()

    override fun isScopeDefinedFor(typeName: SimpleName): Boolean {
        return scopeDefinition.keys.any { it.simpleName == typeName }
    }

    override fun referencesFor(typeName: SimpleName): List<ReferenceExpression> {
        return references.filter {
            it.inTypeName.simpleName == typeName
        }.flatMap {
            it.referenceExpressionList
        }
    }

    override fun referenceForPropertyOrNull(typeName: SimpleName, propertyName: String): ReferenceExpression? {
        val refs = referencesFor(typeName)
        return refs.firstOrNull { it is ReferenceExpressionPropertyDefault }
    }

    override fun identifyingExpressionFor(scopeForTypeName: SimpleName, typeName: SimpleName): Expression? {
        val scope = scopeDefinition.entries.firstOrNull { it.key.simpleName == scopeForTypeName }?.value
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName.simpleName == typeName }
        return identifiable?.identifiedBy
    }

    /**
     * in the syntax of the references language, so that the result can be parsed again.
     * The first line has no indent, because the owning namespace adds it.
     */
    override fun asString(indent: Indent, imports: List<Import>): String {
        val lines = mutableListOf<String>()
        // identifiables of the root scope are written at the top level
        scopeDefinition[CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last]?.let { root ->
            root.identifiables.forEach { lines.add(it.asString(indent)) }
        }
        scopeDefinition.values
            .filter { it.scopeForTypeName != CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last }
            .forEach { lines.add(it.asString(indent)) }
        if (references.isNotEmpty()) {
            val refs = references.joinToString(separator = "\n") { it.asString(indent.inc) }
            lines.add("${indent}references {\n$refs\n${indent}}")
        }
        return lines.joinToString(separator = "\n").removePrefix(indent.value)
    }
}

data class ScopeDefinitionDefault(
    override val scopeForTypeName: PossiblyQualifiedName
) : ScopeDefinition {
    override val identifiables = mutableListOf<Identifiable>()
    override val scopeImports = mutableListOf<ScopeImport>()

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("${indent}scope ${scopeForTypeName.value} {")
        if (this.identifiables.isEmpty() && this.scopeImports.isEmpty()) {
            sb.append(" }")
        } else {
            val lines = this.identifiables.map { it.asString(indent.inc) } + this.scopeImports.map { it.asString(indent.inc) }
            sb.append("\n")
            sb.append(lines.joinToString(separator = "\n"))
            sb.append("\n$indent}")
        }
        return sb.toString()
    }
}

data class IdentifiableDefault(
    override val typeName: PossiblyQualifiedName,
    override val identifiedBy: Expression,
    override val visibility: Expression? = null,
    override val aliasFor: NavigationExpression? = null,
    override val identifiedByAlternatives: List<Expression> = listOf(identifiedBy)
) : Identifiable {
    override fun asString(indent: Indent, imports: List<Import>): String {
        val vis = visibility?.let { " visibility ${it.asString(indent, emptyList())}" } ?: ""
        val alias = aliasFor?.let { " alias-for ${it.asString(indent, emptyList())}" } ?: ""
        val by = identifiedByAlternatives.joinToString(separator = " | ") { it.asString(indent, emptyList()) }
        return "${indent}identify ${typeName.value} by $by$vis$alias"
    }
}

data class ScopeImportDefault(
    override val kind: ScopeImportKind,
    override val elements: Expression,
    override val ofType: PossiblyQualifiedName?,
    override val target: NavigationExpression,
    override val recursive: Expression? = null,
    override val visibility: Expression? = null
) : ScopeImport {
    override fun asString(indent: Indent, imports: List<Import>): String {
        val kw = when (kind) {
            ScopeImportKind.IMPORT_MEMBERS -> "import"
            ScopeImportKind.IMPORT_ELEMENT -> "import"
            ScopeImportKind.INHERIT -> "inherit"
        }
        val what = when (kind) {
            ScopeImportKind.IMPORT_ELEMENT -> "element"
            else -> "members-of"
        }
        val ot = ofType?.let { " of-type ${it.value}" } ?: ""
        val rec = recursive?.let { " recursive ${it.asString(indent, emptyList())}" } ?: ""
        val vis = visibility?.let { " visibility ${it.asString(indent, emptyList())}" } ?: ""
        return "${indent}$kw forall ${elements.asString(indent, emptyList())}$ot $what ${target.asString(indent, emptyList())}$rec$vis"
    }
}

data class ReferenceDefinitionDefault(
    /**
     * name of the asm type in which the property is a reference
     */
    override val inTypeName: PossiblyQualifiedName,

    override val referenceExpressionList: List<ReferenceExpression>
) : ReferenceDefinition {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("${indent}in ${inTypeName.value} {")
        when {
            referenceExpressionList.isEmpty() -> sb.append(" }")
            else -> {
                sb.append("\n")
                sb.append(referenceExpressionList.joinToString(separator = "\n") { it.asString(indent.inc) })
                sb.append("\n${indent}}")
            }
        }
        return sb.toString()
    }
}

abstract class ReferenceExpressionAbstract : ReferenceExpression

data class ReferenceExpressionPropertyDefault(
    override val referringPropertyNavigation: NavigationExpression,
    override val refersToTypeName: List<PossiblyQualifiedName>,
    override val fromNavigation: NavigationExpression?,
    override val condition: Expression? = null,
    override val fromElseNavigation: NavigationExpression? = null
) : ReferenceExpressionAbstract(), ReferenceExpressionProperty {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        val rp = referringPropertyNavigation.asString(indent, emptyList())
        val rt = refersToTypeName.joinToString(separator = " | ") { it.value }
        val fr = when (fromNavigation) {
            null -> ""
            else -> " from " + fromNavigation.asString(indent, emptyList()) + (fromElseNavigation?.let { " else " + it.asString(indent, emptyList()) } ?: "")
        }
        val cn = when (condition) {
            null -> ""
            else -> " when " + condition.asString(indent, emptyList())
        }
        sb.append("${indent}property $rp refers-to $rt$fr$cn")
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

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        val ex = expression.asString(indent, emptyList())
        val ot = when (ofType) {
            null -> ""
            else -> " of-type " + ofType.value
        }
        sb.append("${indent}forall $ex$ot {")
        when {
            referenceExpressionList.isEmpty() -> sb.append(" }")
            else -> {
                sb.append("\n")
                sb.append(referenceExpressionList.joinToString(separator = "\n") { it.asString(indent.inc) })
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