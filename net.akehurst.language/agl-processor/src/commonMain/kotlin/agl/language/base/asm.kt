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

package net.akehurst.language.agl.language.base

import net.akehurst.language.api.language.base.*


class DefinitionBlockDefault<NT : Namespace<DT>, DT : Definition<DT>>(
    override val name: SimpleName,
    namespace: List<NT>
) : DefinitionBlockAbstract<NT, DT>(namespace) {

}

class NamespaceDefault<DT : Definition<DT>>(
    qualifiedName: QualifiedName
) : NamespaceAbstract<DT>(qualifiedName) {

}

abstract class DefinitionBlockAbstract<NT : Namespace<DT>, DT : Definition<DT>>(
    override val namespace: List<NT>
) : Model<NT, DT> {

    override val allDefinitions: List<DT> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = allDefinitions.isEmpty()

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>? {
        return namespace.firstOrNull { it.qualifiedName == qualifiedName }
    }

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val ns = namespace.joinToString(separator = "\n") { "$indent${it.asString(indent)}" }
        sb.append(ns)
        return sb.toString()
    }
}

abstract class NamespaceAbstract<DT : Definition<DT>>(
    override val qualifiedName: QualifiedName
) : Namespace<DT> {

    override val import: List<Import> = mutableListOf()

    override val definition: List<DT> get() = _definition.values.toList()

    override val definitionByName: Map<SimpleName, DT> get() = _definition

    override fun resolveImports(model: Model<Namespace<DT>, DT>) {
        // check explicit imports
        this.import.forEach {
            val ns = model.findNamespaceOrNull(it.asQualifiedName) ?: error("import '$it' cannot be resolved in the TypeModel '${model.name}'")
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

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("namespace $qualifiedName\n")
        val newIndent = indent.inc
        sb.append("\n")
        if (import.isNotEmpty()) {
            val importStr = import.joinToString(separator = "\n") { "$newIndent${it.value}" }
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
    private val _importedNamespaces = mutableMapOf<QualifiedName, Namespace<DT>?>()
    private val _definition = mutableMapOf<SimpleName, DT>()

    fun addDefinition(value: DT) {
        _definition[value.name] = value
    }

    fun addAllDefinition(value: Iterable<DT>) {
        value.forEach {
            _definition[it.name] = it
        }
    }
}

