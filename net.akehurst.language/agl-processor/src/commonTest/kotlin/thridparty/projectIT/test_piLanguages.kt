package thridparty.projectIT

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_piLanguages {

    private companion object {
        val grammarStr = """
            namespace PiLanguageLanguage
            grammar PiLanguageGrammar {
                            
            // rules for "PiEditorDef"
            PiEditorDef = 'editor' identifier 'for' 'language' stringLiteral
                 PiEditConcept* ;
            
            PiEditConcept = __pi_reference '{'
                 PiEditProjection ( '@trigger' stringLiteral )?
                 ( '@symbol' stringLiteral )? ;
            
            PiEditProjection = 'PiEditProjection' identifier
                 'conceptEditor' PiEditConcept?
                 'lines'
                 PiEditProjectionLine* ;
            
            PiEditProjectionLine = 'PiEditProjectionLine'
                 'indent' numberLiteral
                 'items'
                 PiEditProjectionItem* ;
            
            PiEditParsedProjectionIndent = 'PiEditParsedProjectionIndent'
                 'indent' stringLiteral
                 'amount' numberLiteral ;
            
            PiEditProjectionText = 'PiEditProjectionText'
                 'text' stringLiteral
                 'style' stringLiteral ;
            
            PiEditPropertyProjection = 'PiEditPropertyProjection'
                 'propertyName' stringLiteral
                 'keyword' stringLiteral?
                 'listJoin' ListJoin
                 'expression' PiLangExp ;
            
            ListJoin = 'ListJoin'
                 'joinText' stringLiteral?
                 'direction' PiEditProjectionDirection
                 'joinType' ListJoinType? ;
            
            PiEditSubProjection = 'PiEditSubProjection'
                 'optional' booleanLiteral
                 'items'
                 PiEditProjectionItem* ;
            
            PiEditInstanceProjection = 'PiEditInstanceProjection'
                 'keyword' stringLiteral
                 'expression' PiInstanceExp ;
            
            PiEditProjectionItem = PiEditParsedProjectionIndent 
                | PiEditProjectionText 
                | PiEditPropertyProjection 
                | PiEditSubProjection 
                | PiEditInstanceProjection  ;
            
            PiEditProjectionDirection = identifier ;
            
            ListJoinType = identifier ;
            
            // rules for "PiScoperDef"
            PiScoperDef = 'scoper' identifier 'for' 'language' stringLiteral ( 'isnamespace' '{' [ __pi_reference / ',' ]* '}' )?
                 ScopeConceptDef* ;
            
            ScopeConceptDef = __pi_reference '{' PiNamespaceAddition? PiAlternativeScope? '}' ;
            
            PiNamespaceAddition = 'namespace_addition' '=' [ PiLangExp / ' +' ]* ';' ;
            
            PiAlternativeScope = 'scope' '=' PiLangExp ';' ;
            
            // rules for "PiStructureDef"
            PiStructureDef = 'language' identifier PiModelDescription? PiUnitDescription* PiInterface*
                 __pi_super_PiConcept* ;
            
            PiModelDescription = 'model' identifier '{'
                 ( PiProperty ';' )*
                 '}' ;
            
            PiConceptProperty = 'public'? 'reference'? identifier '?'? ':' __pi_reference '()'? ;
            
            PiPrimitiveProperty = 'public'? 'static'? identifier '?'? ':' __pi_reference '()'? ;
            
            PiStringValue = stringLiteral ;
            
            PiNumberValue = numberLiteral ;
            
            PiBooleanValue = booleanLiteral ;
            
            PiUnitDescription = 'modelunit' identifier '{'
                 ( PiProperty ';' )*
                 'file-extension' '=' stringLiteral ';'
                 '}' ;
            
            PiConcept = 'public'? 'abstract'? 'concept' identifier ( 'base' __pi_reference )?
                 ( 'implements' [ __pi_reference / ',' ]* )?
                 '{'
                 ( PiProperty ';' )*
                 '}' ;
            
            PiExpressionConcept = 'public'? 'abstract'? 'expression' identifier ( 'base' __pi_reference )?
                 ( 'implements' [ __pi_reference / ',' ]* )?
                 '{'
                 ( PiProperty ';' )*
                 '}' ;
            
            PiBinaryExpressionConcept = 'public'? 'abstract'? 'binary' 'expression' identifier ( 'base' __pi_reference )?
                 ( 'implements' [ __pi_reference / ',' ]* )?
                 '{'
                 ( PiProperty ';' )*
                 ( 'priority' '=' numberLiteral ';' )?
                 '}' ;
            
            PiLimitedConcept = 'public'? 'limited' identifier ( 'base' __pi_reference )?
                 ( 'implements' [ __pi_reference / ',' ]* )?
                 '{'
                 ( PiProperty ';' )*
                 PiInstance*
                 '}' ;
            
            PiInstance = identifier '=' '{'
                 [ PiPropertyInstance / ',' ]*
                 '}' ;
            
            PiPropertyInstance = identifier ':' PiPrimitiveValue ;
            
            PiInterface = 'public'? 'interface' identifier ( 'base' [ __pi_reference / ',' ]* )?
                 '{'
                 ( PiProperty ';' )*
                 '}' ;
            
            PiProperty = PiConceptProperty 
                | PiPrimitiveProperty  ;
            
            PiPrimitiveValue = PiStringValue 
                | PiNumberValue 
                | PiBooleanValue  ;
            
            __pi_super_PiConcept = PiConcept 
                | __pi_super_PiExpressionConcept 
                | PiLimitedConcept  ;
            
            __pi_super_PiExpressionConcept = PiExpressionConcept 
                | PiBinaryExpressionConcept  ;
            
            // rules for "PiTyperDef"
            PiTyperDef = 'PiTyperDef' identifier
                 'languageName' stringLiteral
                 'typerRules'
                 PiTypeRule*
                 'classifierRules'
                 PiTypeClassifierRule*
                 'language' __pi_reference
                 'typeroot' __pi_reference
                 'types'
                 __pi_reference* ;
            
            PiTypeIsTypeRule = 'PiTypeIsTypeRule' identifier
                 'types'
                 __pi_reference* ;
            
            PiTypeAnyTypeRule = 'PiTypeAnyTypeRule' identifier
                 'statements'
                 PiTypeStatement* ;
            
            PiTypeStatement = 'PiTypeStatement'
                 'statementtype' stringLiteral
                 'isAbstract' booleanLiteral
                 'exp' PiLangExp ;
            
            PiTypeClassifierRule = 'PiTypeClassifierRule' identifier
                 'statements'
                 PiTypeStatement*
                 'conceptRef' __pi_reference ;
            
            PiTypeRule = PiTypeIsTypeRule 
                | PiTypeAnyTypeRule 
                | PiTypeClassifierRule  ;
            
            // rules for "PiValidatorDef"
            PiValidatorDef = 'PiValidatorDef' identifier
                 'languageName' stringLiteral
                 'conceptRules'
                 ConceptRuleSet* ;
            
            ConceptRuleSet = 'ConceptRuleSet'
                 'rules'
                 ValidationRule*
                 'conceptRef' __pi_reference ;
            
            CheckEqualsTypeRule = 'CheckEqualsTypeRule'
                 'type1' PiLangExp
                 'type2' PiLangExp ;
            
            CheckConformsRule = 'CheckConformsRule'
                 'type1' PiLangExp
                 'type2' PiLangExp ;
            
            ExpressionRule = 'ExpressionRule'
                 'exp1' PiLangExp
                 'exp2' PiLangExp
                 'comparator' PiComparator ;
            
            IsuniqueRule = 'IsuniqueRule'
                 'list' PiLangExp
                 'listproperty' PiLangExp
                 'comparator' PiComparator ;
            
            NotEmptyRule = 'NotEmptyRule'
                 'property' PiLangExp ;
            
            ValidNameRule = 'ValidNameRule'
                 'property' PiLangExp ;
            
            ValidationRule = CheckEqualsTypeRule 
                | CheckConformsRule 
                | ExpressionRule 
                | IsuniqueRule 
                | NotEmptyRule 
                | ValidNameRule  ;
            
            PiComparator = identifier ;
            
            // common rules
            PiLangSimpleExp = 'PiLangSimpleExp' identifier
                 'value' numberLiteral
                 'sourceName' stringLiteral
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiLangSelfExp = 'PiLangSelfExp' identifier
                 'sourceName' stringLiteral
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiInstanceExp = 'PiInstanceExp' identifier
                 'sourceName' stringLiteral
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiLangConceptExp = 'PiLangConceptExp' identifier
                 'sourceName' stringLiteral
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiLangAppliedFeatureExp = 'PiLangAppliedFeatureExp' identifier
                 'sourceName' stringLiteral
                 'sourceExp' __pi_reference
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiLangFunctionCallExp = 'PiLangFunctionCallExp' identifier
                 'returnValue' booleanLiteral
                 'sourceName' stringLiteral
                 'actualParams'
                 PiLangExp*
                 'referredElement' __pi_reference
                 'language' __pi_reference ;
            
            PiLangExp = PiLangSelfExp 
                | PiInstanceExp 
                | PiLangConceptExp 
                | PiLangAppliedFeatureExp 
                | PiLangSimpleExp 
                | PiLangFunctionCallExp  ;   
            
            __pi_reference = [ identifier / '::' ]+ ;
                    
            // white space and comments
            skip WHITE_SPACE = "\s+" ;
            skip SINGLE_LINE_COMMENT = "//[^\r\n]*" ;
            skip MULTI_LINE_COMMENT = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
                    
            // the predefined basic types   
            leaf identifier          = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            leaf stringLiteral       = "\"([^\"\\]|\\.)*\"";
            leaf numberLiteral       = "[0-9]+";
            leaf booleanLiteral      = 'false' | 'true';       
            }
        """.trimIndent()
        val processor = Agl.processorFromStringDefault(grammarStr)
    }

    @Test
    fun t() {
        val sentence = """
            language Example

            model Demo {
                name: identifier;
                units: ExampleUnit();
            }
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PiStructureDef") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun sentence1() {
        val sentence = """
            language Example
            
            // TODO because the chars '['and ']' can not yet be escaped in the .edit file,
            // here they are replaced by '(' and ')'
            
            model Demo {
                name: identifier;
                units: ExampleUnit();
            }
            
            modelunit ExampleUnit {
                public name: identifier;
                public entities: Entity();
                public methods: Method();
                file-extension = "exm";
            }
            interface BaseType {
                public name: identifier;
            }
            interface Type base BaseType {
            }
            
            public concept Entity implements Type {
                attributes: Attribute();
                methods: Method();
                reference baseEntity?: Entity;
            }
            
            public concept Method {
                public name: identifier;
                reference declaredType : Type;
                body: ExExpression;
                parameters: Parameter();
            }
            
            concept Attribute {
                reference declaredType: Type;
                name: identifier;
            }
            
            limited AttributeType implements Type {
                name: identifier;
                extra: number;
                String = { name: "String", extra: 199}
                Integer = { name: "Integer", extra: 240261}
                Boolean = { name: "Boolean", extra: 5479}
                ANY = { name: "ANY", extra: 456}
            }
            
            concept Parameter  {
                name: identifier;
                reference declaredType: Type;
            }
            
            ////////////////////////////////////
            //       Expressions
            ////////////////////////////////////
            
            abstract expression ExExpression {
                appliedfeature?: AppliedFeature;
            }
            
            abstract expression LiteralExpression base ExExpression {}
            
            expression StringLiteralExpression base LiteralExpression {
                value: string;
            }
            
            expression NumberLiteralExpression base LiteralExpression   {
                value: number;
            }
            
            expression BooleanLiteralExpression base LiteralExpression {
                value: boolean;
            }
            
            expression AbsExpression base ExExpression {
                expr: ExExpression;
            }
            
            abstract concept AppliedFeature {
                appliedfeature?: AppliedFeature;
            }
            
            concept AttributeRef base AppliedFeature {
                reference attribute: Attribute;
            }
            
            expression ParameterRef base ExExpression {
                reference parameter: Parameter;
            }
            
            concept LoopVariable {
                name: identifier;
            }
            
            expression GroupedExpression base ExExpression {
                inner: ExExpression;
            }
            
            expression LoopVariableRef base ExExpression {
                reference variable: LoopVariable;
            }
            
            expression SumExpression base ExExpression {
                variable: LoopVariable;
                from: ExExpression;
                to  : ExExpression;
                body: ExExpression;
            }
            
            expression MethodCallExpression base ExExpression {
                reference methodDefinition: Method;
                args: ExExpression();
            }
            
            expression IfExpression base ExExpression {
                condition: ExExpression;
                whenTrue: ExExpression;
                whenFalse: ExExpression;
            }
            
            ////////////////////////////////////
            //    Binary ExExpressions
            ////////////////////////////////////
            abstract binary expression BinaryExpression base ExExpression {
                left: ExExpression;
                right: ExExpression;
            }
            
            binary expression MultiplyExpression base BinaryExpression {
                priority = 8;
            }
            
            binary expression PlusExpression base BinaryExpression {
                priority = 4;
            }
            
            binary expression DivideExpression base BinaryExpression {
                priority = 8;
            }
            
            binary expression AndExpression base BinaryExpression {
                priority = 1;
            }
            
            binary expression OrExpression base BinaryExpression {
                priority = 1;
            }
            
            abstract binary expression ComparisonExpression base BinaryExpression {
            }
            
            binary expression LessThenExpression base ComparisonExpression {
                priority = 10;
            }
            
            binary expression GreaterThenExpression base ComparisonExpression {
                priority = 10;
            }
            
            binary expression EqualsExpression base ComparisonExpression {
                priority = 10;
            }
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PiStructureDef") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(emptyList(), result.issues)
    }
}