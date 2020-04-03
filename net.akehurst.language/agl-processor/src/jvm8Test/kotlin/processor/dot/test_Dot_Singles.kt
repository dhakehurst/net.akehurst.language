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
package net.akehurst.language.agl.processor.dot

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
//import java.io.BufferedReader
//import java.io.InputStreamReader
import java.util.ArrayList

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.processor.Agl
import java.io.BufferedReader
import java.io.InputStreamReader


class test_Dot_Singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/dot/Dot.agl").readText()

        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }
        var processor: LanguageProcessor = Agl.processor(grammarStr)

    }

    @Test
    fun a_list__from_Data_Structures() {
        val goal = "a_list"
        val sentence =  """
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        """
        processor.parse(goal, sentence)
    }

    @Test
    fun attr_list__from_Data_Structures() {
        val goal = "attr_list"
        val sentence =  """[
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        ]"""
        processor.parse(goal, sentence)
    }
    @Test
    fun attr_stmt__from_Data_Structures() {
        val goal = "attr_stmt"
        val sentence =  """
            edge [ ]
        """.trimIndent()
        processor.parse(goal, sentence)
    }
    @Test
    fun stmt_list__from_Data_Structures() {
        val goal = "stmt_list"
        val sentence =  """
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
        """.trimIndent()
        processor.parse(goal, sentence)
    }
    @Test
    fun graph__from_Data_Structures() {
        val goal = "graph"
        val sentence = """
            digraph g {
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
            "node0" [
            label = "<f0> 0x10ba8| <f1>"
            shape = "record"
            ];
            }
        """.trimIndent()
        processor.parse(goal, sentence)

    }

    @Test
    fun stmt_list__1() {
        val goal = "stmt_list"
        val sentence =  "graph[a=a ]; node [b=b c=c]; edge[];"
        processor.parse(goal, sentence)
    }

    @Test
    fun attr_list__2s() {
        val goal = "attr_list"
        val sentence = "[x = x; y=y]"
        processor.parse(goal, sentence)

    }

    @Test
    fun attr_list__2n() {
        val goal = "attr_list"
        val sentence = "[x = x y=y]"
        processor.parse(goal, sentence)

    }
}
