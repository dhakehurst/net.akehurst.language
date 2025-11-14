/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_FormatterOverTypedObject {

    data class TestData(val name:String, val template: FormatString, val types: TypesDomain, val self:Any, val expected:String)

    data class TestObject(
        val intValue:Int = 3,
        val strValue:String = "Hello World!",
        val listValue:List<String> = listOf("a","b","c")
    )

    private companion object {
        val testData = listOf<TestData>(
            TestData(
                name = "Output a String from Raw Text",
                template = FormatString("""
                    namespace test
                    format Test {
                      Any -> "Hello World!"
                    }
                    """),
                types = typesDomain("Test", true) { },
                self = 1,
                expected = "Hello World!"
            ),
            TestData(
                name = "Output a String from self Object, implicit template",
                template = FormatString("""
                    namespace test
                    format Test {
                    }
                    """),
                types = typesDomain("Test", true) { },
                self = "Hello World!",
                expected = "Hello World!"
            ),
            TestData(
                name = "Output a String from self Object, explicit template",
                template = FormatString("""
                    namespace test
                    format Test {
                      TestObject -> "Hello World!"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = "Hello World!"
            ),
            TestData(
                name = "Output an Integer value from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "$intValue"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = "3"
            ),
            TestData(
                name = "Output a String value from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "$strValue"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = "Hello World!"
            ),
            TestData(
                name = "Output a List value from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "$listValue"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = "abc"
            ),
            TestData(
                name = $$"Output a String with $EOL from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "Hello$EOL World!"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = """
                    Hello
                     World!
                """.trimIndent()
            ),
            TestData(
                name = $$"Output a String with ${$EOL} from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "Hello${$EOL}World!"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = """
                    Hello
                    World!
                """.trimIndent()
            ),
            TestData(
                name = $$"Output a List value separated with ', ' from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "list: $[listValue sep ', ']"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(),
                expected = "list: a, b, c"
            ),
            TestData(
                name = $$"Output a list separated with $EOL from self Object, explicit template",
                template = FormatString($$"""
                    namespace test
                    format Test {
                      TestObject -> "list:$EOL$[listValue sep $EOL]"
                    }
                    """),
                types = typesDomain("Test", true) {
                    namespace("test") {
                        data("TestObject")
                    }
                },
                self = TestObject(1,""),
                expected = """
                   list:
                   a
                   b
                   c
                """.trimIndent()
            )
        )

        fun test(data:TestData) {
            val res = Agl.formatByReflection(data.template, data.types, data.self)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val actual = res.sentence
            assertEquals(data.expected, actual)
        }
    }

    @Test
    fun all() {
        testData.forEachIndexed { idx, td ->
            println("--- $idx: ${td.name} ---")
            test(td)
        }
    }

    @Test
    fun test1() {
        test(testData[5])
    }

}