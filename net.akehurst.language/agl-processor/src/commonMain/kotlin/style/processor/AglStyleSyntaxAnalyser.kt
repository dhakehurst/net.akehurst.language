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

package net.akehurst.language.style.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.language.regex.api.EscapedPattern
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.style.api.*
import net.akehurst.language.style.asm.*

internal class AglStyleSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AglStyleDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<AglStyleDomain>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::styleSet)
        super.register(this::extends)
        super.register(this::typeImport)
        super.register(this::rule)
        super.register(this::metaRule)
        super.register(this::tagRule)
        super.register(this::selectorExpression)
        super.register(this::selectorAndComposition)
        super.register(this::selectorSingle)
        super.register(this::semanticSelector)
        super.register(this::semanticKind)
        super.register(this::style)
        super.register(this::styleValue)
        super.register(this::possiblyQualifiedName)
    }

    private val _localStore = mutableMapOf<String, Any>()

    /** the location of each possiblyQualifiedName node, in the order the nodes end (used to locate each name of an 'extends') */
    private val _pqnLocations = mutableListOf<InputLocation>()

    /** the location of each 'style' node, in the order the nodes end (used to locate a repeated declaration) */
    private val _styleLocations = mutableListOf<InputLocation>()

    // possiblyQualifiedName = [IDENTIFIER / '.']+ ; (from Base, the same value as BaseSyntaxAnalyser gives, but its location is kept)
    fun possiblyQualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName {
        _pqnLocations.add(sentence.locationForNode(nodeInfo.node))
        return (children as List<String>).joinToString(separator = "").asPossiblyQualifiedName
    }

    // unit = namespace rule* ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleDomainDefault {
        val ns = children[0] as StyleNamespaceDefault
        val ruleBuilder = children[1] as List<((ns: StyleNamespaceDefault) -> Unit)>
        ruleBuilder.forEach { it.invoke(ns) }
        _pqnLocations.clear()
        _styleLocations.clear()
        val su = AglStyleDomainDefault(name = SimpleName("ParsedStyleUnit"), namespace = listOf(ns))
        return su
    }

    // namespace = namespace possiblyQualifiedName ;
    // namespace = 'namespace' possiblyQualifiedName option* import* definition* ; (from Base)
    // the options and imports are kept (SD3); the imports are used to find the style sets an 'extends' names
    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): StyleNamespace {
        val qualifiedName = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, Any>>
        val imports = children[3] as List<Import>
        return StyleNamespaceDefault(
            qualifiedName = qualifiedName.asQualifiedName(null),
            options = OptionHolderDefault(null, options.toMap()),
            import = imports
        )
    }

    // styleSet = 'styles' IDENTIFIER extends? '{' typeImport* rule* '}' ;
    fun styleSet(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: StyleNamespaceDefault) -> Unit {
        val name = SimpleName(children[1] as String)
        val extendsLambda = children[2] as? (StyleNamespace) -> List<StyleSetReference>
        val typeImports = children[4] as List<Import>
        val rules: List<AglStyleTagRule> = children[5] as List<AglStyleTagRule>
        val location = sentence.locationForNode(nodeInfo.node)
        return { ns ->
            when {
                // a second style set with the same name is reported, not added (the first one is kept)
                null != ns.findOwnedDefinitionOrNull(name) -> issues.error(
                    location,
                    "namespace '${ns.qualifiedName.value}' already contains a style set named '${name.value}'."
                )

                else -> {
                    val extends = extendsLambda?.invoke(ns) ?: emptyList()
                    val ss = AglStyleSetDefault(ns, name, extends)
                        .also { setLocationFor(it, nodeInfo, sentence) }
                    typeImports.forEach { ss.addImportType(it) }
                    (ss.rules as MutableList).addAll(rules)
                }
            }
        }
    }

    // typeImport = 'import-types' possiblyQualifiedName ;
    fun typeImport(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
        Import((children[1] as PossiblyQualifiedName).value)

    // extends = ':' [possiblyQualifiedName / ',']+ ;
    fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (StyleNamespace) -> List<StyleSetReference> {
        val extendNameList = children[1] as List<PossiblyQualifiedName>
        val sl = extendNameList.toSeparatedList<Any, PossiblyQualifiedName, String>()
        // the names are the last possiblyQualifiedName nodes ended before this one, in order
        val nameLocations = _pqnLocations.takeLast(sl.items.size)
        repeat(sl.items.size) { _pqnLocations.removeAt(_pqnLocations.lastIndex) }
        return { ns ->
            sl.items.mapIndexed { i, pqn ->
                // need to manually add the Reference as it is not seen by the super class; each reference is located at its name
                StyleSetReferenceDefault(ns, pqn).also { locationMap.add(nodeInfo.path + i.toString(), it, nameLocations[i]) }
            }
        }
    }

    //  rule = metaRule | styleRule ;
    fun rule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleRule =
        children[0] as AglStyleRule

    // metaRule = '$$' PATTERN '{' style* '}' ;
    fun metaRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleMetaRule {
        val patternStr = children[1] as String //TODO: ? selector combinations, and/or/contains etc
        val escaped = EscapedPattern(patternStr.removeSurrounding("\""))
        val styles: List<AglStyleDeclaration> = children[3] as List<AglStyleDeclaration>
        val rule = AglStyleMetaRuleDefault(escaped)
        addDeclarations(rule.declaration, styles)
        return rule
    }

    // tagRule = selectorExpression '{' style* '}' ;
    fun tagRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleTagRule {
        val selector = children[0] as List<AglStyleSelector> //TODO: ? selector combinations, and/or/contains etc

        val styles: List<AglStyleDeclaration> = children[2] as List<AglStyleDeclaration>
        val rule = AglStyleTagRuleDefault(selector)
        addDeclarations(rule.declaration, styles)
        return rule
    }

    // selectorExpression
    //             = selectorAndComposition
    //             | selectorSingle
    //             ; //TODO
    fun selectorExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        when (nodeInfo.alt.option.asIndex) {
            0 -> children[0] as List<AglStyleSelector>
            1 -> listOf(children[0] as AglStyleSelector)
            else -> error("Internal error: alternative 'selectorExpression' not handled")
        }


    // selectorAndComposition = [selectorSingle /',']2+ ;
    fun selectorAndComposition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AglStyleSelector> =
        (children as List<Any>).toSeparatedList<Any, AglStyleSelector, String>().items

    // selectorSingle = LITERAL | PATTERN | IDENTIFIER | SPECIAL_IDENTIFIER | semanticSelector ;
    fun selectorSingle(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleSelector {
        if (4 == nodeInfo.alt.option.asIndex) return children[0] as AglStyleSelector
        // the value is the selector as written, with its enclosing quotes and escapes (e.g. '\''), which is how it is matched
        val mt = children[0] as String
        val kind = when (nodeInfo.alt.option.asIndex) {
            0 -> AglStyleSelectorKind.LITERAL
            1 -> AglStyleSelectorKind.PATTERN
            2 -> AglStyleSelectorKind.RULE_NAME
            3 -> AglStyleSelectorKind.SPECIAL
            else -> error("Internal error: AglStyleSelectorKind not handled")
        }
        return AglStyleSelector(mt, kind)
    }

    // semanticSelector = semanticKind possiblyQualifiedName ;
    // the value is the tag a token gets from semantic analysis, e.g. 'refers-to Circle'
    fun semanticSelector(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleSelector {
        val kindText = children[0] as String
        val typeName = children[1] as PossiblyQualifiedName
        val kind = when (kindText) {
            "type" -> AglStyleSelectorKind.TYPE
            "identify" -> AglStyleSelectorKind.IDENTIFY
            "refers-to" -> AglStyleSelectorKind.REFERS_TO
            else -> error("Internal error: semanticKind '$kindText' not handled")
        }
        return AglStyleSelector("$kindText ${typeName.value}", kind)
    }

    // semanticKind = 'type' | 'identify' | 'refers-to' ;
    fun semanticKind(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // style = STYLE_ID ':' styleValue ';' ;
    fun style(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AglStyleDeclaration {
        val name = children[0] as String
        val value = children[2] as String
        _styleLocations.add(sentence.locationForNode(nodeInfo.node))
        return AglStyleDeclaration(name, value)
    }

    /**
     * the declarations of a rule, in order; a name declared more than once gives a warning, the last value is used (as in CSS).
     * The declarations are the last 'style' nodes ended before the rule.
     */
    private fun addDeclarations(declaration: MutableMap<String, AglStyleDeclaration>, styles: List<AglStyleDeclaration>) {
        val locations = _styleLocations.takeLast(styles.size)
        repeat(styles.size) { _styleLocations.removeAt(_styleLocations.lastIndex) }
        styles.forEachIndexed { i, st ->
            if (declaration.containsKey(st.name)) {
                issues.warn(locations[i], "Style '${st.name}' is declared more than once in this rule, the last value is used")
            }
            declaration[st.name] = st
        }
    }

    // styleValue = STYLE_VALUE | STRING ;
    // a STRING value (e.g. '#82AAFF') is given without its enclosing quotes
    fun styleValue(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        val str = children[0] as String
        return when {
            str.length >= 2 && str.startsWith("'") && str.endsWith("'") -> str.substring(1, str.length - 1).replace("\\'", "'")
            str.length >= 2 && str.startsWith("\"") && str.endsWith("\"") -> str.substring(1, str.length - 1).replace("\\\"", "\"")
            else -> str
        }
    }

}