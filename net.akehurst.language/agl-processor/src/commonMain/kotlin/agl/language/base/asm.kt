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


class DefinitionBlockDefault<DT : Definition<DT>>(
    namespace: List<Namespace<DT>>
) : DefinitionBlockAbstract<DT>(namespace) {

}

class NamespaceDefault<DT : Definition<DT>>(
    qualifiedName: QualifiedName
) : NamespaceAbstract<DT>(qualifiedName) {

}

abstract class DefinitionBlockAbstract<DT : Definition<DT>>(
    override val namespace: List<Namespace<DT>>
) : DefinitionBlock<DT> {

    override val allDefinitions: List<DT> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = allDefinitions.isEmpty()

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>? {
        return namespace.firstOrNull { it.qualifiedName == qualifiedName }
    }

    // --- Formatable ---
    override fun asString(indent: Indent, increment: String): String {
        val sb = StringBuilder()
        val ns = namespace.joinToString(separator = "\n") { "$indent${it.asString(indent, increment)}" }
        sb.append(ns)
        return sb.toString()
    }
}

abstract class NamespaceAbstract<DT : Definition<DT>>(
    override val qualifiedName: QualifiedName
) : Namespace<DT> {

    override val import: List<Import> = mutableListOf()

    override val definition: List<DT> = mutableListOf()

    // --- Formatable ---
    override fun asString(indent: Indent, increment: String): String {
        val sb = StringBuilder()
        sb.append("namespace $qualifiedName\n")
        val newIndent = indent.inc(increment)
        sb.append("\n")
        val importStr = import.joinToString(separator = "\n") { "$newIndent${it.value}" }
        sb.append(importStr)
        sb.append("\n")
        val defs = definition
            .sortedBy { it.name.value }
            .joinToString(separator = "\n") { "$newIndent${it.asString(newIndent, increment)}" }
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
    fun addDefinition(value: DT) {
        (this.definition as MutableList).add(value)
    }
}

