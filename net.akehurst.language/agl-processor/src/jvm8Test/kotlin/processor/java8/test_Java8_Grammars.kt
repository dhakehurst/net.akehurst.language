/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor.java8

import net.akehurst.language.agl.processor.Agl
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull

class test_Java8_Grammars {

    @Test(timeout=5000)
    fun aglSpec() {
        val grammarStr = this::class.java.getResource("/java8/Java8AglSpec.agl").readText()
        val actual = Agl.processorFromString(grammarStr)
        assertNotNull(actual)
    }

    @Test(timeout=5000)
    fun aglOptm() {
        val grammarStr = this::class.java.getResource("/java8/Java8AglOptm.agl").readText()
        //val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8OptmAgl.agl")
        //val bytes = Files.readAllBytes(grammarFile)
        //val grammarStr = String(bytes)
        val actual = Agl.processorFromString(grammarStr)
        assertNotNull(actual)

        val res = Agl.grammarProcessor.analyseText(List::class, grammarStr)
        assertNotNull(actual)
        res.forEach {
            println(it)
        }
    }

    @Test(timeout=5000)
    fun antrlSpec() {
        //val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
        val grammarStr = this::class.java.getResource("/java8/Java8AntlrSpec.agl").readText()
        val actual = Agl.processorFromString(grammarStr)
        assertNotNull(actual)
    }



    @Test(timeout=5000)
    fun antlrOptm() {
        val grammarStr = this::class.java.getResource("/java8/Java8AntlrOptm.agl").readText()
        //val grammarFile = Paths.get("src/jvm8Test/resources/java8/Java8OptmAntlr.agl")
        //val bytes = Files.readAllBytes(grammarFile)
        //val grammarStr = String(bytes)
        val actual = Agl.processorFromString(grammarStr)
        assertNotNull(actual)
    }

}