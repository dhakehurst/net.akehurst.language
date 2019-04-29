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

package net.akehurst.language.processor.java8

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import kotlin.test.Test

class test_Java8_parts {

    companion object {

        var java8Processor: LanguageProcessor = createJava8Processor()

        fun createJava8Processor(): LanguageProcessor {
            val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
            val proc = Agl.processor(grammarStr)
            proc.build()
            return proc
        }
    }

    @Test(timeout=1000)
    fun long_concatenation() {

        val sentence = """
          {
            concat =  "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" ;
          }
        """.trimIndent()
        val goal = "block"

        val t = java8Processor.parse(goal, sentence)

       // println( t.toStringAll("  ") )
    }

}