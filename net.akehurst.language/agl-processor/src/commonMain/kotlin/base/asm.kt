/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.base.asm

import net.akehurst.language.base.api.*

class OptionHolderDefault(
    override var parent: OptionHolder? = null,
    val options: Map<String, String> = emptyMap()
) : OptionHolder {

    override operator fun get(name: String): String? {
        return this.options[name] ?: this.parent?.get(name)
    }

    override fun clone(parent: OptionHolder?): OptionHolder = OptionHolderDefault(parent, this.options)

    override fun toString(): String {
        return options.entries.joinToString { (k,v) -> "${k}: ${v}" }
    }

    override fun hashCode(): Int  = options.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is OptionHolderDefault -> false
        parent != other.parent -> false
        options != other.options -> false
        else -> true
    }
}

abstract class DomainAbstract<NT : Namespace<DT>, DT : Definition<DT>>(
    namespace: List<NT>,
    override val options: OptionHolder
) : Domain<NT, DT> {

    override val allDefinitions: List<DT> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = allDefinitions.isEmpty()

    override val namespace: List<NT> get() = _namespace.values.toList()

    private val _namespace: Map<QualifiedName, NT> = mutableMapOf<QualifiedName, NT>().also { map ->
        namespace.forEach { map[it.qualifiedName] = it }
    }

    init {
        connectionNamespaceOptionHolderParentsToThis()
    }

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>? = _namespace[qualifiedName]

    override fun findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn: PossiblyQualifiedName): DT? = when(pqn) {
        is QualifiedName -> findDefinitionByQualifiedNameOrNull(pqn)
        is SimpleName -> findFirstDefinitionByNameOrNull(pqn)
    }

    override fun findDefinitionByQualifiedNameOrNull(qualifiedName: QualifiedName) =
        _namespace[qualifiedName.front]?.findOwnedDefinitionOrNull(qualifiedName.last)

    override fun findFirstDefinitionByNameOrNull(simpleName: SimpleName): DT? {
        for (ns in namespace) {
            val t = ns.findDefinitionOrNull(simpleName)
            if (null != t) {
                return t
            }
        }
        return null
    }

    override fun resolveReference(reference: DefinitionReference<DT>): DT? {
        val res = when (reference.nameOrQName) {
            is QualifiedName -> findDefinitionByQualifiedNameOrNull(reference.nameOrQName as QualifiedName)
            is SimpleName -> findDefinitionByQualifiedNameOrNull(reference.nameOrQName.asQualifiedName(reference.localNamespace.qualifiedName))
        }
        return if (null != res) {
            reference.resolveAs(res)
            res
        } else {
            null
        }
    }

    fun connectionNamespaceOptionHolderParentsToThis() {
        this.namespace.forEach { it.options.parent = this.options }
    }

    override fun addNamespace(value: NT) {
        if (_namespace.containsKey(value.qualifiedName)) {
            if (_namespace[value.qualifiedName] === value) {
                //same object, no need to add it
            } else {
                error("TypesDomain '${this.name}' already contains a namespace '${value.qualifiedName}'")
            }
        } else {
            (_namespace as MutableMap)[value.qualifiedName] = value
            value.options.parent = this.options //FIXME: could case wrong parent if namespace in multiple Domains!
        }
    }

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val ns = namespace.joinToString(separator = "\n") { "$indent${it.asString(indent)}" }
        sb.append(ns)
        return sb.toString()
    }

    // --- Any ---
    override fun hashCode(): Int = listOf(name, namespace.hashCode()).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is Domain<*, *> -> false
        this.name != other.name -> false
        this.namespace != other.namespace -> false
        else -> true
    }

    override fun toString(): String = "Domain '$name'"
}

abstract class NamespaceAbstract<DT : Definition<DT>>(
    override val options: OptionHolder,
    argImport: List<Import>
) : Namespace<DT> {

    override val definition: List<DT> get() = _definition.values.toList()

    override val definitionByName: Map<SimpleName, DT> get() = _definition

    protected val _importedNamespaces = mutableMapOf<QualifiedName, Namespace<DT>?>()
    protected val _definition = linkedMapOf<SimpleName, DT>() //order is important

    override val import: List<Import> = argImport.toMutableList()

    init {
        connectionDefinitionOptionHolderParentsToThis()
    }

    override fun resolveImports(domain: Domain<Namespace<DT>, DT>) {
        // check explicit imports
        this.import.forEach {
            val ns = domain.findNamespaceOrNull(it.asQualifiedName)
                ?: error("import '$it' cannot be resolved in the namespace '${qualifiedName.value}' of TypesDomain '${domain.name.value}'")
            _importedNamespaces[it.asQualifiedName] = ns
        }
    }

    override fun findDefinitionOrNull(simpleName: SimpleName): DT? =
        findOwnedDefinitionOrNull(simpleName)
            ?: import.firstNotNullOfOrNull {
                val tns = _importedNamespaces[it.asQualifiedName]
                //    ?: error("namespace '$it' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                tns?.findOwnedDefinitionOrNull(simpleName)
            }

    override fun findOwnedDefinitionOrNull(simpleName: SimpleName): DT? = _definition[simpleName]

    fun connectionDefinitionOptionHolderParentsToThis() {
        this.definition.forEach { it.options.parent = this.options }
    }

    override fun addImport(value: Import) {
        if (this.import.contains(value)) {
            // do not repeat imports
        } else {
            (this.import as MutableList).add(value)
        }
    }

    override fun addDefinition(value: DT) {
        check(_definition.containsKey(value.name).not()) { "namespace '$qualifiedName' already contains a declaration named '${value.name}', cannot add another" }
        _definition[value.name] = value
        value.options.parent = this.options
    }

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("namespace $qualifiedName\n")
        val newIndent = indent.inc
        if (import.isNotEmpty()) {
            val importStr = import.joinToString(separator = "\n") { "${newIndent}import ${it.value}" }
            sb.append(importStr)
            sb.append("\n")
        }
        val defs = definition
            .sortedBy { it.name.value }
            .joinToString(separator = "\n") { "$newIndent${it.asString(newIndent)}" }
        sb.append(defs)
        return sb.toString()
    }

    // -- Any ---
    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is Namespace<*> -> false
        else -> qualifiedName == other.qualifiedName
    }

    override fun toString(): String = "namespace $qualifiedName"

    // --- Implementation ---

    fun addAllDefinition(value: Iterable<DT>) {
        value.forEach { addDefinition(it) }
    }
}

abstract class DefinitionAbstract<DT : Definition<DT>> : Definition<DT> {
    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(this.name)

    override fun asString(indent: Indent): String {
        TODO("not implemented")
    }
}

class DomainDefault(
    override val name: SimpleName,
    options: OptionHolder,
    namespace: List<NamespaceDefault> = emptyList()
) : DomainAbstract<NamespaceDefault, DefinitionDefault>(namespace, options) {

}

class NamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder,
    import: List<Import>
) : NamespaceAbstract<DefinitionDefault>(options, import) {

}

class DefinitionDefault(
    override val namespace: Namespace<DefinitionDefault>,
    override val name: SimpleName,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : DefinitionAbstract<DefinitionDefault>() {

}