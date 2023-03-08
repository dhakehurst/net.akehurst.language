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
package net.akehurst.language.agl.processor.xml

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
//import java.io.BufferedReader
//import java.io.InputStreamReader

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_Xml_Singles {

    private companion object {

        val grammarStr = this::class.java.getResource("/xml/Xml.agl").readText()
        const val goal = "document"

        var processor = Agl.processorFromStringDefault(grammarStr).processor!!
    }

    @Test
    fun parse_comment_prolog_root() {
        val sentence = """
            <!-- comment -->
            <?xml version="1.0" encoding="UTF-8" ?>
            <root ></root>
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
            assertNull(result.sppt)
            assertTrue(result.issues.isNotEmpty())
    }

}
