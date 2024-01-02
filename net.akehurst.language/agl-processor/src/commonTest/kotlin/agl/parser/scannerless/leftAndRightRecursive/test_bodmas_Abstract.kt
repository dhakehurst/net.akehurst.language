/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.leftAndRightRecursive

import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract

internal abstract class test_bodmas_Abstract : test_LeftCornerParserAbstract() {

    abstract fun empty_fails()
    abstract fun a()
    abstract fun a_add_b()
    abstract fun a_mul_b()
    abstract fun a_add_b_add_c()
    abstract fun a_mul_b_mul_c()
    abstract fun a_add_b_mul_c()
    abstract fun a_mul_b_add_c()

}