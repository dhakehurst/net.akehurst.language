/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package thridparty.projectIT

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_MetaTyperLanguage {

    private companion object {
        val grammarStr = """
            namespace MetaTyperLanguage
            grammar MetaTyperGrammar {
                            
            // rules for "PiTyperDef"
            PiTyperDef = 'typer'
                 ( 'istype' '{' [ __pi_reference / ',' ]* '}' )?
                 ( 'hastype' '{' [ __pi_reference / ',' ]* '}' )?
                 PitAnyTypeRule?
                 PitClassifierRule* ;
            
            PitAnyTypeRule = 'anytype' '{'
                 PitSingleRule*
                 '}' ;
            
            PitSingleRule = PitStatementKind PitExp ';' ;
            
            PitPropertyCallExp = (PitExp '.')? __pi_reference ;
            
            PitExpWithType = '(' PitExp 'as' __pi_reference ')' ;
            
            PitSelfExp = 'self' ;
            
            PitAnytypeExp = 'anytype' ;
            
            PitInstanceExp = ( __pi_reference ':' )?
                 __pi_reference ;
            
            PitWhereExp = PitProperty 'where' '{'
                 ( __pi_binary_PitExp ';' )*
                 '}' ;
            
            PitFunctionCallExp = identifier '(' [ PitExp / ',' ]* ')' ;
            
            PitConformanceOrEqualsRule = __pi_reference '{'
                 PitSingleRule*
                 '}' ;
            
            PitInferenceRule = __pi_reference '{'
                 'infertype' PitExp ';'
                 '}' ;
            
            PitLimitedRule = __pi_reference '{'
                 ( __pi_binary_PitExp ';' )*
                 '}' ;
            
            PitExp = PitAppliedExp 
                | PitExpWithType 
                | PitSelfExp 
                | PitAnytypeExp 
                | PitWhereExp 
                | PitFunctionCallExp 
                | PitInstanceExp 
                | __pi_binary_PitExp ;
            
            PitAppliedExp = PitPropertyCallExp  ;
            
            PitClassifierRule = PitConformanceOrEqualsRule 
                | PitInferenceRule 
                | PitLimitedRule  ;
            
            __pi_binary_PitExp = [PitExp / __pi_binary_operator]2+ ;
            leaf __pi_binary_operator = 'conformsto' | 'equalsto' ;
            
            PitStatementKind = 'equalsto'
                | 'conformsto' ;
                
            PitProperty = identifier ':' __pi_reference ;
            
            // common rules   
            
            __pi_reference = [ identifier / '::::' ]+ ;
                    
            // white space and comments
            skip WHITE_SPACE = "\s+" ;
            skip SINGLE_LINE_COMMENT = "//[^\r\n]*" ;
            skip MULTI_LINE_COMMENT = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
                                
            // the predefined basic types   
            leaf identifier          = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            leaf stringLiteral       = "\"([^\"\\\\]|\\.)*\"";
            leaf numberLiteral       = "[0-9]+";
            leaf booleanLiteral      = 'false' | 'true';
                        
            }
        """.trimIndent()
        val processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        const val goal = "PiTyperDef"
    }

    @Test
    fun sentence1() {
        val sentence = """
// FROM defs-old
typer

// What are types?
istype { Type, TypeDeclaration }

// Which concepts have a type?
hastype { Exp }

// What are the top and bottom types?
anytype {
    conformsto PredefinedType:ANY; // PredefinedType:ANY is the least specific type
}

PredefinedType {
    PredefinedType:NULL conformsto anytype; // PredefinedType:NULL is the most specific type
}

// Which type does an expression have?
NumberLiteral {
    infertype PredefinedType:NUMBER;
}

StringLiteral {
    infertype PredefinedType:STRING;
}

BooleanLiteral {
    infertype PredefinedType:BOOLEAN;
}

NamedExp {
    infertype self.myType;
}

PlusExp {
    infertype (commonSuperType(self.left, self. right) as Type);
}

UnitLiteral {
    // 62 kilogram, or 112 miles
    infertype x:UnitOfMeasurement where {
                  x.baseType equalsto (typeof(self.inner) as Type);
                  x.unit equalsto self.unit;
              };
}

GenericLiteral {
    // Set{ 12, 14, 16, 18 }
    infertype x:GenericType where {
        x.innerType equalsto (typeof(self.content) as Type);
        x.kind equalsto self.kind;
    };
}

// Which types are 'equal' to each other?
SimpleType {
    equalsto self.type;
}

NamedType {
    equalsto aa:NamedType where {
            aa.name equalsto self.name;
        };
}

GenericType {
    equalsto x:GenericType where {
            x.innerType equalsto self.innerType;
            x.kind equalsto self.kind;
        };
    conformsto x:GenericType where {
            // both conditions must be true
            self.innerType conformsto x.innerType;
            self.kind conformsto x.kind;
        };
}

GenericKind {
    Set conformsto Collection;
    Sequence conformsto Bag;
    Bag conformsto Collection;
    // Collection;
}

UnitOfMeasurement {
    equalsto aap:UnitOfMeasurement where {
            aap.baseType equalsto self.baseType;
            aap.unit equalsto self.unit;
        };
    conformsto rr:UnitOfMeasurement where {
            self.baseType conformsto rr.baseType;
            self.unit equalsto rr.unit;
        };
}

//OrType { // represents "one of", e.g. string|string[]
//    conformsto rr:OrType where {
//        rr conformsto oneOf(rr.inners)
//    }
//}
//
//anytype {
//    conformsto rr:AndType where {
//        self conformsto allOf(rr.inners)
//    }
//}
//
//AndType { // represents "all of", e.g. Comparable&Serializable
//    conformsto rr:AndType where {
//        rr conformsto allOf(self.inners)
//    }
//}
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }
}