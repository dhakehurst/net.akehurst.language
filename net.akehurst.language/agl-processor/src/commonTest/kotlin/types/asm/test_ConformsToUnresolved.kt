/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.types.asm

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * conformsTo must not throw when a types definition is incomplete (e.g. a types file with a parse error),
 * an unresolvable type conforms to nothing (other than Any)
 */
class test_ConformsToUnresolved {

    private val domain = typesDomain("Test", true) {
        namespace("test") {
            data("A")
            data("B") { supertype("Missing") }
            data("C") { supertype("A") }
            union("U") {
                typeRef("A")
                typeRef("Missing")
            }
            union("V") {
                typeRef("Missing")
            }
        }
    }

    private fun def(name: String) = domain.findByQualifiedNameOrNull(QualifiedName("test.$name"))!!

    @Test
    fun unresolvable_supertype() {
        assertFalse(def("B").conformsTo(def("A")))
        assertTrue(def("C").conformsTo(def("A")))
    }

    @Test
    fun union_with_unresolvable_alternative() {
        assertTrue(def("A").conformsTo(def("U")))
        assertFalse(def("B").conformsTo(def("U")))
        assertFalse(def("U").conformsTo(def("A")))
        assertFalse(def("V").conformsTo(def("A")))
    }

    @Test
    fun all_pairs_do_not_throw() {
        domain.allDefinitions.forEach { a -> domain.allDefinitions.forEach { b -> a.conformsTo(b) } }
    }
}
