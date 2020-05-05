/**
 *  Derived from  the main text of the Java Language Specification
 *
 *  author: Ali Afroozeh
 */
module java::JavaSpecification

extend java::\lexical::CharLevel;


/************************************************************************************************************************
 * Types, Values, and Variables
 ***********************************************************************************************************************/

syntax Type
     = PrimitiveType
     | ReferenceType
     ;

syntax PrimitiveType
     = "byte"
     | "short"
     | "char"
     | "int"
     | "long"
     | "float"
     | "double"
     | "boolean"
     ;

syntax ReferenceType
     = TypeDeclSpecifier TypeArguments?
     | ArrayType
     ;

syntax ReferenceTypeNonArrayType
     = TypeDeclSpecifier TypeArguments?
     ;

syntax TypeList
     = {Type ","}+
     ;

syntax TypeName
     = QualifiedIdentifier
     ;

syntax TypeVariable
     = Identifier
     ;

syntax ArrayType
     = Type "[" "]"
     ;

syntax TypeParameters
     = "\<" {TypeParameter ","}+ "\>"
     ;

syntax TypeParameter
     = TypeVariable TypeBound?
     ;

syntax TypeBound
     = "extends" {ReferenceType "&"}+
     ;

syntax TypeArguments
     = "\<" {TypeArgument ","}+ "\>"
     ;

syntax TypeArgument  // fix: changed ReferenceType to Type to deal with primitive array types such as < String[] >
     = Type
     | "?" (("extends" | "super") Type)?
     ;

syntax QualifiedIdentifier
     = {Identifier "."}+;

syntax QualifiedIdentifierList
     = {QualifiedIdentifier  ","}+;

/************************************************************************************************************************
 * Top level
 ***********************************************************************************************************************/

start syntax CompilationUnit
    = PackageDeclaration? ImportDeclaration* TypeDeclaration*
      ;

syntax PackageDeclaration
     = Annotation* "package"  QualifiedIdentifier ";"
     ;

syntax ImportDeclaration
    = "import"  "static"?  {Identifier "."}+ ("." "*")? ";"
    ;

syntax TypeDeclaration
    = ClassDeclaration
    | InterfaceDeclaration
    | ";"
    ;

 /************************************************************************************************************************
 * Classes
 ***********************************************************************************************************************/

syntax ClassDeclaration
     = NormalClassDeclaration
     | EnumDeclaration
     ;

syntax NormalClassDeclaration
    = ClassModifier* "class" Identifier TypeParameters? ("extends" Type)? ("implements" TypeList)? ClassBody;

syntax ClassModifier
     = Annotation
     | "public"
     | "protected"
     | "private"
     | "abstract"
     | "static"
     | "final"
     | "strictfp"
     ;

syntax ClassBody
    = "{" ClassBodyDeclaration* "}"
    ;

syntax ClassBodyDeclaration
     = ClassMemberDeclaration
     | InstanceInitializer
     | StaticInitializer
     | ConstructorDeclaration
     ;

syntax InstanceInitializer
     = Block
     ;

syntax StaticInitializer
     = "static" Block
     ;

syntax ConstructorDeclaration
     = ConstructorModifier* ConstructorDeclarator Throws? ConstructorBody
     ;

syntax ConstructorModifier
     = Annotation
     | "public"
     | "protected"
     | "private"
     ;

syntax ConstructorDeclarator
     = TypeParameters? Identifier "(" FormalParameterList? ")"
     ;

syntax ConstructorBody
     = "{" ExplicitConstructorInvocation? BlockStatement* "}"
     ;

syntax ExplicitConstructorInvocation
     = NonWildTypeArguments? "this" "(" ArgumentList? ")" ";"
     | NonWildTypeArguments? "super" "(" ArgumentList? ")" ";"
     | Primary "." NonWildTypeArguments? "super" "(" ArgumentList? ")" ";"
     ;

syntax NonWildTypeArguments
     = "\<" { ReferenceType ","}+ "\>"
     ;

syntax ClassMemberDeclaration
     = FieldDeclaration
     | MethodDeclaration
     | ClassDeclaration
     | InterfaceDeclaration
     | ";"
	 ;

/************************************************************************************************************************
 * Interfaces
 ***********************************************************************************************************************/

syntax InterfaceDeclaration
     = NormalInterfaceDeclaration
     | AnnotationTypeDeclaration
     ;

syntax NormalInterfaceDeclaration
     =  InterfaceModifier* "interface" Identifier TypeParameters? ("extends" TypeList)? InterfaceBody
     ;

syntax InterfaceModifier
     = Annotation
     | "public"
     | "protected"
     | "private"
     | "abstract"
     | "static"
     | "strictfp"
     ;

