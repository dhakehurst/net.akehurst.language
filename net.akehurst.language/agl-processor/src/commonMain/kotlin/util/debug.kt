/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.util

internal fun debug(indentDelta: Debug.IndentDelta, lazyMessage:()->String) = Debug.debug(indentDelta, lazyMessage)

internal object Debug {
    enum class IndentDelta{NONE, INC_BEFORE, INC_AFTER, DEC_BEFORE, DEC_AFTER}

    const val CHECK = false
    const val OUTPUT_SM_BUILD = false
    const val OUTPUT_RUNTIME_BUILD = false
    const val OUTPUT_RUNTIME = false
    const val OUTPUT_TREE_DATA = false

    private const val indentDeltaStr = "  "
    private var currentIndent = ""

    fun debug(indentDelta:IndentDelta,lazyMessage:()->String) {
        if (OUTPUT_SM_BUILD || OUTPUT_RUNTIME || OUTPUT_RUNTIME_BUILD || OUTPUT_TREE_DATA) {
            when(indentDelta){
                IndentDelta.DEC_BEFORE -> currentIndent = currentIndent.substring(indentDeltaStr.length)
                IndentDelta.INC_BEFORE -> currentIndent += indentDeltaStr
                else -> Unit
            }
            println("${currentIndent}${lazyMessage.invoke()}")
            when(indentDelta){
                IndentDelta.DEC_AFTER -> currentIndent = currentIndent.substring(indentDeltaStr.length)
                IndentDelta.INC_AFTER -> currentIndent += indentDeltaStr
                else -> Unit
            }
        }
    }
}
