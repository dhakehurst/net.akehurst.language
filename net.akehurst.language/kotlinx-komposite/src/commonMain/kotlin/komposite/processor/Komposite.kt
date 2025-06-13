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

/**
 * Kotlin (JVM & JS) do not provide a mechnism/syntax for indicating which members of an object
 * should be considered 'composite' parts.
 * Primitive values are always reference
 * EnumType values are always reference
 * ValueType values are always composite
 * TupleType values are always composite
 * UnnamedSupertypeType values are always composite
 *
 * but Datatypes values could be either composite or reference
 * thus we need a way to augment the member definitions to indicate which.
 * A common approach to this kind of thing is to use Annotations,
 * but this makes a dependency on whatever provides the annotation.
 * Not possible for third party libs, and not 'nice' in other situations.
 *
 * thus we create an 'augmentation' DSL for defining just those members that are composite
 *
 */
object Komposite {

    private var _processor: LanguageProcessor<TypeModel,Any>? = null

    internal fun processor(): LanguageProcessor<TypeModel,Any> {
        if (null == _processor) {
            val grammarStr = fetchGrammarStr()
            val res = Agl.processorFromString<TypeModel,Any>(
                grammarDefinitionStr = grammarStr,
                configuration = Agl.configuration {
                    defaultGoalRuleName("unit")
                    syntaxAnalyserResolver { ProcessResultDefault(KompositeSyntaxAnalyser2()) }
                    semanticAnalyserResolver { ProcessResultDefault(KompositeSemanticAnalyser()) }
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
            
            grammar Komposite {
                skip WHITE_SPACE = "\s+" ;
                skip COMMENT = MULTI_LINE_COMMENT | SINGLE_LINE_COMMENT ;
                     MULTI_LINE_COMMENT = "[/][*](?:.|\n)*?[*][/]" ;
                     SINGLE_LINE_COMMENT = "//[^\n\r]*" ;
            
                unit = namespace* ;
                namespace = 'namespace' qualifiedName declaration*;
                qualifiedName = [ NAME / '.']+ ;
                declaration = declKind NAME '{' property* '}' ;
                declKind = 'interface' | 'class' ;
                property = characteristic NAME ;
                characteristic
                   = 'ref'    // reference
                   | 'cmp'    // composite
                   | 'dis'    // disregard / ignore
                   ;
            
                leaf NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
                leaf POSITIVE_INTEGER = "[0-9]+" ;
            }
            
            """.trimIndent()
    }

    fun process(datatypeModel: String) = this.processor().process(datatypeModel)

}