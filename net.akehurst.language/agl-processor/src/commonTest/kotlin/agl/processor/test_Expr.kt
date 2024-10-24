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

package agl.processor

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.test_ProcessorAbstract
import kotlin.test.Test

class test_Expr : test_ProcessorAbstract() {

    companion object {
        val grammarStr = """
namespace test.d
grammar Test {
    skip WS = "\s+" ;
    expr
      = value
      | var
      ;

    var = NAME;
    value = INT ;

    leaf INT = "[0-9]+";
    leaf NAME = "[a-z]+";
}
        """.trimIndent()

        val processor = Agl.processorFromString<Any,Any>(grammarStr).processor!!
    }

    @Test
    fun f() {
        val text = "a"

        val expected = """
             expr|1 { var { NAME : 'a' } }
        """.trimIndent()

        super.test(processor,"expr", text, expected)
    }

}