syntax InterfaceBody
     = "{" InterfaceMemberDeclaration* "}"
     ;

syntax InterfaceMemberDeclaration
     = ConstantDeclaration
     | AbstractMethodDeclaration
     | ClassDeclaration
     | InterfaceDeclaration
     | ";"
     ;

syntax ConstantDeclaration
     = ConstantModifier* Type VariableDeclarators ";"
     ;

syntax ConstantModifier
     = Annotation
     | "public"
     | "static"
     | "final"
     ;

syntax AbstractMethodDeclaration
     = AbstractMethodModifier* TypeParameters? Result MethodDeclarator Throws? ";"
     ;

syntax AbstractMethodModifier
     = Annotation
     | "public"
     | "abstract"
     ;

syntax AnnotationTypeDeclaration
     = InterfaceModifier* "@" "interface" Identifier AnnotationTypeBody
     ;

syntax AnnotationTypeBody
     = "{" AnnotationTypeElementDeclaration* "}"
     ;

syntax AnnotationTypeElementDeclaration
     = AbstractMethodModifier* Type Identifier "(" ")" ("[" "]")* DefaultValue? ";"
     | ConstantDeclaration
     | ClassDeclaration
     | InterfaceDeclaration
     | AnnotationTypeDeclaration
     | ";"
     ;

syntax DefaultValue
     = "default" ElementValue
     ;

/************************************************************************************************************************
 * Fields
 ***********************************************************************************************************************/

syntax FieldDeclaration
     = FieldModifier* Type VariableDeclarators ";"
     ;

syntax FieldModifier
     = Annotation
     | "public"
     | "protected"
     | "private"
     | "static"
     | "final"
     | "transient"
     | "volatile"
     ;

syntax VariableDeclarators
    = {VariableDeclarator ","}+
    ;

syntax VariableDeclarator
     = VariableDeclaratorId ("=" VariableInitializer)?
     ;

syntax VariableDeclaratorId
    = Identifier ("[" "]")*
    ;

syntax VariableInitializer
    = ArrayInitializer
    | Expression
    ;

syntax ArrayInitializer
    = "{"  {VariableInitializer ","}* ","? "}"
    ;

/************************************************************************************************************************
 * Methods
 ***********************************************************************************************************************/

syntax MethodDeclaration
     = MethodHeader MethodBody
     ;

syntax MethodHeader
     = MethodModifier* TypeParameters? Result MethodDeclarator Throws?
     ;

syntax MethodDeclarator
     = Identifier "(" FormalParameterList? ")"
     | MethodDeclarator "[" "]"
     ;

syntax FormalParameterList
     = (FormalParameter ",")* LastFormalParameter
     ;

syntax FormalParameter
     = VariableModifier* Type VariableDeclaratorId
     ;

syntax VariableModifier
    = "final"
    | Annotation
    ;

syntax LastFormalParameter
     = VariableModifier* Type "..." VariableDeclaratorId
     | FormalParameter
     ;

syntax MethodModifier
     = Annotation
     | "public"
     | "protected"
     | "private"
     | "abstract"
     | "static"
     | "final"
     | "synchronized"
     | "native"
     | "strictfp"
     ;

syntax Result
     = Type
     | "void"
     ;

syntax Throws
     = "throws" {ExceptionType ","}+
     ;

syntax ExceptionType
     = TypeName
     ;

syntax MethodBody
     = Block
     | ";"
     ;


/************************************************************************************************************************
 * Annotations
 ***********************************************************************************************************************/

syntax Annotation
    = "@" TypeName  "(" {ElementValuePair ","}* ")"
    | "@" TypeName ( "(" ElementValue ")" )?
    ;

syntax ElementValuePair
    = Identifier "=" ElementValue
    ;

syntax ElementValue
    = ConditionalExpression
    | Annotation
    | ElementValueArrayInitializer
    ;

syntax ElementValueArrayInitializer
    = "{" ElementValues? ","? "}"
    ;

syntax ElementValues
     = {ElementValue ","}+
     ;

/************************************************************************************************************************
 * Enums
 ***********************************************************************************************************************/

syntax EnumDeclaration
     = ClassModifier* "enum" Identifier ("implements" TypeList)? EnumBody
     ;

syntax EnumBody
     = "{" {EnumConstant ","}* ","? EnumBodyDeclarations? "}"
     ;

syntax EnumConstant
     = Annotation* Identifier Arguments? ClassBody?
     ;

syntax Arguments
     = "(" ArgumentList? ")"
     ;

syntax EnumBodyDeclarations
     = ";" ClassBodyDeclaration*
     ;

