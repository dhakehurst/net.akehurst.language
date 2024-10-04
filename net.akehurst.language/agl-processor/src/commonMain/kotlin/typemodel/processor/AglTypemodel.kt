/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.typemodel.processor

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.asm.grammar

object AglTypemodel {

    const val goalRuleName = "unit"

    const val grammarStr = """namespace net.akehurst.language
  grammar Typemodel : Base {
    unit = namespace declaration+ ;
    declaration = primitive | enum | collection | datatype ;
    primitive = 'primitive' IDENTIFIER ;
    enum = 'enum' IDENTIFIER ;
    collection = 'collection' IDENTIFIER '<' typeParameterList '>' ;
    typeParameterList = [ IDENTIFIER / ',']+ ;
    datatype = 'datatype' IDENTIFIER supertypes? '{' property* '}' ;
    supertypes = ':' [ typeReference / ',']+ ;
    property = characteristic IDENTIFIER ':' typeReference ;
    typeReference = qualifiedName typeArgumentList? '?'?;
    typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    characteristic
       = 'reference-val'    // reference, constructor argument
       | 'reference-var'    // reference mutable property
       | 'composite-val'    // composite, constructor argument
       | 'composite-var'    // composite mutable property
       | 'dis'    // disregard / ignore
       ;

  }"""

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "Typemodel"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)

    }


    const val komposite = """namespace net.akehurst.language.typemodel.api
interface TypeInstance {
    cmp typeArguments
}
interface TypeDeclaration {
    cmp supertypes
}
interface ValueType {
    cmp constructors
}
interface DataType {
    cmp constructors
}
interface UnnamedSupertypeType {
    cmp subtypes
}
interface PropertyDeclaration {
    cmp typeInstance
}
interface ConstructorDeclaration {
    cmp parameters
}
interface MethodDeclaration {
    cmp parameters
}
interface ParameterDeclaration {
    cmp typeInstance
}

namespace net.akehurst.language.typemodel.asm
class TypeNamespaceAbstract {
    cmp ownedUnnamedSupertypeType
    cmp ownedTupleTypes
}
class TypeDeclarationSimpleAbstract {
    cmp propertyByIndex
}
"""

}