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

package net.akehurst.language.parser.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals


class test_SText_tokensByLine {

    companion object {
        private val grammarStr1 = """
/**
 * Copyright (c) 2010 - 2018 committers of YAKINDU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * 	committers of YAKINDU - initial API and implementation
 *
 */
namespace  org.yakindu.stext

grammar Expressions {

    skip WS = "\s+";
   	skip SINGLE_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
   	skip MULTI_LINE_COMMENT = "//.*?${'$'}" ;

    Expression = AssignmentExpression;
    AssignmentExpression = ConditionalExpression ( AssignmentOperator ConditionalExpression )* ;
    ConditionalExpression = LogicalOrExpression ( '?' LogicalOrExpression ':' LogicalOrExpression )? ;
    LogicalOrExpression  = LogicalAndExpression ( LogicalOrOperator LogicalAndExpression )* ;
    LogicalAndExpression = LogicalNotExpression ( LogicalAndOperator LogicalNotExpression )* ;
    LogicalNotExpression = BitwiseOrExpression | LogicalNotOperator BitwiseOrExpression ;
    BitwiseOrExpression = BitwiseXorExpression ( BitwiseOrOperator BitwiseXorExpression )* ;
    BitwiseXorExpression = BitwiseAndExpression ( BitwiseXOrOperator BitwiseAndExpression )* ;
    BitwiseAndExpression = LogicalRelationExpression ( BitwiseAndOperator LogicalRelationExpression )* ;
    LogicalRelationExpression = ShiftExpression ( RelationalOperator ShiftExpression )* ;
    ShiftExpression = NumericalAddSubtractExpression ( ShiftOperator NumericalAddSubtractExpression )* ;
    NumericalAddSubtractExpression = NumericalMultiplyDivideExpression ( AdditiveOperator NumericalMultiplyDivideExpression )* ;
    NumericalMultiplyDivideExpression = NumericalUnaryExpression ( MultiplicativeOperator NumericalUnaryExpression )* ;
    NumericalUnaryExpression = PostFixUnaryExpression | UnaryOperator PostFixUnaryExpression ;
    PostFixUnaryExpression = TypeCastExpression ( PostFixOperator )* ;
    TypeCastExpression = PrimaryExpression ( 'as' TypeSpecifier)* ;
    PrimaryExpression
        = PrimitiveValueExpression
        | FeatureCall
	    | ParenthesizedExpression
	    ;

    PrimitiveValueExpression = Literal ;
    FeatureCall = ElementReferenceExpression (
       ( '.' | '.@' ) ID (
             '('(Argument (',' Argument)*)?')'
		     |
	         ('[' Expression ']') ('[' Expression ']')*
	         )?
	   )*
	;

    ElementReferenceExpression = ID	('('(Argument (',' Argument)*)?	')' | ('[' Expression ']')  ('[' Expression ']')* )?;

Argument=(ID '=')?  ConditionalExpression ;

ParenthesizedExpression = '(' Expression ')' ;

TypeSpecifier = QID ('<' (TypeSpecifier (',' TypeSpecifier)*)? '>')?;

    Literal
      = BoolLiteral
      | IntLiteral
      | HexLiteral
      | BinaryLiteral
      | DoubleLiteral
      | FloatLiteral
      | StringLiteral
      | NullLiteral
      ;

    BoolLiteral = BOOL;
    IntLiteral = INT;
    DoubleLiteral = DOUBLE;
    FloatLiteral =FLOAT;
    HexLiteral =HEX;
    BinaryLiteral =BINARY;
    StringLiteral =STRING;
    NullLiteral = 'null';

    LogicalAndOperator = '&&';
    LogicalOrOperator ='||';
    LogicalNotOperator = '!';
    BitwiseXOrOperator=	'^';
    BitwiseOrOperator=	'|';
    BitwiseAndOperator=	'&';
    PostFixOperator='++' | '--';
    AssignmentOperator = '=' | '*='	| '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=' ;
    ShiftOperator = '<<' | '>>' ;
    AdditiveOperator = '+' | '-' ;
    MultiplicativeOperator = '*' | '/' | '%';
    UnaryOperator = '+' | '-' | '~';
    RelationalOperator = '<' | '<=' | '>' | '>=' | '==' | '!=' ;

    leaf BOOL = "true|false|yes|no" ;
    leaf HEX  = "0(x|X)[0-9a-fA-F]+" ;
    leaf BINARY  = "0(b|B)[01]+" ;
    DOUBLE = (INT '.' INT) ('e' ('-' | '+') INT)? ('d' | 'D')? ;
    FLOAT = (INT '.' INT) ('e' ('-' | '+') INT)? ('f' | 'F')? ;
    QID = ID ('.' ID)* ;
    leaf INT = "[0-9]+" ;
    leaf ID = "[a-zA-Z_][a-zA-Z_0-9]*" ;
     STRING = DQ_STRING | SQ_STRING ;
    leaf DQ_STRING = "\"(?:\\?.)*?\"";
    leaf SQ_STRING = "'(?:\\?.)*?'" ;
}
        """.trimIndent()
        private val grammarStr2 = """
/**
 * Copyright (c) 2010 - 2018 committers of YAKINDU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * 	committers of YAKINDU - initial API and implementation
 *
 */
namespace org.yakindu.stext

grammar SText extends Expressions {

// ---- root rules ----
//These root rules are not relevant for the grammar integration in the statechart. They just integrate the different start rules consistently
//into a single grammar.
//

Root = DefRoot*;

DefRoot = StatechartRoot | StateRoot | TransitionRoot ;
StatechartRoot = '@@statechart@@' StatechartSpecification ;
StateRoot = '@@state@@' StateSpecification ;

TransitionRoot = '@@transition@@' TransitionSpecification ;

// ---- start rules ----
// Define the starting points used by the statechart integration. These rules hook in the concrete rules of the specific grammar.
//
StatechartSpecification = ('namespace' FQN)? Annotation* StatechartScope* ;
StateSpecification = StateScope;
TransitionSpecification = TransitionReaction ;
StateScope = LocalReaction* ;
StatechartScope = InterfaceScope | InternalScope | ImportScope ;
InterfaceScope = 'interface' ID? ':' ScopeDeclaration* ;
ScopeDeclaration = Annotation*
	(
		('const' | 'var') ('readonly'?) ID ':'	TypeSpecifier ('=' Expression)?
	|
		Direction? 'event' ID (':' TypeSpecifier)?
	|
		'alias' ID ':' TypeSpecifier
	|
		'operation' ID '(' (Parameter (',' Parameter)*)? ')' (':' TypeSpecifier)?
	);

InternalScope = 'internal' ':' (ScopeDeclaration | LocalReaction)*;
ImportScope = 'import' ':' STRING* ;

Direction = 'in' | 'out' ;
Annotation = '@' QID ('(' (ConditionalExpression (',' ConditionalExpression)*)? ')')?;

Parameter = ID ('...')? ':' TypeSpecifier;

LocalReaction = ReactionTrigger ('/' ReactionEffect);

TransitionReaction = StextTrigger? ('/' ReactionEffect)? ('#' TransitionProperty*)?;

StextTrigger = ReactionTrigger | DefaultTrigger;

ReactionTrigger = ((EventSpec ("," EventSpec)* ('['Guard ']')?) | ('[' Guard ']'));

DefaultTrigger = ('default' | 'else');

Guard = Expression;

ReactionEffect = (Expression | EventRaisingExpression) ( ';' (Expression | EventRaisingExpression))*;

TransitionProperty  = EntryPointSpec | ExitPointSpec;

EntryPointSpec = '>' ID;

ExitPointSpec = ID '>';

EventSpec =	RegularEventSpec | TimeEventSpec | BuiltinEventSpec;

// Use SimpleFeatureCall for eventSpec to avoid guard ambiguity with array access
RegularEventSpec = SimpleFeatureCall;
SimpleFeatureCall = SimpleElementReferenceExpression (('.' | '.@') ID ('(' (Argument (',' Argument)*)? ')')?)*;
SimpleElementReferenceExpression = ID ('(' (Argument (',' Argument)*)? ')')?;
TimeEventSpec = TimeEventType ConditionalExpression TimeUnit;
TimeEventType =	'after' | 'every';
BuiltinEventSpec = EntryEvent | ExitEvent | AlwaysEvent;
EntryEvent= 'entry';
ExitEvent= 'exit';
AlwaysEvent= 'always' | 'oncycle' ;

EventRaisingExpression = 'raise' FeatureCall (':' Expression)?;
EventValueReferenceExpression = 'valueof' '(' FeatureCall ')';
ActiveStateReferenceExpression = 'active' '(' FQN ')';

PrimaryExpression
    = PrimitiveValueExpression
    | FeatureCall
    | ActiveStateReferenceExpression
    | EventValueReferenceExpression
    | ParenthesizedExpression
    ;

TimeUnit = 's' | 'ms' | 'us' | 'ns' ;


FQN = ID ('.' ID)*;

}
        """.trimIndent()

        val exprProcessor = Agl.processor(grammarStr1)
        var processor: LanguageProcessor = Agl.processor(grammarStr2)
    }

    @Test
    fun t1() {
        val sppt = processor.parse("StateScope", "after 10 s / raise ABC.intEvent")
        val actual = sppt.tokensByLine

        assertEquals("after", actual[0][0].matchedText)
    }

}