/************************************************************************************************************************
 * Statements
 ***********************************************************************************************************************/

syntax Block
     = "{" BlockStatement* "}"
     ;

syntax BlockStatement
     = LocalVariableDeclarationStatement
     | ClassDeclaration
     | Statement
     ;

syntax LocalVariableDeclarationStatement
     = VariableModifier*  Type VariableDeclarators ";"
     ;

syntax Statement
     = StatementWithoutTrailingSubstatement
     | Identifier ":" Statement
     | "if" "(" Expression ")" Statement
     | "if" "(" Expression ")" StatementNoShortIf "else" Statement
     | "while" "(" Expression ")" Statement
     | ForStatement
     ;

syntax StatementWithoutTrailingSubstatement
     = Block
     | ";"
     | StatementExpression ";"
     | "assert" Expression (":" Expression)? ";"
     | "switch" "(" Expression ")" "{" SwitchBlockStatementGroup* SwitchLabel* "}"
     | "do" Statement "while" "(" Expression ")" ";"
     | "break" Identifier? ";"
     | "continue" Identifier? ";"
     | "return" Expression? ";"
     | "synchronized" "(" Expression ")" Block
     | "throw" Expression ";"
     | "try" Block (CatchClause+ | (CatchClause* Finally))
     | "try" ResourceSpecification Block CatchClause* Finally?
     ;

syntax StatementNoShortIf
     = StatementWithoutTrailingSubstatement
     | Identifier ":" StatementNoShortIf
     | "if" "(" Expression ")" StatementNoShortIf "else" StatementNoShortIf
     | "while" "(" Expression ")" StatementNoShortIf
     | "for" "(" ForInit? ";" Expression? ";" ForUpdate? ")" StatementNoShortIf
     ;

syntax ForStatement
     = "for" "(" ForInit? ";" Expression? ";" ForUpdate? ")" Statement
     | "for" "(" FormalParameter ":" Expression ")" Statement
     ;

syntax StatementExpression
     = Assignment
     | PreIncrementExpression
     | PreDecrementExpression
     | PostIncrementExpression
     | PostDecrementExpression
     | MethodInvocation
     | ClassInstanceCreationExpression
    ;

syntax CatchClause
    = "catch" "(" VariableModifier* CatchType Identifier ")" Block
    ;

syntax CatchType
    =  {QualifiedIdentifier "|"}+
    ;

syntax Finally
    = "finally" Block
    ;

syntax ResourceSpecification
    = "(" Resources ";"? ")"
    ;

syntax Resources
    = {Resource ";"}+
    ;

syntax Resource
     = VariableModifier* ReferenceType VariableDeclaratorId "=" Expression
     ;

syntax SwitchBlockStatementGroup
	     = SwitchLabel+ BlockStatement+
     ;

syntax SwitchLabel
     = "case" ConstantExpression ":"
     | "default" ":"
     ;

syntax LocalVariableDeclaration
    = VariableModifier* Type { VariableDeclarator ","}+
    ;

syntax ForInit
     = {StatementExpression ","}+
     | LocalVariableDeclaration
     ;

syntax ForUpdate
     = {StatementExpression ","}+
     ;

/************************************************************************************************************************
 * Expressions
 ***********************************************************************************************************************/

syntax Primary
	 =  PrimaryNoNewArray
	 |  ArrayCreationExpression
	 ;

syntax PrimaryNoNewArray
     = Literal
     | Type "." "class"
     | "void" "." "class"
     | "this"
     | ClassName "." "this"
     | "(" Expression ")"
     | ClassInstanceCreationExpression
     | FieldAccess
     | MethodInvocation
     | ArrayAccess
     ;

syntax ClassInstanceCreationExpression
     = "new" TypeArguments? TypeDeclSpecifier TypeArgumentsOrDiamond?  "(" ArgumentList? ")" ClassBody?
     | (Primary | QualifiedIdentifier) "." "new" TypeArguments? Identifier TypeArgumentsOrDiamond? "(" ArgumentList? ")" ClassBody?
     ; // Check if it's a good idea to add Identifier to primary?

syntax TypeArgumentsOrDiamond
     = "\<" "\>"
     | TypeArguments
     ;

syntax ArgumentList
     = {Expression ","}+
     ;

syntax ArrayCreationExpression
	     = "new" (PrimitiveType | ReferenceType) DimExpr+ ("[" "]")*
	     | "new" (PrimitiveType | ReferenceTypeNonArrayType) ("[" "]")+ ArrayInitializer
     ;

syntax DimExpr
     = "[" Expression "]"
     ;

syntax FieldAccess
     = Primary "." Identifier
     | "super" "." Identifier
     | ClassName "." "super" "." Identifier
     ;

