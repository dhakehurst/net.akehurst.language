/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.test.Test

class test_SyntaxAnalyserAbstract {

    @Test
    fun t() {
        TODO()
    }
}

class TestSyntaxAnalyser : SyntaxAnalyserAbstract() {

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return 1 as T
    }

}

data class A(
    val value: String
) {

}