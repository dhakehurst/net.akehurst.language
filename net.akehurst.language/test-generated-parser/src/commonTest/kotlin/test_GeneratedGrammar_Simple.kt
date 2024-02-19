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

import net.akehurst.language.agl.Agl
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_GeneratedGrammar_Simple {

    @Test
    fun build_for_generated_should_fail() {
        val sentence = "a"

        val p = GeneratedGrammar_Simple.processor.buildFor(Agl.parseOptions {
            goalRuleName("S")
        })

    }

    @Test
    fun parse() {
        val sentence = "a"
        val result = GeneratedGrammar_Simple.parse(sentence)

        assertNotNull(result.sppt)
    }

}