syntax MethodInvocation
     = MethodName "(" ArgumentList? ")"
     | Primary "." NonWildTypeArguments? Identifier "(" ArgumentList? ")"
     | "super" "." NonWildTypeArguments? Identifier "(" ArgumentList? ")"
     | ClassName "." "super" "." NonWildTypeArguments? Identifier "(" ArgumentList? ")"
     | TypeName "." NonWildTypeArguments Identifier "(" ArgumentList? ")"
     ;

syntax ArrayAccess
     = ExpressionName "[" Expression "]"
     | PrimaryNoNewArray "[" Expression "]"
     ;

syntax PostfixExpression
     = Primary
     | ExpressionName
     | PostIncrementExpression
     | PostDecrementExpression
     ;

syntax PostIncrementExpression
     = PostfixExpression "++"
     ;

syntax PostDecrementExpression
     = PostfixExpression "--"
     ;

syntax UnaryExpression
     = PreIncrementExpression
     | PreDecrementExpression
     | "+" !>> "+" UnaryExpression
     | "-" !>> "-" UnaryExpression
     | UnaryExpressionNotPlusMinus
     ;


syntax PreIncrementExpression
     = "++" UnaryExpression
     ;


syntax PreDecrementExpression
     = "--" UnaryExpression
     ;

syntax UnaryExpressionNotPlusMinus
     = PostfixExpression
     | "~" UnaryExpression
     | "!" UnaryExpression
     | CastExpression
     ;

syntax CastExpression
     = "(" PrimitiveType ")" UnaryExpression
     | "(" ReferenceType ")" UnaryExpressionNotPlusMinus
     ;

syntax MultiplicativeExpression
     = UnaryExpression
     | MultiplicativeExpression "*" UnaryExpression
     | MultiplicativeExpression "/" UnaryExpression
     | MultiplicativeExpression "%" UnaryExpression
     ;

syntax AdditiveExpression
     = MultiplicativeExpression
     | AdditiveExpression "+" !>> "+" MultiplicativeExpression
     | AdditiveExpression "-" !>> "-" MultiplicativeExpression
     ;

syntax ShiftExpression
     = AdditiveExpression
     | ShiftExpression "\<\<" AdditiveExpression
     | ShiftExpression "\>\>" AdditiveExpression
     | ShiftExpression "\>\>\>" AdditiveExpression
     ;


syntax RelationalExpression
     = ShiftExpression
     | RelationalExpression "\<" ShiftExpression
     | RelationalExpression "\>" ShiftExpression
     | RelationalExpression "\<=" ShiftExpression
     | RelationalExpression "\>=" ShiftExpression
     | RelationalExpression "instanceof" ReferenceType
     ;


syntax EqualityExpression
     = RelationalExpression
     | EqualityExpression "==" RelationalExpression
     | EqualityExpression "!=" RelationalExpression
     ;

syntax AndExpression
     = EqualityExpression
     | AndExpression "&" EqualityExpression
     ;

syntax ExclusiveOrExpression
     = AndExpression
     | ExclusiveOrExpression "^" AndExpression
     ;

syntax InclusiveOrExpression
     = ExclusiveOrExpression
     | InclusiveOrExpression "|" ExclusiveOrExpression
     ;

syntax ConditionalAndExpression
     = InclusiveOrExpression
     | ConditionalAndExpression "&&" InclusiveOrExpression
     ;

syntax ConditionalOrExpression
     = ConditionalAndExpression
     | ConditionalOrExpression "||" ConditionalAndExpression
     ;


syntax ConditionalExpression
     = ConditionalOrExpression
     | ConditionalOrExpression "?" Expression ":" ConditionalExpression
     ;

syntax AssignmentExpression
     = ConditionalExpression
     | Assignment
     ;


syntax Assignment
     = LeftHandSide AssignmentOperator AssignmentExpression
     ;

syntax LeftHandSide
     = ExpressionName
     | "(" LeftHandSide ")"
     | FieldAccess
     | ArrayAccess
     ;

syntax Expression
     = AssignmentExpression
     ;

syntax ConstantExpression
     = Expression
     ;

syntax ClassName
	 = QualifiedIdentifier
	 ;

syntax ExpressionName
     = QualifiedIdentifier
     ;

syntax MethodName
     = QualifiedIdentifier
     ;

syntax TypeDeclSpecifier
     = Identifier (TypeArguments? "." Identifier)*
     ;

syntax SuperSuffix
     =  Arguments
     | "." Identifier Arguments?
     ;

syntax ExplicitGenericInvocationSuffix
     = "super" SuperSuffix
     | Identifier Arguments
     ;
