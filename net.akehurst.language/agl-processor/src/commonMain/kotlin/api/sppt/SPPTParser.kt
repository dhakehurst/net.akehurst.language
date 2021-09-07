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

package net.akehurst.language.api.sppt


/**
        grammar Tree {
            skip WHITESPACE : "\s+" ;

            tree : node ;
            nodes : node+ ;
            node : branch | leaf | EMPTY ;
            branch : NAME '{'  nodes '}' ;
            leaf : LITERAL | pattern ;
            pattern : PATTERN ':' LITERAL ;
            PATTERN : "\"([^\"\\]|\\.)*\"" ;
            LITERAL : "'([^'\\]|\\.)*'" ;
            EMPTY : '§empty' ;

            NAME : "[a-zA-Z_][a-zA-Z_0-9]*" ;  //same as IDENTIFIER from OGL.ogl
        }
 */
interface SPPTParser {

    fun parse(treeAsString: String, addTree:Boolean=false): SharedPackedParseTree

}