/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.reference.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.api.CrossReferenceNamespace
import net.akehurst.language.reference.api.ScopeImportKind
import net.akehurst.language.reference.asm.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.ParsePath
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class ReferencesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<CrossReferenceDomain>() {

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::declarations)
        super.register(this::rootDeclarations)
        super.register(this::rootDeclaration)
        super.register(this::importIntoScope)
        super.register(this::typeGroup)
        super.register(this::scopes)
        super.register(this::scope)
        super.register(this::identifiables)
        super.register(this::identifiable)
        super.register(this::identifyingExpressions)
        super.register(this::visibility)
        super.register(this::aliasFor)
        super.register(this::scopeImports)
        super.register(this::scopeImport)
        super.register(this::importDefinition)
        super.register(this::importKind)
        super.register(this::recursive)
        super.register(this::inheritDefinition)
        super.register(this::inheritNamedDefinition)
        super.register(this::inheritByPositionDefinition)
        super.register(this::namedPath)
        super.register(this::namedSegment)
        super.register(this::fromInherited)
        super.register(this::referencesOpt)
        super.register(this::references)
        super.register(this::referenceDefinitions)
        super.register(this::referenceDefinition)
        super.register(this::referenceExpressionList)
        super.register(this::referenceExpression)
        super.register(this::propertyReferenceExpression)
        super.register(this::propertyReferenceWhen)
        super.register(this::propertyWhenOption)
        super.register(this::propertyWhenElse)
        super.register(this::propertyWhenItem)
        super.register(this::from)
        super.register(this::fromElse)
        super.register(this::condition)
        super.register(this::collectionReferenceExpression)
        super.register(this::rootOrNavigation)
        super.register(this::ofType)
        super.register(this::typeReferences)
        super.register(this::possiblyQualifiedTypeReference)
        super.register(this::simpleTypeName)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    //  Base::unit = options* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CrossReferenceDomainDefault {
        val options = children[0] as List<Pair<String, String>>
        val namespace = children[1] as List<CrossReferenceNamespace>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val result = CrossReferenceDomainDefault(SimpleName("Unit"), optHolder, namespace)
        //namespace.forEach { result.declarationsForNamespace[it.qualifiedName] = it }
        return result
    }

    // override namespace = namespace = 'namespace' possiblyQualified import* declarations  ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CrossReferenceNamespace {
        val qualifiedName = children[1] as PossiblyQualifiedName
        //TODO: parse options
        val imports = children[2] as List<Import>
        val declarations = children[3] as (CrossReferenceNamespace) -> DeclarationsForNamespaceDefault

        val ns = CrossReferenceNamespaceDefault(qualifiedName = qualifiedName.asQualifiedName(null), import = imports)
        val def = declarations.invoke(ns)
        return ns
    }

    // declarations = rootDeclarations scopes referencesOpt
    private fun declarations(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (CrossReferenceNamespace) -> DeclarationsForNamespaceDefault {
        val rootDeclarations = children[0] as List<Any>
        val rootIdentifiables = rootDeclarations.filterIsInstance<IdentifiableDefault>()
        val importsIntoScope = rootDeclarations.filterIsInstance<ScopeImportDefault>()
        val typeGroups = rootDeclarations.filterIsInstance<TypeGroupDefault>()
        val scopes = children[1] as List<ScopeDefinitionDefault>
        val referencesOpt = children[2] as List<ReferenceDefinitionDefault>?
        return { ns ->
            val decl = DeclarationsForNamespaceDefault(ns)
            (decl.scopeDefinition[CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last]?.identifiables as MutableList?)?.addAll(rootIdentifiables)
            // top-level 'import-into-scope' declarations are held by the root scope definition (like top-level 'identify')
            (decl.scopeDefinition[CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last]?.scopeImports as MutableList?)?.addAll(importsIntoScope)
            scopes.forEach {
                // one 'scope' block per type: a second block for the same type is an error (it would replace the first)
                if (decl.scopeDefinition.containsKey(it.scopeForTypeName)) {
                    issues.error(locationMap[it.scopeForTypeName], "There is more than one 'scope ${it.scopeForTypeName.value} { }' block in namespace '${ns.qualifiedName.value}', only one is allowed for a type", null)
                }
                decl.scopeDefinition[it.scopeForTypeName] = it
            }
            referencesOpt?.let {
                // one 'in' block per type: a second block for the same type is an error
                val seen = mutableSetOf<PossiblyQualifiedName>()
                referencesOpt.forEach {
                    if (!seen.add(it.inTypeName)) {
                        issues.error(locationMap[it.inTypeName], "There is more than one 'in ${it.inTypeName.value} { }' block in namespace '${ns.qualifiedName.value}', only one is allowed for a type", null)
                    }
                }
                decl.references.addAll(referencesOpt)
            }
            typeGroups.forEach {
                if (decl.typeGroups.containsKey(it.name)) {
                    issues.error(locationMap[it], "There is more than one 'type-group ${it.name.value}' in namespace '${ns.qualifiedName.value}'", null)
                }
                decl.typeGroups[it.name] = it
            }
            expandTypeGroups(decl)
            decl.also { setLocationFor(it,nodeInfo,sentence) }
        }
    }

    // typeGroup = 'type-group' IDENTIFIER '=' typeReferences ;
    private fun typeGroup(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeGroupDefault =
        TypeGroupDefault(SimpleName(children[1] as String), children[3] as List<PossiblyQualifiedName>).also { setLocationFor(it, nodeInfo, sentence) }

    /** replace the 'type-group' names in each 'refers-to' by the group's list (a group may use other groups) */
    private fun expandTypeGroups(decl: DeclarationsForNamespaceDefault) {
        if (decl.typeGroups.isEmpty()) return
        val expanded = mutableMapOf<SimpleName, List<PossiblyQualifiedName>>()
        fun expand(names: List<PossiblyQualifiedName>, visiting: List<SimpleName>): List<PossiblyQualifiedName> = names.flatMap { n ->
            val grp = decl.typeGroups[SimpleName(n.value)]
            when {
                null == grp -> listOf(n)
                grp.name in visiting -> {
                    issues.error(locationMap[grp], "'type-group ${grp.name.value}' uses itself: ${(visiting + grp.name).joinToString(" -> ") { it.value }}", null)
                    emptyList()
                }
                else -> expanded.getOrPut(grp.name) { expand(grp.typeNames, visiting + grp.name) }
            }
        }.distinct()
        decl.typeGroups.values.forEach { expand(listOf(it.name.value.asPossiblyQualifiedName), emptyList()) }
        fun visit(list: List<ReferenceExpression>) {
            list.forEach { re ->
                when (re) {
                    is ReferenceExpressionPropertyDefault -> {
                        val e = expand(re.refersToTypeNameAsWritten, emptyList())
                        if (e != re.refersToTypeNameAsWritten) re.refersToTypeNameExpanded = e
                    }
                    is ReferenceExpressionCollectionDefault -> visit(re.referenceExpressionList)
                    else -> Unit
                }
            }
        }
        decl.references.forEach { visit(it.referenceExpressionList) }
    }

    // rootDeclarations = rootDeclaration* ;
    private fun rootDeclarations(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Any> =
        children as List<Any>

    // rootDeclaration = identifiable | importIntoScope ;
    private fun rootDeclaration(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Any =
        children[0] as Any

    // scopes = scope* ;
    private fun scopes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ScopeDefinitionDefault> =
        children as List<ScopeDefinitionDefault>

    // scope = 'scope' possiblyQualifiedTypeReference '{' identifiables scopeImports '}'
    private fun scope(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeDefinitionDefault {
        val simpleTypeName = children[1] as PossiblyQualifiedName
        val identifiables = children[3] as List<IdentifiableDefault>
        val scopeImports = (children[4] as List<ScopeImportDefault>?) ?: emptyList()
        val scope = ScopeDefinitionDefault(simpleTypeName)
        scope.identifiables.addAll(identifiables)
        scope.scopeImports.addAll(scopeImports)
        locationMap.add(ParsePath(), PropertyValue(scope, "typeReference"), locationMap[simpleTypeName]!!)
        return scope
    }

    // identifyingExpressions = [expression / '|']+ ;
    private fun identifyingExpressions(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> =
        (children as List<Any?>).toSeparatedList<Any?, Expression, String>().items

    // identifiables = identifiable*
    private fun identifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<IdentifiableDefault> =
        (children as List<IdentifiableDefault>?) ?: emptyList()

    // identifiable = 'identify' possiblyQualifiedTypeReference 'by' identifyingExpressions visibility? aliasFor? ;
    private fun identifiable(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IdentifiableDefault {
        val simpleTypeName = children[1] as PossiblyQualifiedName
        val alternatives = children[3] as List<Expression>
        val expression = alternatives.first()
        val visibility = children[4] as Expression?
        val aliasFor = children[5] as NavigationExpression?
        val identifiable = IdentifiableDefault(simpleTypeName, expression, visibility, aliasFor, alternatives)
        locationMap.add(ParsePath(), PropertyValue(identifiable, "simpleTypeName"), locationMap[simpleTypeName]!!)
        locationMap.add(ParsePath(), PropertyValue(identifiable, "expression"), locationMap[expression]!!)
//        locationMap[PropertyValue(identifiable, "simpleTypeName")] = locationMap[simpleTypeName]!!
//        locationMap[PropertyValue(identifiable, "expression")] = locationMap[expression]!!
        return identifiable
    }

    // visibility = 'visibility' expression ;
    private fun visibility(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[1] as Expression

    // aliasFor = 'alias-for' rootOrNavigation ;
    private fun aliasFor(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationExpression =
        asNavigation(children[1] as Expression)

    // scopeImports = scopeImport* ;
    private fun scopeImports(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ScopeImportDefault> =
        children as List<ScopeImportDefault>

    // scopeImport = importDefinition | inheritDefinition ;
    private fun scopeImport(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault =
        children[0] as ScopeImportDefault

    // importDefinition = 'import' 'forall' rootOrNavigation ofType? importKind rootOrNavigation recursive? visibility? ;
    private fun importDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault {
        val elements = children[2] as Expression
        val ofType = children[3] as PossiblyQualifiedName?
        val kind = children[4] as ScopeImportKind
        val target = asNavigation(children[5] as Expression)
        val recursive = children[6] as Expression?
        val visibility = children[7] as Expression?
        val condition = children[8] as Expression?
        return ScopeImportDefault(kind, elements, ofType, target, recursive, visibility, condition)
    }

    // importIntoScope = 'import-into-scope' possiblyQualifiedTypeReference importKind rootOrNavigation recursive? visibility? condition? ;
    private fun importIntoScope(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault {
        val ofType = children[1] as PossiblyQualifiedName
        val kind = children[2] as ScopeImportKind
        val target = asNavigation(children[3] as Expression)
        val recursive = children[4] as Expression?
        val visibility = children[5] as Expression?
        val condition = children[6] as Expression?
        return ScopeImportDefault(kind, RootExpressionDefault.SELF, ofType, target, recursive, visibility, condition, intoScope = true)
    }

    // importKind = 'members-of' | 'element' ;
    private fun importKind(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportKind =
        when (children[0] as String) {
            "element" -> ScopeImportKind.IMPORT_ELEMENT
            else -> ScopeImportKind.IMPORT_MEMBERS
        }

    // recursive = 'recursive' expression ;
    private fun recursive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[1] as Expression

    // inheritDefinition = 'inherit' 'forall' rootOrNavigation ofType? 'members-of' rootOrNavigation condition? ;
    private fun inheritDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault {
        val elements = children[2] as Expression
        val ofType = children[3] as PossiblyQualifiedName?
        val target = asNavigation(children[5] as Expression)
        val condition = children[6] as Expression?
        return ScopeImportDefault(ScopeImportKind.INHERIT, elements, ofType, target, null, null, condition)
    }

    // inheritNamedDefinition = 'inherit' 'named' namedPath condition? ;
    private fun inheritNamedDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault {
        val named = children[2] as List<String>
        val condition = children[3] as Expression?
        val nothing = NavigationExpressionDefault(RootExpressionDefault.NOTHING, emptyList())
        return ScopeImportDefault(ScopeImportKind.INHERIT, RootExpressionDefault.NOTHING, null, nothing, null, null, condition, named)
    }

    // inheritByPositionDefinition = 'inherit' 'by-position' condition? ;
    private fun inheritByPositionDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeImportDefault {
        val condition = children[2] as Expression?
        val nothing = NavigationExpressionDefault(RootExpressionDefault.NOTHING, emptyList())
        return ScopeImportDefault(ScopeImportKind.INHERIT, RootExpressionDefault.NOTHING, null, nothing, null, null, condition, byPosition = true)
    }

    // namedPath = [namedSegment / '.']+ ;
    private fun namedPath(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        (children as List<Any?>).toSeparatedList<Any?, String, String>().items

    // namedSegment = IDENTIFIER | STRING ;  a STRING is written in single quotes, with \' and \\ escaped
    private fun namedSegment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        (children[0] as String).let { s ->
            when {
                s.startsWith("'") -> s.substring(1, s.length - 1).replace("\\'", "'").replace("\\\\", "\\")
                else -> s
            }
        }

    // fromInherited = 'inherited' ;
    private fun fromInherited(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Boolean = true

    private fun asNavigation(expr: Expression): NavigationExpression = when (expr) {
        is NavigationExpression -> expr
        is RootExpression -> NavigationExpressionDefault(RootExpressionDefault(expr.name), emptyList())
        else -> {
            issues.error(locationMap[expr], "Expected a navigation, got '${expr.asString()}'", null)
            NavigationExpressionDefault(RootExpressionDefault.ERROR, emptyList())
        }
    }

    // referencesOpt = references?
    private fun referencesOpt(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault>? =
        children[0] as List<ReferenceDefinitionDefault>?

    // references = 'references' '{' referenceDefinitions '}' ;
    private fun references(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children[2] as List<ReferenceDefinitionDefault>

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children as List<ReferenceDefinitionDefault>

    // referenceDefinition = 'in' possiblyQualifiedTypeReference '{' referenceExpressionList '}'
    private fun referenceDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceDefinitionDefault {
        val inTypeName = children[1] as PossiblyQualifiedName
        val referenceExpressionList = children[3] as List<ReferenceExpressionAbstract>
        val def = ReferenceDefinitionDefault(inTypeName, referenceExpressionList)
//        locationMap[PropertyValue(def, "in")] = locationMap[inTypeName]!!
        locationMap.add(ParsePath(), PropertyValue(def, "in"), locationMap[inTypeName]!!)
        // locationMap[PropertyValue(def, "propertyReference")] = locationMap[referringPropertyName]!!
        //typeReferences.forEachIndexed { i, n ->
        //    locationMap[PropertyValue(def, "typeReferences[$i]")] = n.second
        //}
        return def
    }

    // referenceExpressionList = referenceExpression* ;
    // a 'property p when { ... }' gives one reference expression per option
    private fun referenceExpressionList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceExpressionAbstract> =
        children.flatMap { if (it is List<*>) it as List<ReferenceExpressionAbstract> else listOf(it as ReferenceExpressionAbstract) }

    //referenceExpression = propertyReferenceExpression | propertyReferenceWhen | collectionReferenceExpression ;
    private fun referenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Any =
        children[0]!!

    // propertyReferenceWhen = 'property' rootOrNavigation 'when' '{' propertyWhenItem+ '}' ;
    // one reference expression per option, in order, each with the option's condition ('else' has none); the first whose condition holds is used
    private fun propertyReferenceWhen(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceExpressionPropertyDefault> {
        val navigation = asNavigation(children[1] as Expression)
        val options = children[4] as List<WhenOption>
        options.dropLast(1).filter { null == it.condition }.forEach {
            issues.error(locationMap[navigation], "In 'property ${navigation.asString()} when { }', 'else' must be the last option", null)
        }
        return options.mapIndexed { i, opt ->
            ReferenceExpressionPropertyDefault(navigation, opt.typeReferences, opt.from?.first, opt.condition, opt.from?.second, opt.from?.third ?: false, i).also {
                opt.typeReferences.forEachIndexed { j, n ->
                    locationMap.add(ParsePath(), PropertyValue(it, "typeReferences[$j]"), locationMap[n]!!)
                }
            }
        }
    }

    // propertyWhenItem = propertyWhenOption | propertyWhenElse ;
    private fun propertyWhenItem(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption =
        children[0] as WhenOption

    private data class WhenOption(val condition: Expression?, val typeReferences: List<PossiblyQualifiedName>, val from: Triple<NavigationExpression, NavigationExpression?, Boolean>?)

    // propertyWhenOption = expression '->' 'refers-to' typeReferences from? ;
    private fun propertyWhenOption(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption =
        WhenOption(children[0] as Expression, children[3] as List<PossiblyQualifiedName>, children[4] as Triple<NavigationExpression, NavigationExpression?, Boolean>?)

    // propertyWhenElse = 'else' '->' 'refers-to' typeReferences from? ;
    private fun propertyWhenElse(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption =
        WhenOption(null, children[3] as List<PossiblyQualifiedName>, children[4] as Triple<NavigationExpression, NavigationExpression?, Boolean>?)

    //propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? ;
    private fun propertyReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceExpressionPropertyDefault {
        val expr = children[1] as Expression
        val typeReferences = children[3] as List<PossiblyQualifiedName>
        val from = children[4] as Triple<NavigationExpression, NavigationExpression?, Boolean>?
        val condition = children[5] as Expression?

        val navigation = when (expr) {
            is NavigationExpression -> expr
            is RootExpression -> NavigationExpressionDefault(RootExpressionDefault(expr.name), emptyList())
            else -> {
                issues.error(locationMap[expr], "", null)
                NavigationExpressionDefault(RootExpressionDefault.ERROR, emptyList())
            }
        }
        return ReferenceExpressionPropertyDefault(navigation, typeReferences, from?.first, condition, from?.second, from?.third ?: false).also {
            typeReferences.forEachIndexed { i, n ->
//                locationMap[PropertyValue(it, "typeReferences[$i]")] = locationMap[n]!!
                locationMap.add(ParsePath(), PropertyValue(it, "typeReferences[$i]"), locationMap[n]!!)
            }
        }
    }

    // condition = 'when' expression ;
    private fun condition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[1] as Expression

    // from = 'from' fromInherited? rootOrNavigation fromElse? ;
    private fun from(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Triple<NavigationExpression, NavigationExpression?, Boolean> =
        Triple(asNavigation(children[2] as Expression), children[3] as NavigationExpression?, (children[1] as Boolean?) ?: false)

    // fromElse = 'else' rootOrNavigation ;
    private fun fromElse(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationExpression =
        asNavigation(children[1] as Expression)

    // collectionReferenceExpression = 'forall' rootOrNavigation ofType? condition? '{' referenceExpressionList '}' ;
    private fun collectionReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceExpressionCollectionDefault {
        val rootOrNavigation = children[1] as Expression
        val ofType = children[2] as PossiblyQualifiedName?
        val condition = children[3] as Expression?
        val referenceExpression = children[5] as List<ReferenceExpressionAbstract>
        return ReferenceExpressionCollectionDefault(rootOrNavigation, ofType, referenceExpression, condition)
    }

    // ofType = 'of-type' possiblyQualifiedTypeReference ;
    private fun ofType(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[1] as PossiblyQualifiedName

    //rootOrNavigation = root | navigation ;
    private fun rootOrNavigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // typeReferences = [possiblyQualifiedTypeReference / '|']+
    private fun typeReferences(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PossiblyQualifiedName> { //List<Pair<String, InputLocation>> {
        return (children as List<Any>).toSeparatedList<Any, PossiblyQualifiedName, String>().items
    }

    // possiblyQualifiedTypeReference = possiblyQualifiedName
    private fun possiblyQualifiedTypeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[0] as PossiblyQualifiedName

    // simpleTypeName = IDENTIFIER ;
    private fun simpleTypeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): SimpleName =
        SimpleName((children[0] as String))

}