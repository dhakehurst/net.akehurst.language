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

package net.akehurst.language.agl.thridparty

import net.akehurst.language.agl.Agl
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_aglJava {

    private companion object {

        val grammarStr = """
/*
 * This may be more permissive than the other ambiguous grammar
 * derived from [https://docs.oracle.com/javase/specs/jls/se8/html/jls-19.html]
 */
namespace net.akehurst.language.java8

grammar Base {
    skip leaf WHITESPACE = "\s+" ;
    skip leaf COMMENT_SINGLE_LINE = "//[^\r\n]*" ;
    skip leaf COMMENT_MULTI_LINE = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;

    leaf IDENTIFIER = JAVA_LETTER JAVA_LETTER_OR_DIGIT* ;
    leaf JAVA_LETTER_OR_DIGIT = JAVA_LETTER | "[0-9]" ;
    leaf JAVA_LETTER= UNICODE_LETTER | '$' | '_' ;
    leaf UNICODE_LETTER = "[A-Za-z]" ; //TODO: add unicode chars !

    QualifiedName = [ IDENTIFIER / '.' ]+ ;

}

grammar Literals {
    Literal
      = INTEGER_LITERAL
      | FLOATING_POINT_LITERAL
      | BOOLEAN_LITERAL
      | CHARACTER_LITERAL
      | STRING_LITERAL
      | NULL_LITERAL
      ;

    leaf INTEGER_LITERAL
      = HEX_NUMERAL
      | OCT_NUMERAL
      | BINARY_NUMERAL
      | DECIMAL_NUMERAL
      ;

    leaf DECIMAL_NUMERAL = "(0|[1-9]([0-9_]*[0-9])?)" INTEGER_TYPE_SUFFIX? ;
    leaf HEX_NUMERAL     = "0[xX][0-9a-fA-F]([0-9a-fA-F_]*[0-9a-fA-F])?" INTEGER_TYPE_SUFFIX? ;
    leaf OCT_NUMERAL     = "0_*[0-7]([0-7_]*[0-7])?" INTEGER_TYPE_SUFFIX? ;
    leaf BINARY_NUMERAL  = "0[bB][01]([01_]*[01])?" INTEGER_TYPE_SUFFIX? ;

    leaf INTEGER_TYPE_SUFFIX = 'l' | 'L' ;

    leaf FLOATING_POINT_LITERAL
     = "[0-9]+" "[fdFD]"
     | "[0-9]+[eE][+-]?[0-9]+" "[fdFD]"?
     | "[0-9]*[.][0-9]*" "[eE][+-]?(0|[1-9])+"? "[fdFD]"?
     ;

    leaf BOOLEAN_LITERAL   = 'true' | 'false' ;
    leaf CHARACTER_LITERAL = "'" ("[^'\r\n\\\\]" | ESCAPE_SEQUENCE) "'" ;
    leaf ESCAPE_SEQUENCE
        = '\\' "[btnfr\x27\\\\]"
        | '\\' ("[0-3]"? "[0-7]")? "[0-7]"
        | '\\' 'u'+ "[0-9a-fA-F]{4}"
        ;
    leaf STRING_LITERAL    = "\"([^\"\\\\]|\\.)*\"" ;
    leaf NULL_LITERAL      = 'null' ;
}

grammar Annotations : Base, Literals {
    Annotation = NormalAnnotation | MarkerAnnotation | SingleElementAnnotation ;
    NormalAnnotation = '@' QualifiedName '(' ElementValuePairList ')' ;
    ElementValuePairList = [ ElementValuePair / ',' ]* ;
    ElementValuePair = IDENTIFIER '=' ElementValue ;
    // overridden in expressions
    ElementValue = Literal | ElementValueArrayInitializer | Annotation ;
    ElementValueArrayInitializer = '{' ElementValueList ','? '}' ;
    ElementValueList = [ ElementValue / ',' ]* ;
    MarkerAnnotation = '@' QualifiedName ;
    SingleElementAnnotation = '@' QualifiedName '(' ElementValue ')' ;
}

grammar Types : Annotations {
    Type = Annotation* UnannType ;
    PrimitiveType = Annotation* UnannPrimitiveType ;
    leaf UnannPrimitiveType = NumericType | 'boolean' ;
    leaf NumericType = IntegralType | FloatingPointType ;
    leaf IntegralType = 'byte' | 'short' | 'int' | 'long' | 'char' ;
    leaf FloatingPointType = 'float' | 'double' ;

    ReferenceType = QualifiedTypeReference | ArrayType ;
    QualifiedTypeReference = [ TypeReference / '.' ]+ ;
    TypeReference = Annotation* UnannTypeReference ;
    ArrayType = (PrimitiveType | QualifiedTypeReference) Dims ;
    Dims = (Annotation* '[' ']')+ ;

    TypeParameter = Annotation* IDENTIFIER TypeBound? ;
    TypeBound = 'extends' QualifiedTypeReference AdditionalBound? ;
    AdditionalBound = '&' QualifiedTypeReference ;

    TypeArguments = '<' TypeArgumentList '>' ;
    TypeArgumentList = [ TypeArgument / ',']* ;
    TypeArgument = ReferenceType | Wildcard ;
    Wildcard = Annotation* '?' WildcardBounds? ;
    WildcardBounds = 'extends' ReferenceType | 'super' ReferenceType ;

    UnannType = UnannTypeNonArray Dims? ;
    UnannTypeNonArray = UnannReferenceType < UnannPrimitiveType ;
    UnannReferenceType = UnannQualifiedTypeReference ;
    UnannQualifiedTypeReference = UnannTypeReference ( '.' [ TypeReference / '.' ]+ )? ;
    UnannTypeReference = IDENTIFIER TypeArguments? ;
}

grammar Expressions : Types {

    // from Annotations
    override ElementValue = Expression | ElementValueArrayInitializer | Annotation ;

    Expression
     = LambdaExpression
     | Assignment
     | TernaryIf
     | Infix
     | Prefix
     | Postfix
     | CastExpression
     | Navigations
     ;

    Navigations = Primary ('.' [ NavigableExpression  / '.' ]+)? ;
    NavigableExpression
      = IDENTIFIER
      | MethodReference
      | GenericMethodInvocation
      | ArrayAccess
      | 'this'
      | 'super'
      ;

    Primary = PrimaryNoNewArray | ArrayCreationExpression ;

    PrimaryNoNewArray
      = IDENTIFIER
      | Literal
      | MethodReference
      | MethodInvocation
      | ArrayAccess
      | ClassLiteral
      | 'this'
      | 'super'
      | GroupedExpression
      | ClassInstanceCreationExpression
      ;

    MethodReference
      = QualifiedName '::' TypeArguments? IDENTIFIER
      | ReferenceType '::' TypeArguments? IDENTIFIER
      | Primary '::' TypeArguments? IDENTIFIER
      | 'super' '::' TypeArguments? IDENTIFIER
      | QualifiedName '.' 'super' '::' TypeArguments? IDENTIFIER
      | QualifiedTypeReference '::' TypeArguments? 'new'
      | ArrayType '::' 'new'
      ;

    GenericMethodInvocation = TypeArguments? MethodInvocation ;
    MethodInvocation = IDENTIFIER ArgumentList ;
    ArgumentList = '(' Arguments ')' ;
    Arguments = [ Expression / ',' ]* ;

    ClassLiteral
      = QualifiedName ('[' ']')* '.' 'class'
      | NumericType ('[' ']')* '.' 'class'
      | 'boolean' ('[' ']')* '.' 'class'
      | 'void' '.' 'class'
      ;

    GroupedExpression = '(' Expression ')' ;

    ClassInstanceCreationExpression
      = UnqualifiedClassInstanceCreationExpression
      | QualifiedName '.' UnqualifiedClassInstanceCreationExpression
      | Primary '.' UnqualifiedClassInstanceCreationExpression
      ;
    UnqualifiedClassInstanceCreationExpression
      = 'new' TypeArguments? ClassOrInterfaceTypeToInstantiate  ArgumentList ClassBody? ;
    // overridden in Classes
    ClassBody = '{' '}' ;
    ClassOrInterfaceTypeToInstantiate = Annotation* IDENTIFIER ('.' Annotation* IDENTIFIER)* TypeArgumentsOrDiamond? ;


    TernaryIf = Expression '?' Expression ':' Expression ;

    Infix = [ Expression / INFIX_OPERATOR ]2+ ;
    leaf INFIX_OPERATOR
      = '%' | '/' | '*' | '+' | '-'
      | '<<' | '>>>' | '>>'
      | '<=' | '>=' | '<' | '>' | 'instanceof'
      | '==' | '!='
      | '&&' | '||' | '&' | '|' | '^'
      ;

    Assignment = [ Expression / ASSIGNMENT_OPERATOR ]2+ ;
    leaf ASSIGNMENT_OPERATOR
      = '=' | '+=' | '-=' | '*=' | '/=' | '%='
      | '&=' | '|=' | '^='
      | '>>=' | '>>>=' | '<<='
      ;

    Prefix = PREFIX_OPERATOR Expression ;
    leaf PREFIX_OPERATOR = '++' | '--' | '+' | '-' | '!' | '~' ;
    Postfix = Expression POSTFIX_OPERATOR ;
    leaf POSTFIX_OPERATOR = '++' | '--' ;
    CastExpression
          = '(' PrimitiveType ')' Expression
          | '(' ReferenceType AdditionalBound* ')' Expression
          ;

    TypeArgumentsOrDiamond = TypeArguments | '<>' ;

    ArrayAccess = Expression '[' Expression ']' ;

    ArrayCreationExpression
      = 'new' PrimitiveType DimExprs Dims?
      | 'new' QualifiedTypeReference DimExprs Dims?
      | 'new' PrimitiveType Dims ArrayInitializer
      | 'new' QualifiedTypeReference Dims ArrayInitializer
      ;

    ArrayInitializer = '{' VariableInitializerList ','? '}' ;
    VariableInitializerList = [ VariableInitializer / ',' ]* ;
    VariableInitializer = Expression | ArrayInitializer ;

    DimExprs = DimExpr DimExpr* ;
    DimExpr = Annotation* '[' Expression ']' ;

    LambdaExpression = LambdaParameters '->' LambdaBody ;
    LambdaParameters
      = IDENTIFIER
      | '(' FormalParameterList? ')'
      | '(' InferredFormalParameterList ')'
      ;
    InferredFormalParameterList = [IDENTIFIER / ',']+ ;

    FormalParameterList = ReceiverParameter? FormalParameters VarargsParameter? ;
    FormalParameters = [FormalParameter / ',']* ;
    FormalParameter = VariableModifier* UnannType VariableDeclaratorId ;
    VarargsParameter = VariableModifier* UnannType Annotation* '...' VariableDeclaratorId ;
    ReceiverParameter = Annotation* UnannType (IDENTIFIER '.')? 'this' ;

    VariableModifier = Annotation | 'final' ;
    VariableDeclaratorId = IDENTIFIER Dims? ;
    VariableDeclaratorList = [ VariableDeclarator / ',' ]+ ;
    VariableDeclarator = VariableDeclaratorId ('=' VariableInitializer)? ;

    // overridden in BlocksAndStatements
    LambdaBody = Expression ;

    ConstantExpression = Expression ;
}

grammar BlocksAndStatements : Expressions {

    // from Expressions
    override LambdaBody = Expression | Block ;

    Block = '{' BlockStatements '}' ;
    BlockStatements = BlockStatement* ;
    // overridden in classes
    BlockStatement = LocalVariableDeclarationStatement | Statement ;
    LocalVariableDeclarationStatement = LocalVariableDeclaration ';' ;
    LocalVariableDeclaration = VariableModifier* UnannType VariableDeclaratorList ;
    Statement
      = StatementWithoutTrailingSubstatement
      | LabeledStatement
      | IfThenStatement
      | IfThenElseStatement
      | WhileStatement
      | ForStatement
      ;
    StatementNoShortIf
      = StatementWithoutTrailingSubstatement
      | LabeledStatementNoShortIf
      | IfThenElseStatementNoShortIf
      | WhileStatementNoShortIf
      | ForStatementNoShortIf
      ;
    StatementWithoutTrailingSubstatement
      = Block
      | EmptyStatement
      | ExpressionStatement
      | AssertStatement
      | SwitchStatement
      | DoStatement
      | BreakStatement
      | ContinueStatement
      | ReturnStatement
      | SynchronizedStatement
      | ThrowStatement
      | TryStatement
      ;
    EmptyStatement = ';' ;
    LabeledStatement = IDENTIFIER ':' Statement ;
    LabeledStatementNoShortIf = IDENTIFIER ':' StatementNoShortIf ;
    ExpressionStatement = StatementExpression ';' ;
    StatementExpression
      = Expression
      ;
    IfThenStatement = 'if' '(' Expression ')' Statement ;
    IfThenElseStatement = 'if' '(' Expression ')' StatementNoShortIf 'else' Statement ;
    IfThenElseStatementNoShortIf = 'if' '(' Expression ')' StatementNoShortIf 'else' StatementNoShortIf ;
    AssertStatement
     = 'assert' Expression (':' Expression)? ';'
     ;
    SwitchStatement = 'switch' '(' Expression ')' SwitchBlock ;
    SwitchBlock = '{' SwitchBlockStatementGroup* SwitchLabel* '}' ;
    SwitchBlockStatementGroup = SwitchLabels BlockStatements ;
    SwitchLabels = SwitchLabel+ ;
    SwitchLabel
      = 'case' ConstantExpression ':'
      | 'case' EnumConstantName ':'
      | 'default' ':'
      ;
    EnumConstantName = IDENTIFIER ;
    WhileStatement = 'while' '(' Expression ')' Statement ;
    WhileStatementNoShortIf = 'while' '(' Expression ')' StatementNoShortIf ;
    DoStatement = 'do' Statement 'while' '(' Expression ')' ';' ;
    ForStatement = BasicForStatement | EnhancedForStatement ;
    ForStatementNoShortIf = BasicForStatementNoShortIf | EnhancedForStatementNoShortIf ;
    BasicForStatement = 'for' '(' ForInit? ';' Expression? ';' ForUpdate? ')' Statement ;
    BasicForStatementNoShortIf = 'for' '(' ForInit? ';' Expression? ';' ForUpdate? ')' StatementNoShortIf ;
    ForInit = StatementExpressionList | LocalVariableDeclaration ;
    ForUpdate = StatementExpressionList ;
    StatementExpressionList = [StatementExpression /',' ]+ ;
    EnhancedForStatement = 'for' '(' VariableModifier* UnannType VariableDeclaratorId ':' Expression ')' Statement ;
    EnhancedForStatementNoShortIf = 'for' '(' VariableModifier* UnannType VariableDeclaratorId ':' Expression ')' StatementNoShortIf ;
    BreakStatement = 'break' IDENTIFIER? ';' ;
    ContinueStatement = 'continue' IDENTIFIER? ';' ;
    ReturnStatement = 'return' Expression? ';' ;
    ThrowStatement = 'throw' Expression ';' ;
    SynchronizedStatement = 'synchronized' '(' Expression ')' Block ;
    TryStatement
      = 'try' Block Catches
      | 'try' Block Catches? Finally
      | TryWithResourcesStatement
      ;
    Catches = CatchClause CatchClause* ;
    CatchClause = 'catch' '(' CatchFormalParameter ')' Block ;
    CatchFormalParameter = VariableModifier* CatchType VariableDeclaratorId ;
    CatchType = UnannQualifiedTypeReference ('|' QualifiedTypeReference)* ;
    Finally = 'finally' Block ;
    TryWithResourcesStatement = 'try' ResourceSpecification Block Catches? Finally? ;
    ResourceSpecification = '(' ResourceList ';'? ')' ;
    ResourceList = Resource (';' Resource)* ;
    Resource = VariableModifier* UnannType VariableDeclaratorId '=' Expression ;
}

grammar Classes : BlocksAndStatements {

    // from BlocksAndStatements
    override BlockStatement = LocalVariableDeclarationStatement | ClassDeclaration | Statement ;
    // from Expressions
    override ClassBody = '{' ClassBodyDeclaration* '}' ;

    ClassDeclaration = NormalClassDeclaration | EnumDeclaration ;
    NormalClassDeclaration = ClassModifier* 'class' IDENTIFIER TypeParameters? Superclass? Superinterfaces? ClassBody ;
    ClassModifier = Annotation | CLASS_MODIFIER ;
    leaf CLASS_MODIFIER = 'public' | 'protected' | 'private' | 'abstract' | 'static' | 'final' | 'strictfp' ;
    TypeParameters = '<' TypeParameterList '>' ;
    TypeParameterList = [ TypeParameter / ',' ]+ ;
    Superclass = 'extends' QualifiedTypeReference ;
    Superinterfaces = 'implements' InterfaceTypeList ;
    InterfaceTypeList = [ QualifiedTypeReference / ',' ]+ ;

    ClassBodyDeclaration
       = ClassMemberDeclaration
       | InstanceInitializer
       | StaticInitializer
       | ConstructorDeclaration
       ;
    // overridden in Interfaces
    ClassMemberDeclaration
       = FieldDeclaration
       | MethodDeclaration
       | ClassDeclaration
       | ';'
       ;
    FieldDeclaration = FieldModifier* UnannType VariableDeclaratorList ';' ;
    FieldModifier = Annotation | FIELD_MODIFIER ;
    leaf FIELD_MODIFIER = 'public' | 'protected' | 'private' | 'static' | 'final' | 'transient' | 'volatile' ;

    MethodDeclaration = MethodModifier* MethodHeader MethodBody ;
    MethodModifier  = Annotation | METHOD_MODIFIER ;
    leaf METHOD_MODIFIER = 'public' | 'protected' | 'private' | 'abstract'
      | 'static' | 'final' | 'synchronized' | 'native' | 'strictfp'
      ;
    MethodHeader
      = Result MethodDeclarator Throws?
      | TypeParameters Annotation* Result MethodDeclarator Throws?
      ;
    Result = UnannType | 'void' ;
    MethodDeclarator = IDENTIFIER '(' FormalParameterList? ')' Dims? ;

    Throws = 'throws' ExceptionTypeList ;
    ExceptionTypeList = [ ExceptionType / ',' ]+ ;
    ExceptionType = QualifiedTypeReference ;
    MethodBody = Block | ';' ;
    InstanceInitializer = Block ;
    StaticInitializer = 'static' Block ;
    ConstructorDeclaration = ConstructorModifier* ConstructorDeclarator Throws? ConstructorBody ;
    ConstructorModifier = Annotation | CONSTRUCTOR_MODIFIER ;
    leaf CONSTRUCTOR_MODIFIER = 'public' | 'protected' | 'private' ;
    ConstructorDeclarator = TypeParameters? SimpleTypeName '(' FormalParameterList? ')' ;
    SimpleTypeName = IDENTIFIER ;
    ConstructorBody = '{' ExplicitConstructorInvocation? BlockStatements? '}' ;
    ExplicitConstructorInvocation
      = TypeArguments? 'this'  ArgumentList  ';'
      | TypeArguments? 'super'  ArgumentList  ';'
      | QualifiedName '.' TypeArguments? 'super'  ArgumentList  ';'
      | Primary '.' TypeArguments? 'super'  ArgumentList  ';'
      ;
    EnumDeclaration = ClassModifier* 'enum' IDENTIFIER Superinterfaces? EnumBody ;
    EnumBody = '{' EnumConstantList? ','? EnumBodyDeclarations? '}' ;
    EnumConstantList = [ EnumConstant / ',' ]+ ;
    EnumConstant = EnumConstantModifier* IDENTIFIER ArgumentList? ClassBody? ;
    EnumConstantModifier = Annotation ;
    EnumBodyDeclarations = ';' ClassBodyDeclaration* ;
}

grammar Interfaces : Classes {
    // from Classes
    override ClassMemberDeclaration
                = FieldDeclaration
                | MethodDeclaration
                | ClassDeclaration
                | InterfaceDeclaration
                | ';'
                ;

    InterfaceDeclaration = NormalInterfaceDeclaration | AnnotationTypeDeclaration ;
    NormalInterfaceDeclaration = InterfaceModifier* 'interface' IDENTIFIER TypeParameters? ExtendsInterfaces? InterfaceBody ;
    InterfaceModifier = Annotation | INTERFACE_MODIFIER ;
    leaf INTERFACE_MODIFIER = 'public' | 'protected' | 'private' | 'abstract' | 'static' | 'strictfp' ;
    ExtendsInterfaces = 'extends' InterfaceTypeList ;
    InterfaceBody = '{' InterfaceMemberDeclaration* '}' ;
    InterfaceMemberDeclaration
      = ConstantDeclaration
      | InterfaceMethodDeclaration
      | ClassDeclaration
      | InterfaceDeclaration
      | ';'
      ;
    ConstantDeclaration = ConstantModifier* UnannType VariableDeclaratorList ;
    ConstantModifier = Annotation | CONSTANT_MODIFIER ;
    leaf CONSTANT_MODIFIER = 'public' | 'static' | 'final' ;
    InterfaceMethodDeclaration = InterfaceMethodModifier* MethodHeader MethodBody ;
    InterfaceMethodModifier =  Annotation | 'public' | 'abstract' | 'default' | 'static' | 'strictfp' ;
    AnnotationTypeDeclaration = InterfaceModifier* '@' 'interface' IDENTIFIER AnnotationTypeBody ;
    AnnotationTypeBody = '{' AnnotationTypeMemberDeclaration* '}' ;
    AnnotationTypeMemberDeclaration
      = AnnotationTypeElementDeclaration
      | ConstantDeclaration
      | ClassDeclaration
      | InterfaceDeclaration
      | ';'
      ;
    AnnotationTypeElementDeclaration = AnnotationTypeElementModifier* UnannType IDENTIFIER '(' ')' Dims? DefaultValue? ';' ;
    AnnotationTypeElementModifier = Annotation | ANNOTATION_MODIFIER ;
    leaf ANNOTATION_MODIFIER = 'public' | 'abstract' ;
    DefaultValue = 'default' ElementValue ;
}

grammar Packages : Interfaces {
    CompilationUnit = PackageDeclaration? ImportDeclaration* TypeDeclaration* ;
    PackageDeclaration = PackageModifier* 'package' [IDENTIFIER / '.']+ ';' ;
    PackageModifier = Annotation ;
    ImportDeclaration = 'import' 'static'? QualifiedName ( '.' '*' )? ';' ;
    TypeDeclaration = ClassDeclaration | InterfaceDeclaration | ';' ;
}
        """.trimIndent()

    }

    @Test
    fun parse_blocks_empty() {
        val processor = Agl.processorFromString(grammarStr, Agl.configuration {
            targetGrammarName(("BlocksAndStatements"))
            defaultGoalRuleName("Block")
        }).processor!!
        val goal = "Block"
        val sentence = """
        {}
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun process_blocks_empty() {
        val processor = Agl.processorFromString(grammarStr, Agl.configuration(Agl.configurationSimple()) {
            targetGrammarName(("BlocksAndStatements"))
            defaultGoalRuleName("Block")
        }).processor!!
        val goal = "Block"
        val sentence = """
        {}
        """.trimIndent()
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertNotNull(result.asm)
        assertTrue(result.allIssues.errors.isEmpty())
    }
}