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

package net.akehurst.language.regexp.processor

import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObject
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.regex.agl.RegexParser
import net.akehurst.language.regex.api.Regex
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.types.api.TypesDomain

object AglRegex : LanguageObjectAbstract<Regex, SentenceContextAny>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Regex"

    override val identity: LanguageIdentity = LanguageIdentity("${NAMESPACE_NAME}.$NAME")

    override val extends by lazy { emptyList<LanguageObject<Any, SentenceContextAny>>() }

    override val grammarString = """
        namespace ${AglGrammar.NAMESPACE_NAME}
          grammar ${AglGrammar.NAME} {
            regex = [concatenation / '|']+ ;
            concatenation = expression+ ;
            expression = atom quantifier? ;
            atom
              = literal
              | escapedChar
              | predefinedCharClass
              | characterClass
              | group
              | boundaryMatcher
              | backreference
              | DOT
            ;
            literal = "[^.\+*?[](){}^$|]" ;
            escapedChar = '\' escapeSequence ;
            escapeSequence
              = DIGIT
              | METACHAR
              | COMMON_ESCAPED
              | hexadecimal
              | unicode
              | CONTROL
              ;
            hexadecimal = 'x' HEX_DIGIT HEX_DIGIT ;
            unicode = 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT ;
            control = 'c' CONTROL_CHAR ;
            
            predefinedCharClass
              = '\d' | '\D'               // Digit, Non-digit
              | '\s' | '\S'               // Whitespace, Non-whitespace
              | '\w' | '\W'               // Word character, Non-word character
              | '\h' | '\H'               // Horizontal whitespace, Non-horizontal whitespace (Java 8+)
              | '\v' | '\V'               // Vertical whitespace, Non-vertical whitespace (Java 8+)
              | '\p{' UnicodeProperty '}' // Unicode character property
              | '\P{' UnicodeProperty '}' // Negated Unicode character property
            ;
            
            characterClass = '[' '^'? charClassExpr* ']' ;
            charClassExpr
                = charClassAtom
                | charClassAtom '-' charClassAtom             // Range [a-z]
                | charClassExpr '&&' '[' charClassContent ']' // Intersection [a-z&&[^bc]]
                ;
            charClassContent = '^'? charClassExpr* ; // Content within a nested character class for intersection
            charClassAtom
                = LiteralInCharClass
                | EscapedChar
                | PredefinedCharClass
                ;
            LiteralInCharClass = <Any character except ], \, ^ (at start), - (in range), & (for intersection)> ;


            group =
                '(' alternation ')'      // Capturing group
                | '(?:' alternation ')'  // Non-capturing group
                | '(?i:' alternation ')' // Flags within groups, e.g., (?i:...) for case-insensitive
                | '(?=' alternation ')'  // Positive lookahead
                | '(?!' alternation ')'  // Negative lookahead
                | '(?<=' alternation ')' // Positive lookbehind
                | '(?<!' alternation ')' // Negative lookbehind
                | '(?>' alternation ')'  // Possessive group
                ;
    
            BoundaryMatcher =
                '^'    // Beginning of a line *)
                | '$'  // End of a line *)
                | '\b' //   Word boundary *)
                | '\B' // Non-word boundary *)
                | '\A' // Beginning of the input *)
                | '\Z' // End of the input but for final terminator *)
                | '\z' // End of the input *)
                | '\G' // End of the previous match *)
                ;
    
            backreference = '\' NON_ZERO_DIGIT ;
    
            Quantifier
                = '*'                       // Zero or more ('?' | '+')? 
                | '+'                       // One or more [ '?' | '+' ]
                | '?'                       // Zero or one [ '?' | '+' ]
                | '{' DIGIT+ '}'            // Exactly n times: {n} *) [ '?' | '+' ]
                | '{' DIGIT+ ',' '}'        // At least n times: {n,} *) [ '?' | '+' ]
                | '{' DIGIT+ ',' DIGIT+ '}' // Between n and m times: {n,m} *) [ '?' | '+' ]
                ;
    
            leaf DOT = '.' ;
            leaf DIGIT = "[0-9]" ;
            leaf NON_ZERO_DIGIT = "[1-9]" ;
            leaf METACHAR = "[.\+*?[](){}^$|]" ;
            leaf COMMON_ESCAPED = "t|n|r|f|a|e" ;
            leaf HEX_DIGIT = "[0-9a-fA-F]" ;
            leaf CONTROL_CHAR = "[a-z@[\]^_]" ;
          }
        """.trimIndent()

    override val typesString: String = """
        namespace ${NAMESPACE_NAME}.style.api
          // TODO
    """.trimIndent()

    override val kompositeString: String = """
        namespace ${NAMESPACE_NAME}.style.api
          // TODO
    """.trimIndent()

    override val asmTransformString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace ${NAMESPACE_NAME}
            // TODO
    """.trimIndent()

    override val styleString = """
        namespace ${NAMESPACE_NAME}
            // TODO
    """.trimIndent()

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val grammarDomain: GrammarDomain
        get() = TODO("not implemented")

    override val typesDomain: TypesDomain
        get() = TODO("not implemented")

    override val asmTransformDomain: AsmTransformDomain
        get() = TODO("not implemented")

    override val crossReferenceDomain: CrossReferenceDomain
        get() = TODO("not implemented")

    override val styleDomain: AglStyleDomain
        get() = TODO("not implemented")

    override val formatDomain: AglFormatDomain
        get() = TODO("not implemented")

    override val defaultTargetGrammar: Grammar
        get() = TODO("not implemented")

    override val defaultTargetGoalRule: String
        get() = TODO("not implemented")

    override val completionProvider: CompletionProvider<Regex, SentenceContextAny>?
        get() = TODO("not implemented")


    val parser by lazy { RegexParser() }
}