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

package net.akehurst.language.sentence.api

interface Sentence {
    val text: String

    fun textAt(position: Int, length: Int): String

    /**
     * position in text of the beginning of the requested line, first line is 0
     *
     * in result InputLocation, first line is 1
     */
    fun positionOfLine(line: Int): Int

    /**
     * location (position, line, column, length) in given line of the given position and length, first line is 0
     *
     * in result InputLocation, first line is 1
     */
    fun locationInLine(line: Int, position: Int, length: Int): InputLocation

    /**
     * location (position, line, column, length) of the given position and length
     */
    fun locationFor(position: Int, length: Int): InputLocation

    fun contextInText(position: Int): String

    //fun setEolPositions(eols: List<Int>)
}

data class InputLocation(
    val position: Int,
    val column: Int,
    val line: Int,
    var length: Int
) {

    val endPosition get() = position + length
}