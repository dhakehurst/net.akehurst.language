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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.withTypeCache
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/** type lookups are cached only during a run (beginTypeCache .. endTypeCache) */
class test_ObjectGraphByReflection_typeCache {

    private class AM(td: net.akehurst.language.types.api.TypesDomain) : ObjectGraphAccessorMutatorByReflection(td, IssueHolder(), LocationMapDefault()) {
        fun props(t: TypeInstance) = allResolvedPropertyOf(t)
        fun def(t: TypeInstance) = resolvedDefinitionOrNullOf(t)
        fun typeOf(qn: String) = typeOfQualifiedNameOrNull(qn.asQualifiedName)
    }

    private val domain = typesDomain("test", true) {
        namespace("ns") {
            data("A") { propertyOf(setOf(VAL), "name", "String") }
        }
    }

    // a new TypeInstance each time, as e.g. typeFor gives
    private fun typeA() = domain.findByQualifiedNameOrNull("ns.A".asQualifiedName)!!.type()

    @Test
    fun not_cached_outside_a_run() {
        val am = AM(domain)
        assertNotSame(am.props(typeA()), am.props(typeA()))
    }

    @Test
    fun cached_in_a_run_also_for_new_TypeInstances() {
        val am = AM(domain)
        am.withTypeCache {
            assertSame(am.props(typeA()), am.props(typeA()))
        }
    }

    @Test
    fun nested_runs_share_the_cache_and_it_is_dropped_at_the_end_of_the_outer_run() {
        val am = AM(domain)
        am.withTypeCache {
            val p1 = am.props(typeA())
            am.withTypeCache { assertSame(p1, am.props(typeA())) }
            assertSame(p1, am.props(typeA()))
        }
        assertNotSame(am.props(typeA()), am.props(typeA()))
    }

    @Test
    fun changing_typesDomain_drops_the_cache() {
        val am = AM(domain)
        am.withTypeCache {
            val p1 = am.props(typeA())
            am.typesDomain = domain
            assertNotSame(p1, am.props(typeA()))
        }
    }

    @Test
    fun resolvedDefinition_in_a_run_is_as_without_cache() {
        val am = AM(domain)
        val ns = domain.findNamespaceOrNull("ns".asQualifiedName)!!
        val unresolvable = ns.createTypeInstance(null, net.akehurst.language.base.api.SimpleName("NotDefined"), emptyList(), false)
        val expected = typeA().resolvedDefinitionOrNull
        am.withTypeCache {
            assertSame(expected, am.def(typeA()))
            assertSame(expected, am.def(typeA()))
            assertNull(am.def(unresolvable))
            assertNull(am.def(unresolvable))
        }
        assertSame(expected, am.def(typeA()))
    }

    @Test
    fun type_by_qualified_name_is_one_instance_in_a_run_and_not_found_is_null() {
        val am = AM(domain)
        assertNotSame(am.typeOf("ns.A"), am.typeOf("ns.A"))
        am.withTypeCache {
            assertSame(am.typeOf("ns.A"), am.typeOf("ns.A"))
            assertNull(am.typeOf("ns.Unknown"))
            assertNull(am.typeOf("ns.Unknown"))
        }
    }
}
