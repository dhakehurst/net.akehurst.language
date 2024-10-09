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

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.builder.grammar

object AglStyle {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammar.OPTION_defaultGoalRule, "rules"))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("rules")!!

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "Style"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)

        concatenation("unit") {
            ref("namespace"); lst(1, -1) { ref("styleSet") }
        }
        concatenation("styleSet") {
            lit("styles"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
              lst(0,-1) { ref("rule") }
            lit("}")
        }
        concatenation("extends") {
            lit(":"); spLst(1, -1) { ref("qualifiedName"); lit(",") }
        }
        concatenation("rule") {
            ref("selectorExpression"); lit("{"); lst(0, -1) { ref("style") }; lit("}")
        }
        choice("selectorExpression") {
            ref("selectorAndComposition")
            ref("selectorSingle")
        }
        separatedList("selectorAndComposition", 2, -1) { ref("selectorSingle"); lit(",") }
        choice("selectorSingle") {
            ref("LITERAL")
            ref("PATTERN")
            ref("IDENTIFIER")
            ref("META_IDENTIFIER")
        }
        // these must match what is in the AglGrammarGrammar
        concatenation("LITERAL", isLeaf = true) { pat("'([^'\\\\]|\\\\.)*'") }
        concatenation("PATTERN", isLeaf = true) { pat("\"([^\"\\\\]|\\\\.)*\"") }

        concatenation("META_IDENTIFIER", isLeaf = true) { pat("[\\$][a-zA-Z_][a-zA-Z_0-9-]*") }

        concatenation("style") {
            ref("STYLE_ID"); lit(":"); ref("styleValue"); lit(";")
        }
        choice("styleValue") {
            ref("STYLE_ID")
            ref("STRING")
        }
        concatenation("STYLE_ID", isLeaf = true) { pat("[-a-zA-Z_][-a-zA-Z_0-9]*") }
        concatenation("STYLE_VALUE", isLeaf = true) { pat("[^;: \\t\\n\\x0B\\f\\r]+") }
        concatenation("STRING", isLeaf = true) { pat("'([^'\\\\]|\\\\'|\\\\\\\\)*'") }
    }


    const val styleStr = """namespace net.akehurst.language
styles Style {
    META_IDENTIFIER {
      foreground: orange;
      font-style: bold;
    }
    IDENTIFIER {
      foreground: blue;
      font-style: bold;
    }
    LITERAL {
      foreground: blue;
      font-style: bold;
    }
    PATTERN {
      foreground: darkblue;
      font-style: bold;
    }
    STYLE_ID {
      foreground: darkred;
      font-style: italic;
    }
}"""

    const val scopeModelStr = """
references {
    in scope property typeReference refers-to GrammarRule
    in identifiable property typeReference refers-to GrammarRule
    in referenceDefinition property typeReference refers-to GrammarRule
    in referenceDefinition property propertyReference refers-to GrammarRule
}
    """

    //TODO: gen this from the ASM
    override fun toString(): String = """
namespace net.akehurst.language.agl.language

grammar Style : Base {

    unit = namespace styleSet+ ;
    styleSet = 'styles' IDENTIFIER extends? '{' rule* '}' ;
    extends = ':' [qualifiedName / ',']+ ;
    rule = selectorExpression '{' styleList '}' ;
    selectorExpression
     = selectorAndComposition
     | selectorSingle
     ; //TODO
    selectorAndComposition = [selectorSingle /',']2+ ;
    selectorSingle = LITERAL | PATTERN | IDENTIFIER | META_IDENTIFIER ;
    styleList = style* ;
    style = STYLE_ID ':' styleValue ';' ;
    styleValue = STYLE_VALUE | STRING ;
    
    leaf LITERAL = "'([^'\\]|\\.)+'" ;
    leaf PATTERN = "\"([^\"\\]|\\.)+\"" ;
    leaf META_IDENTIFIER = "[\\${'$'}][a-zA-Z_][a-zA-Z_0-9-]*" ;
    leaf STYLE_ID = "[-a-zA-Z_][-a-zA-Z_0-9]*" ;
    leaf STYLE_VALUE = "[^;: \t\n\x0B\f\r]+" ;
    leaf STRING = "'([^'\\]|\\'|\\\\)*'" ;
}
    """.trimIndent()
}


