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

import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.asm.grammar

internal object AglCrossReferences {
    //: GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "References") {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "CrossReferences"
    ) {
        extendsGrammar(AglExpressions.grammar.selfReference)
        list("unit", 0, -1) { ref("namespace") }
        concatenation("namespace") {
            lit("namespace"); ref("qualifiedName"); lit("{"); ref("imports"); ref("declarations"); lit("}")
        }
        list("imports", 0, -1) { ref("import") }
        concatenation("declarations") {
            ref("rootIdentifiables"); ref("scopes"); opt { ref("references") }
        }
        list("rootIdentifiables", 0, -1) { ref("identifiable") }
        list("scopes", 0, -1) { ref("scope") }
        concatenation("scope") {
            lit("scope"); ref("simpleTypeName"); lit("{"); ref("identifiables"); lit("}")
        }
        list("identifiables", 0, -1) { ref("identifiable") }
        concatenation("identifiable") {
            lit("identify"); ref("simpleTypeName"); lit("by"); ref("expression")
        }
        concatenation("references") {
            lit("references"); lit("{"); ref("referenceDefinitions"); lit("}")
        }
        list("referenceDefinitions", 0, -1) { ref("referenceDefinition") }
        concatenation("referenceDefinition") {
            lit("in"); ref("simpleTypeName"); lit("{"); ref("referenceExpressionList"); lit("}")
        }
        list("referenceExpressionList", 0, -1) { ref("referenceExpression") }
        choice("referenceExpression") {
            ref("propertyReferenceExpression")
            ref("collectionReferenceExpression")
        }
        concatenation("propertyReferenceExpression") {
            lit("property"); ref("rootOrNavigation"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }
        }
        concatenation("from") { lit("from"); ref("navigation") }
        concatenation("collectionReferenceExpression") {
            lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; lit("{"); ref("referenceExpressionList"); lit("}")
        }
        concatenation("ofType") { lit("of-type"); ref("possiblyQualifiedTypeReference") }
        choice("rootOrNavigation") {
            ref("root")
            ref("navigation")
        }
        separatedList("typeReferences", 1, -1) { ref("possiblyQualifiedTypeReference"); lit("|") }
        concatenation("possiblyQualifiedTypeReference") { ref("qualifiedName") }
        concatenation("simpleTypeName") { ref("IDENTIFIER") }
    }

    const val grammarStr = """namespace net.akehurst.language.agl.language

grammar References extends Expressions {

    unit = namespace* ;
    namespace = 'namespace' qualifiedName '{' imports declarations '}' ;
    imports = import*;
    declarations = rootIdentifiables scopes references? ;
    rootIdentifiables = identifiable* ;
    scopes = scope* ;
    scope = 'scope' simpleTypeName '{' identifiables '}' ;
    identifiables = identifiable* ;
    identifiable = 'identify' simpleTypeName 'by' expression ;

    references = 'references' '{' referenceDefinitions '}' ;
    referenceDefinitions = referenceDefinition* ;
    referenceDefinition = 'in' simpleTypeName '{' referenceExpression* '}' ;
    referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
    propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? ;
    from = 'from' navigation ;
    collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
    ofType = 'of-type' possiblyQualifiedTypeReference ;
    
    rootOrNavigation = root | navigation ;
    
    typeReferences = [possiblyQualifiedTypeReference / '|']+ ;
    possiblyQualifiedTypeReference = qualifiedName ;
    simpleTypeName = IDENTIFIER ;
}
"""

    const val styleStr = """${"$"}keyword {
  foreground: darkgreen;
  font-style: bold;
}"""

    const val formatterStr = """
@TODO
References -> when {
    referenceDefinitions.isEmpty -> "references { }"
    else -> "
      references {
        referenceDefinitions
      }
    "
}
ReferenceDefinitions -> [referenceDefinition / '\n']
ReferenceDefinition -> "in §typeReference property §propertyReference refers-to §typeReferences"
    """

    //init {
    //    super.extends.add(AglExpressions.grammar.selfReference)
    //    super.grammarRule.addAll(createRules())
    // }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}