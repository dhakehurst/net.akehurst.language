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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.language.expressions.AglExpressions
import net.akehurst.language.agl.language.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.language.grammar.asm.NamespaceDefault
import net.akehurst.language.agl.language.grammar.asm.builder.grammar
import net.akehurst.language.api.language.grammar.GrammarRule

/**

 */
internal object AglCrossReferences {
    //: GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "References") {
    const val goalRuleName = "unit"
    private fun createRules(): List<GrammarRule> {
        val b = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language"), "References");
        b.extendsGrammar(AglExpressions.grammar)

        b.rule("unit").multi(0, -1, b.nonTerminal("namespace"))
        b.rule("namespace").concatenation(
            b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"), b.terminalLiteral("{"),
            b.nonTerminal("imports"),
            b.nonTerminal("declarations"),
            b.terminalLiteral("}")
        )
        b.rule("imports").multi(0, -1, b.nonTerminal("import"))
        b.rule("declarations").concatenation(b.nonTerminal("rootIdentifiables"), b.nonTerminal("scopes"), b.nonTerminal("referencesOpt"))
        b.rule("rootIdentifiables").multi(0, -1, b.nonTerminal("identifiable"))
        b.rule("scopes").multi(0, -1, b.nonTerminal("scope"))
        b.rule("scope").concatenation(b.terminalLiteral("scope"), b.nonTerminal("typeReference"), b.terminalLiteral("{"), b.nonTerminal("identifiables"), b.terminalLiteral("}"))
        b.rule("identifiables").multi(0, -1, b.nonTerminal("identifiable"))
        b.rule("identifiable").concatenation(b.terminalLiteral("identify"), b.nonTerminal("typeReference"), b.terminalLiteral("by"), b.nonTerminal("expression"))
        b.rule("referencesOpt").optional(b.nonTerminal("references"))
        b.rule("references").concatenation(
            b.terminalLiteral("references"),
            b.terminalLiteral("{"),
            b.nonTerminal("referenceDefinitions"),
            b.terminalLiteral("}")
        )
        b.rule("referenceDefinitions").multi(0, -1, b.nonTerminal("referenceDefinition"))
        b.rule("referenceDefinition").concatenation(
            b.terminalLiteral("in"),
            b.nonTerminal("typeReference"),
            b.terminalLiteral("{"),
            b.nonTerminal("referenceExpressionList"),
            b.terminalLiteral("}"),
        )
        b.rule("referenceExpressionList").multi(0, -1, b.nonTerminal("referenceExpression"))
        b.rule("referenceExpression").choiceLongestFromConcatenationItem(
            b.nonTerminal("propertyReferenceExpression"),
            b.nonTerminal("collectionReferenceExpression"),
        )
        b.rule("propertyReferenceExpression").concatenation(
            b.terminalLiteral("property"),
            b.nonTerminal("rootOrNavigation"),
            b.terminalLiteral("refers-to"),
            b.nonTerminal("typeReferences"),
            b.nonTerminal("fromOpt")
        )
        b.rule("fromOpt").optional(b.nonTerminal("from"))
        b.rule("from").concatenation(
            b.terminalLiteral("from"),
            b.nonTerminal("navigation")
        )
        b.rule("collectionReferenceExpression").concatenation(
            b.terminalLiteral("forall"),
            b.nonTerminal("rootOrNavigation"),
            b.nonTerminal("ofTypeOpt"),
            b.terminalLiteral("{"),
            b.nonTerminal("referenceExpressionList"),
            b.terminalLiteral("}"),
        )

        b.rule("rootOrNavigation").choiceLongestFromConcatenationItem(
            b.nonTerminal("root"),
            b.nonTerminal("navigation")
        )

        b.rule("ofTypeOpt").optional(b.nonTerminal("ofType"))
        b.rule("ofType").concatenation(b.terminalLiteral("of-type"), b.nonTerminal("typeReference"))

        b.rule("typeReferences").separatedList(1, -1, b.terminalLiteral("|"), b.nonTerminal("typeReference"))
        b.rule("typeReference").concatenation(b.nonTerminal("qualifiedName"))

        return b.grammar.grammarRule
    }

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "CrossReferences"
    ) {
        extendsGrammar(AglExpressions.grammar)
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
            lit("scope"); ref("typeReference"); lit("{"); ref("identifiables"); lit("}")
        }
        list("identifiables", 0, -1) { ref("identifiable") }
        concatenation("identifiable") {
            lit("identify"); ref("typeReference"); lit("by"); ref("expression")
        }
        concatenation("references") {
            lit("references"); lit("{"); ref("referenceDefinitions"); lit("}")
        }
        list("referenceDefinitions", 0, -1) { ref("referenceDefinition") }
        concatenation("referenceDefinition") {
            lit("in"); ref("typeReference"); lit("{"); ref("referenceExpressionList"); lit("}")
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
        concatenation("ofType") { lit("of-type"); ref("typeReference") }
        choice("rootOrNavigation") {
            ref("root")
            ref("navigation")
        }
        separatedList("typeReferences", 1, -1) { ref("typeReference"); lit("|") }
        concatenation("typeReference") { ref("qualifiedName") }
    }

    const val grammarStr = """namespace net.akehurst.language.agl.language

grammar References extends Expressions {

    unit = namespace* ;
    namespace = 'namespace' qualifiedName '{' imports declarations '}' ;
    imports = import*;
    declarations = rootIdentifiables scopes references? ;
    rootIdentifiables = identifiable* ;
    scopes = scope* ;
    scope = 'scope' typeReference '{' identifiables '}' ;
    identifiables = identifiable* ;
    identifiable = 'identify' typeReference 'by' expression ;

    references = 'references' '{' referenceDefinitions '}' ;
    referenceDefinitions = referenceDefinition* ;
    referenceDefinition = 'in' typeReference '{' referenceExpression* '}' ;
    referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
    propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? ;
    from = 'from' navigation ;
    collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
    ofType = 'of-type' typeReference ;
    
    rootOrNavigation = root | navigation ;
    
    typeReferences = [typeReference / '|']+ ;
    typeReference = qualifiedName ;

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