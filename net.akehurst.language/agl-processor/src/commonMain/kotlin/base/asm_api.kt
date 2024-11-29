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

package net.akehurst.language.base.api

import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformRuleSetReference
import kotlin.jvm.JvmInline

//FixME: wanted these in the companion object below, but is a kotlin bug
// [https://youtrack.jetbrains.com/issue/IDEA-359261/value-class-extension-methods-not-working-when-defined-in-companion-object]
val String.asPossiblyQualifiedName: PossiblyQualifiedName
    get() = when {
        this.isQualifiedName -> QualifiedName(this)
        else -> SimpleName(this)
    }
val String.isQualifiedName: Boolean get() = this.contains(".")
val String.asQualifiedName: QualifiedName get() = QualifiedName(this)

interface PossiblyQualifiedName {
    companion object {
        //FIXME: from here - see above
    }

    val value: String
    val simpleName: SimpleName

    /**
     * if this is a SimpleName, append it to the given qualifiedName (or just the simple name if namespace is null),
     * if already a QualifiedName return it with no modification
     **/
    fun asQualifiedName(namespace: QualifiedName?): QualifiedName
}

/**
 * A qualified name, separator assumed to be '.'
 */
@JvmInline
value class QualifiedName(override val value: String) : PossiblyQualifiedName, PublicValueType {

    constructor(namespace: QualifiedName, name: SimpleName) : this("${namespace.value}.$name")

    val isQualified: Boolean get() = value.isQualifiedName

    val parts: List<SimpleName> get() = value.split(".").map { it.asSimpleName }
    val last: SimpleName get() = parts.last()
    val front: QualifiedName get() = QualifiedName(parts.dropLast(1).joinToString(separator = "."))
    val asPossiblyQualified: PossiblyQualifiedName get() = this.value.asPossiblyQualifiedName

    fun append(lastPart: SimpleName) = QualifiedName(this, lastPart)

    override val simpleName: SimpleName get() = last
    val asImport: Import get() = Import(this.value)

    override fun asQualifiedName(namespace: QualifiedName?) = this

    override fun toString(): String = value
}

//FixME: wanted these in the companion object below, but is a kotlin bug
// [https://youtrack.jetbrains.com/issue/IDEA-359261/value-class-extension-methods-not-working-when-defined-in-companion-object]
val String.isSimpleName: Boolean get() = this.contains(".").not()
val String.asSimpleName: SimpleName get() = SimpleName(this)

@JvmInline
value class SimpleName(override val value: String) : PossiblyQualifiedName, PublicValueType {
    companion object {
        //FIXME: from here - see above
    }

    override val simpleName: SimpleName get() = this
    override fun asQualifiedName(namespace: QualifiedName?) = when (namespace) {
        null -> QualifiedName(this.value)
        else -> namespace.append(this)
    }

    override fun toString(): String = value
}

// This is added so that any value class will correctly
// make its 'value' public in JS
interface PublicValueType {
    val value: Any
}

/**
 * A qualified name of something to import.
 *
 * Separator assumed to be '.'
 */
@JvmInline
value class Import(override val value: String) : PublicValueType {
    val asQualifiedName: QualifiedName get() = QualifiedName(value)
    override fun toString() = value
}

class Indent(val value: String = "", val increment: String = "  ") {
    val inc get() = Indent(this.value + increment, increment)
    override fun toString() = value
}

interface Formatable {
    fun asString(indent: Indent = Indent("", "  ")): String
}

interface OptionHolder {
    var parent: OptionHolder?

    operator fun get(name: String): String?
    fun clone(parent:OptionHolder?): OptionHolder
}

/**
 * A group of related namespaces in which the definitions may reference each other
 */
//TODO: consider using alternative words for this interface - Domain, System, Unit, Module !
interface Model<NT : Namespace<DT>, DT : Definition<DT>> : Formatable {
    val name: SimpleName

    val options: OptionHolder

    val namespace: List<NT>

    val allDefinitions: List<DT>

    val isEmpty: Boolean

    fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>?

    fun findDefinitionOrNullByQualifiedName(qualifiedName: QualifiedName): DT?

    fun resolveReference(reference:DefinitionReference<DT>): DT?

    fun addNamespace(value: NT)
}

interface Namespace<DT : Definition<DT>> : Formatable {
    val qualifiedName: QualifiedName

    val options: OptionHolder

    /**
     * Things in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val import: List<Import>

    val definition: List<DT>

    val definitionByName: Map<SimpleName, DT>

    fun resolveImports(model: Model<Namespace<DT>, DT>)

    /** find owned or imported definition **/
    fun findDefinitionOrNull(simpleName: SimpleName): DT?

    /** find owned definition (not imported) **/
    fun findOwnedDefinitionOrNull(simpleName: SimpleName): DT?

    fun addImport(value: Import)
    fun addDefinition(value: DT)
}

interface DefinitionReference<DT : Definition<DT>> {
    val localNamespace: Namespace<DT>
    val nameOrQName: PossiblyQualifiedName
    var resolved: TransformRuleSet?

    fun resolveAs(resolved: DT)

    //fun cloneTo(ns: Namespace<DT>): DefinitionReference<DT>
}

interface Definition<DT : Definition<DT>> : Formatable {
    val namespace: Namespace<DT>
    val name: SimpleName
    val qualifiedName: QualifiedName

    val options: OptionHolder
}