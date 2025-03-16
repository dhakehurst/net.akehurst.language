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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import testFixture.data.ambiguity.OptionalCoveredByPrecedingList
import kotlin.test.Test

class test_optional_covered_by_list_nonTerm_multi_not_at_start_of_rule_2 : test_LeftCornerParserAbstract() {

    // S = 'b' as 'a'? ; vs = v+ ; v = [a-z]
    private companion object {
        val td = OptionalCoveredByPrecedingList.data[1]
    }

    @Test
    fun empty__fails() {
        test(td,"")
    }

    @Test
    fun b__fails() {
        test(td,"b")
    }

    @Test
    fun ba() {
        test(td,"ba")
    }

    @Test
    fun bv() {
        test(td,"bv")
    }

    @Test
    fun baa() {
        test(td,"baa")
    }

    @Test
    fun bav() {
        test(td,"bav")
    }

    @Test
    fun baav() {
        test(td,"baav")
    }

    @Test
    fun baaa() {
        test(td,"baaa")
    }

    @Test
    fun baaaa() {
        test(td,"baaaa")
    }

}