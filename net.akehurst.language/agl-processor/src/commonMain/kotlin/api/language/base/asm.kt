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

package net.akehurst.language.agl.api.language.base

import kotlin.jvm.JvmInline

/**
 * A qualified name, separator assumed to be '.'
 */
@JvmInline
value class QualifiedName(val value: String)

/**
 * A qualified name of something to import.
 *
 * Separator assumed to be '.'
 */
@JvmInline
value class Import(val value: String)

@JvmInline
value class Indent(val value: String) {
    fun inc(increment: String) = Indent(this.value + increment)
}

interface Formatable {
    fun asString(indent: Indent, increment: String): String
}

/**
 * A parsable block of text - typically a file
 */
interface DefinitionBlock<DT : Definition<DT>> : Formatable {
    val namespace: List<Namespace<DT>>
}

interface Namespace<DT : Definition<DT>> : Formatable {
    val qualifiedName: QualifiedName
    val import: List<Import>
    val definition: List<DT>
}

interface Definition<DT : Definition<DT>> : Formatable {
    val namespace: Namespace<DT>
    val name: String
    val qualifiedName: QualifiedName
}