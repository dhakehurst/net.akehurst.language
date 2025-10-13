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

package net.akehurst.language.style.builder

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.regex.api.UnescapedPattern
import net.akehurst.language.style.api.*
import net.akehurst.language.style.asm.*

@DslMarker
annotation class StyleModelDslMarker

fun styleDomain(name: String, init: StyleDomainBuilder.() -> Unit): AglStyleDomain {
    val b = StyleDomainBuilder(SimpleName(name))
    b.init()
    return b.build()
}


@StyleModelDslMarker
class StyleDomainBuilder(
    private val _name: SimpleName
) {

    private val _namespaces = mutableListOf<StyleNamespace>()

    fun namespace(qualifiedName: String, init: StyleNamespaceBuilder.() -> Unit) {
        val b = StyleNamespaceBuilder(QualifiedName(qualifiedName))
        b.init()
        _namespaces.add( b.build() )
    }

    fun build(): AglStyleDomain = AglStyleDomainDefault(_name).also { mdl ->
        _namespaces.forEach { namespace -> mdl.addNamespace(namespace) }
    }
}

@StyleModelDslMarker
class StyleNamespaceBuilder internal constructor(
    qualifiedName: QualifiedName
) {
    private val _namespace = StyleNamespaceDefault(qualifiedName)

    fun styles(name: String, init: StylesBuilder.() -> Unit) {
        val b = StylesBuilder(_namespace, SimpleName(name))
        b.init()
        b.build()
    }

    fun build() = _namespace
}

@StyleModelDslMarker
class StylesBuilder internal constructor(
    private val _namespace: StyleNamespace,
    private val _name: SimpleName
) {

    private val _extends = mutableListOf<StyleSetReference>()
    private val _rules = mutableListOf<AglStyleRule>()

    fun extends(styleSetName: String) {
        _extends.add(StyleSetReferenceDefault(_namespace, styleSetName.asPossiblyQualifiedName))
    }

    fun metaRule(unescapedPattern: String, init: StyleMetaRuleBuilder.() -> Unit) {
        val b = StyleMetaRuleBuilder(UnescapedPattern(unescapedPattern))
        b.init()
        _rules.add(b.build())
    }

    fun tagRule(vararg tag: String, init: StyleTagRuleBuilder.() -> Unit) {
        val b = StyleTagRuleBuilder(tag.map {
            val kind = when{
                it.startsWith("'") -> AglStyleSelectorKind.LITERAL
                it.startsWith("\"") -> AglStyleSelectorKind.PATTERN
                it.startsWith("$") -> AglStyleSelectorKind.SPECIAL
                else -> AglStyleSelectorKind.RULE_NAME
            }
            AglStyleSelector(it, kind)
        })
        b.init()
        _rules.add(b.build())
    }

    fun build() = AglStyleSetDefault(_namespace, _name, _extends).also { ss ->
        (ss.rules as MutableList).addAll(_rules)
    }
}

@StyleModelDslMarker
class StyleMetaRuleBuilder internal constructor(
    private val _pattern: UnescapedPattern
) {
    private val _declarations = mutableListOf<AglStyleDeclaration>()

    fun declaration(name: String, value: String) {
        _declarations.add(AglStyleDeclaration(name, value))
    }

    fun build() = AglStyleMetaRuleDefault(_pattern.escapedFoAgl).also { rule ->
        _declarations.forEach {
            rule.declaration[it.name] = it
        }
    }
}

@StyleModelDslMarker
class StyleTagRuleBuilder internal constructor(
    private val _selectors: List<AglStyleSelector>
) {
    private val _declarations = mutableListOf<AglStyleDeclaration>()

    fun declaration(name: String, value: String) {
        _declarations.add(AglStyleDeclaration(name, value))
    }

    fun build() = AglStyleTagRuleDefault(_selectors).also { rule ->
        _declarations.forEach {
            rule.declaration[it.name] = it
        }
    }
}