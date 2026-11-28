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

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.style.api.AglStyleSelectorKind
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.types.api.EnumType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * AglStyle.typesDomain describes the kotlin classes of the style DSL (style_asm_api.kt, style_asm.kt);
 * the expected properties here are those of the kotlin interfaces and classes (SD5)
 */
class test_AglStyle_typesDomain {

    private fun ownProperties(qualifiedName: String): List<String> {
        val td = AglStyle.typesDomain.findByQualifiedNameOrNull(QualifiedName(qualifiedName))
        assertNotNull(td, "type '$qualifiedName' not found")
        return td.property.map { it.name.value }
    }

    @Test
    fun selectorKind_literals_match_kotlin_enum() {
        val td = AglStyle.typesDomain.findByQualifiedNameOrNull(QualifiedName("net.akehurst.language.style.api.AglStyleSelectorKind")) as EnumType
        assertEquals(AglStyleSelectorKind.entries.map { it.name }, td.literals)
    }

    @Test
    fun api_types() {
        val api = "net.akehurst.language.style.api"
        assertEquals(listOf("styleSet"), ownProperties("$api.StyleNamespace"))
        assertEquals(listOf("localNamespace", "nameOrQName", "resolved"), ownProperties("$api.StyleSetReference"))
        assertEquals(listOf("namespace", "extends", "importTypes", "rules", "metaRules", "tagRules", "allRules"), ownProperties("$api.StyleSet"))
        assertEquals(listOf("declaration"), ownProperties("$api.AglStyleRule"))
        assertEquals(listOf("pattern"), ownProperties("$api.AglStyleMetaRule"))
        assertEquals(listOf("selector"), ownProperties("$api.AglStyleTagRule"))
        assertEquals(listOf("value", "kind", "resolvedTypeName", "matchTag"), ownProperties("$api.AglStyleSelector"))
        assertEquals(listOf("name", "value"), ownProperties("$api.AglStyleDeclaration"))
    }

    @Test
    fun asm_types() {
        val asm = "net.akehurst.language.style.asm"
        assertEquals(listOf("localNamespace", "nameOrQName", "resolved"), ownProperties("$asm.StyleSetReferenceDefault"))
        assertEquals(listOf("qualifiedName", "import"), ownProperties("$asm.StyleNamespaceDefault"))
        assertEquals(listOf("namespace", "name", "extends", "importTypes", "rules"), ownProperties("$asm.AglStyleSetDefault"))
        assertEquals(listOf("pattern", "declaration"), ownProperties("$asm.AglStyleMetaRuleDefault"))
        assertEquals(listOf("selector", "declaration"), ownProperties("$asm.AglStyleTagRuleDefault"))
        assertEquals(listOf("name", "namespace"), ownProperties("$asm.AglStyleDomainDefault"))
    }

    @Test
    fun all_types_resolve() {
        // every property type is found (no unresolved type name)
        AglStyle.typesDomain.namespace.filter { it.qualifiedName.value.startsWith("net.akehurst.language.style") }.forEach { ns ->
            ns.definition.forEach { td ->
                td.property.forEach { p ->
                    assertNotNull(p.typeInstance.resolvedDefinitionOrNull, "${td.qualifiedName.value}.${p.name.value}: ${p.typeInstance}")
                }
            }
        }
    }
}
