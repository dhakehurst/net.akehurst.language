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

package net.akehurst.language.api.language.base

import net.akehurst.language.api.language.base.QualifiedName.Companion.isQualifiedName
import net.akehurst.language.api.language.base.SimpleName.Companion.asSimpleName
import kotlin.jvm.JvmInline

interface PossiblyQualifiedName {
    companion object {
        val String.asPossiblyQualifiedName
            get() = when {
                this.isQualifiedName -> QualifiedName(this)
                else -> SimpleName(this)
            }
    }

    val simpleName: SimpleName
}

/**
 * A qualified name, separator assumed to be '.'
 */
@JvmInline
value class QualifiedName(val value: String) : PossiblyQualifiedName {
    companion object {
        val String.isQualifiedName: Boolean get() = this.contains(".")
        val String.asQualifiedName: QualifiedName get() = QualifiedName(this)
    }

    constructor(namespace: QualifiedName, name: SimpleName) : this("${namespace.value}.$name")

    val parts: List<SimpleName> get() = value.split(".").map { it.asSimpleName }
    val last: SimpleName get() = parts.last()
    val front: QualifiedName get() = QualifiedName(parts.dropLast(1).joinToString(separator = "."))

    fun append(lastPart: SimpleName) = QualifiedName(this, lastPart)

    override val simpleName: SimpleName get() = last
}

@JvmInline
value class SimpleName(val value: String) : PossiblyQualifiedName {
    companion object {
        val String.isSimpleName: Boolean get() = this.contains(".").not()
        val String.asSimpleName: SimpleName get() = SimpleName(this)
    }

    override val simpleName: SimpleName get() = this
}

/**
 * A qualified name of something to import.
 *
 * Separator assumed to be '.'
 */
@JvmInline
value class Import(val value: String) {
    val asQualifiedName: QualifiedName get() = QualifiedName(value)
}

@JvmInline
value class Indent(val value: String) {
    fun inc(increment: String) = Indent(this.value + increment)
}

interface Formatable {
    fun asString(indent: Indent = Indent(""), increment: String = "  "): String
}

/**
 * A parsable block of text - typically a file
 */
interface DefinitionBlock<DT : Definition<DT>> : Formatable {
    /**
     * Ordered Map
     */
    val namespace: List<Namespace<DT>>

    val allDefinitions: List<DT>

    val isEmpty: Boolean

    fun findNamespaceOrNull(qualifiedName: QualifiedName): Namespace<DT>?
}

interface Namespace<DT : Definition<DT>> : Formatable {
    val qualifiedName: QualifiedName

    /**
     * Things in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val import: List<Import>

    val definition: List<DT>
}

interface Definition<DT : Definition<DT>> : Formatable {
    val namespace: Namespace<DT>
    val name: SimpleName
    val qualifiedName: QualifiedName
}