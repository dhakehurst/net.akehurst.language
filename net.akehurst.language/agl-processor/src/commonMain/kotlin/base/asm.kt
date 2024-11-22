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
import net.akehurst.language.reference.api.CrossReferenceNamespace
import net.akehurst.language.transform.api.TransformNamespace

class OptionHolderDefault(
    override var parent: OptionHolder?,
    val options: Map<String,String>
) :OptionHolder {
    override operator fun get(name:String):String? {
        return this.options[name] ?: this.parent?.get(name)
    }
}

abstract class ModelAbstract<NT : Namespace<DT>, DT : Definition<DT>>(
    namespace: List<NT>,
    override val options: OptionHolder
) : Model<NT, DT> {

    override val allDefinitions: List<DT> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = allDefinitions.isEmpty()

    override val namespace: List<NT>  get() = _namespace.values.toList()

    private val _namespace: Map<QualifiedName, NT> = mutableMapOf<QualifiedName, NT>().also { map ->
        namespace.forEach { map[it.qualifiedName] = it }
    }

    init {
        connectionNamespaceOptionHolderParentsToThis()
    }

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>? = _namespace[qualifiedName]

    fun connectionNamespaceOptionHolderParentsToThis() {
        this.namespace.forEach { it.options.parent = this.options }
    }

    fun addNamespace(value:NT) {
        if (_namespace.containsKey(value.qualifiedName)) {
            if (_namespace[value.qualifiedName] === value) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${value.qualifiedName}'")
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
        other !is Model<*, *> -> false
        this.name != other.name -> false
        this.namespace != other.namespace -> false
        else -> true
    }
    override fun toString(): String = "Domain '$name'"
}

abstract class NamespaceAbstract<DT : Definition<DT>>(
    override val options: OptionHolder,
    override val import: List<Import>
) : Namespace<DT> {

    override val definition: List<DT> get() = _definition.values.toList()

    override val definitionByName: Map<SimpleName, DT> get() = _definition

    protected val _importedNamespaces = mutableMapOf<QualifiedName, Namespace<DT>?>()
    protected val _definition = linkedMapOf<SimpleName, DT>() //order is important

    init {
        connectionDefinitionOptionHolderParentsToThis()
    }

    override fun resolveImports(model: Model<Namespace<DT>, DT>) {
        // check explicit imports
        this.import.forEach {
            val ns = model.findNamespaceOrNull(it.asQualifiedName)
                ?: error("import '$it' cannot be resolved in the TypeModel '${model.name}'")
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

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("namespace $qualifiedName\n")
        val newIndent = indent.inc
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

    fun addDefinition(value: DT) {
        _definition[value.name] = value
        value.options.parent = this.options
    }

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

class ModelDefault(
    override val name: SimpleName,
    options: OptionHolder,
    namespace: List<NamespaceDefault> =  emptyList()
) : ModelAbstract<NamespaceDefault, DefinitionDefault>(namespace,options) {

}

class NamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder,
    import: List<Import>
) : NamespaceAbstract<DefinitionDefault>(options,import) {

}

class DefinitionDefault(
    override val namespace: Namespace<DefinitionDefault>,
    override val name: SimpleName,
    override val options: OptionHolder = OptionHolderDefault(null,emptyMap()),
) : DefinitionAbstract<DefinitionDefault>() {

}