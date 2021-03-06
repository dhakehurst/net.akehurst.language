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

package net.akehurst.language.examples.simple

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.js.JsName

object SimpleExample {

    val grammarStr = """
        namespace net.akehurst.language.examples.simple
        grammar SimpleExample {
        
            skip WHITE_SPACE = "\s+" ;
	        skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
	        skip SINGLE_LINE_COMMENT = "//.*?${'$'}" ;

            unit = definition* ;
            definition = classDefinition ;
            classDefinition =
                'class' NAME '{'
                    propertyDefinition*
                    methodDefinition*
                '}'
            ;
            
            propertyDefinition = NAME ':' NAME ;
            methodDefinition = NAME '(' parameterList ')' body ;
            parameterList = [ parameterDefinition / ',']* ;
            parameterDefinition = NAME ':' NAME ;
            body = '{' '}' ;
 
            NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            BOOLEAN = "true | false" ;
            NUMBER = "[0-9]+([.][0-9]+)?" ;
            STRING = "'(?:\\?.)*?'" ;
        }
    """

    @JsName("processor")
    val processor: LanguageProcessor = Agl.processor(
            grammarStr,
            SimpleExampleSyntaxAnalyser(),
            SimpleExampleFormatter()
    )

}
