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

package net.akehurst.language.processor.java8

import net.akehurst.language.processor.processor
import kotlin.test.Test

class test_Java8_parts {



    @Test
    fun t1() {
        val grammarStr = """
compilationUnit
	=	packageDeclaration? importDeclaration* typeDeclaration*
	;

packageDeclaration
	=	packageModifier* 'package' packageName ';'
	;
packageModifier
	=	annotation
	;

importDeclaration
	=	singleTypeImportDeclaration
	|	typeImportOnDemandDeclaration
	|	singleStaticImportDeclaration
	|	staticImportOnDemandDeclaration
	;

        """.trimIndent()

        val sentence = "interface An { An[] value(); }"
        val goal = "compliationUnit"

        val p = processor(grammarStr)
        p.parse(goal, sentence)
    }

}