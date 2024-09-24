/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlinx.komposite.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.TypeModel

object Komposite {

    private var _processor: LanguageProcessor<TypeModel,Any>? = null

    internal fun processor(): LanguageProcessor<TypeModel,Any> {
        if (null == _processor) {
            val grammarStr = fetchGrammarStr()
            val res = Agl.processorFromString<TypeModel,Any>(
                grammarDefinitionStr = grammarStr,
                configuration = Agl.configuration {
                    defaultGoalRuleName("model")
                    syntaxAnalyserResolver { ProcessResultDefault(KompositeSyntaxAnalyser2(),IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(KompositeSemanticAnalyser(),IssueHolder(LanguageProcessorPhase.ALL)) }
                //formatter(Formatter())
                }
            )
            //proc.buildFor() //build for default goal, see above
            _processor = res.processor
        }
        return _processor!!
    }

    internal fun fetchGrammarStr(): String {
        return """
            namespace net.akehurst.kotlin.komposite
            
            grammar Composite {
                skip WHITE_SPACE = "\s+" ;
                skip COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
                     MULTI_LINE_COMMENT = "[/][*](?:.|\n)*?[*][/]" ;
                     SINGLE_LINE_COMMENT = "//[^\n\r]*" ;
            
                model = namespace* ;
                namespace = 'namespace' qualifiedName '{' import* declaration* '}' ;
                qualifiedName = [ NAME / '.']+ ;
                import = 'import' qualifiedName ;
                declaration = primitive | enum | collection | datatype ;
                primitive = 'primitive' NAME ;
                enum = 'enum' NAME ;
                collection = 'collection' NAME '<' typeParameterList '>' ;
                typeParameterList = [ NAME / ',']+ ;
                datatype = 'datatype' NAME supertypes? '{' property* '}' ;
                supertypes = ':' [ typeReference / ',']+ ;
                property = characteristic NAME ':' typeReference ;
                typeReference = qualifiedName typeArgumentList? '?'?;
                typeArgumentList = '<' [ typeReference / ',']+ '>' ;
                characteristic
                   = 'reference-val'    // reference, constructor argument
                   | 'reference-var'    // reference mutable property
                   | 'composite-val'    // composite, constructor argument
                   | 'composite-var'    // composite mutable property
                   | 'dis'    // disregard / ignore
                   ;
            
                leaf NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
                leaf POSITIVE_INTEGER = "[0-9]+" ;
            }
            
            """.trimIndent()
    }

    fun process(datatypeModel: String) = this.processor().process(datatypeModel)

}