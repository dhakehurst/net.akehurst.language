/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import kotlin.test.Test

class test_nestedLists_from_Dot {

    companion object {
        val grammarStr="""
namespace test
grammar Dot  {
    skip leaf WHITESPACE = "\s+" ;

	stmt_list = stmt1 * ;
    stmt1 = stmt  ';'? ;
	stmt = node_stmt | attr_stmt ;

    node_stmt = node_id attr_lists? ;
    node_id = ID port? ;
    port =
        ':' ID (':' compass_pt)?
      | ':' compass_pt
      ;
    leaf compass_pt	= 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_' ;

    attr_stmt = attr_type attr_lists ;
    attr_type = 'graph' | 'node' | 'edge' ;
    attr_lists = attr_list+ ;
    attr_list = '[' a_list ']' ;
    a_list = [ attr / a_list_sep ]* ;
    attr = ID '=' ID ;
    a_list_sep = (';' | ',')? ;

	ID =
	  ALPHABETIC_ID
	| NUMERAL

	;

	leaf ALPHABETIC_ID = "[a-zA-Z_][a-zA-Z_0-9]*" ;
	leaf NUMERAL = "[-+]?([0-9]+([.][0-9]+)?|([.][0-9]+))" ;
}
        """.trimIndent()

        val proc = Agl.processorFromString<Any,Any>(grammarStr).processor!!
    }

    @Test
    fun t() {
        val goal = "stmt_list"
        val sentence =  "graph[a=a ]; node [b=b c=c]; edge[];"
        proc.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
    }

}