package net.akehurst.language.processor.tgql

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.processor.processor
import kotlin.test.*

class test_TGQL {

    companion object {
        private val grammarStr = """
namespace com.itemis.typedgraph.query

grammar Query {

    skip WHITE_SPACE = "\s+" ;
	skip SINGLE_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
	skip MULTI_LINE_COMMENT = "//.*?$" ;

	query = singleQuery | unionedQuery ;
	singleQuery = timespanDefinition? querySource? returnDefinition? ;
	unionedQuery = query unionOperator query ;
	unionOperator = 'UNION' ; //TODO: handle distinct or duplicates

    timespanDefinition = 'FOR' timeDef nameDefinition? ;
	timeDef =  time | timespan ;
	time = 'TIME' timePoint ;
	timePoint = 'start' | 'now' | timestamp ;
	timestamp = SINGLE_QUOTE_STRING ;
	timespan = 'TIMESPAN' timeRange 'EVERY' period ;
	timeRange = 'all' | timePoint 'UNTIL' timePoint ;
	period = 'second' | 'minute' | 'hour' | 'day' | 'week' | 'month' | 'year' ;


	querySource = pathQuery | storedQueryReference ;
	storedQueryReference = 'FROM' STORED_QUERY_ID ;
    pathQuery = 'MATCH' pathExpression whereClause? ;
    pathExpression =  nodeSelector linkedNodeSelectorPath? ;

	nodeSelector = nodeTypeReferenceExpression nameDefinition? ;
    nodeTypeReferenceExpression = ANY_NODE_TYPE | NONE_NODE_TYPE | nodeTypeReference | negatedNodeTypeReferenceExpression | nodeTypeReferenceGroup ;
    negatedNodeTypeReferenceExpression = 'NOT' nodeTypeReferenceExpression ;
    nodeTypeReferenceGroup = '(' [nodeTypeReferenceExpression / 'OR' ]+ ')' ;

    linkedNodeSelector = linkSelectorExpression ;
    linkedNodeSelectorNegated = linkSelectorNegated ;
	linkSelectorExpression = linkSelector | linkSelectorGroupedPath ;
	linkSelector = 'LINKED' links? multiplicity? via? ('TO'|'FROM'|'WITH') nodeSelector ;

	links = 'USING' POSITIVE_INT '..' UNLIMITED_POSITIVE_INT 'LINKS' ;
	multiplicity = POSITIVE_INT '..' UNLIMITED_POSITIVE_INT 'TIMES' ;

	via = 'VIA'  linkTypeReferenceExpression ;
	linkSelectorNegated = 'NOT' linkSelectorGroupedPath ;
	linkSelectorGroupedPath = '(' linkSelectorGroupedItem ')' ;
	linkSelectorGroupedItem = linkedNodeSelectorPath | linkSelectorOperator ;
	//linkSelectorGroupedOperator = '(' linkSelectorOperator ')';
	linkSelectorOperator = linkedNodeSelectorPath logicalOperator linkedNodeSelectorPath ;

	linkedNodeSelectorPath = linkedNodeSelectorPathNormal | linkedNodeSelectorPathNegated ;
	linkedNodeSelectorPathNormal = linkedNodeSelector+ ;
	linkedNodeSelectorPathNegated = linkedNodeSelector*  linkedNodeSelectorNegated ;

    linkTypeReferenceExpression = ANY_LINK_TYPE | linkTypeReference | negatedLinkTypeReferenceExpression | linkTypeReferenceGroup ;
    negatedLinkTypeReferenceExpression = 'NOT' linkTypeReferenceExpression ;
    linkTypeReferenceGroup = '(' [linkTypeReferenceExpression / 'OR' ]+ ')' ;

	whereClause = 'WHERE' expression ;

	returnDefinition = returnExpression | returnTable | returnSelect | returnGraph ;

	returnExpression = 'RETURN' aggregateFunctionCallOrExpression ;

	returnTable = 'RETURN' 'TABLE' columnDefinition+ orderBy? ;
	columnDefinition = 'COLUMN' NAME 'CONTAINING' aggregateFunctionCallOrExpression ;

    returnSelect = 'RETURN' 'SELECT' selectList;
    selectList = [selectItem / ',']* ;
    selectItem = NAME | propertyReference ;
    propertyReference = NAME '.' NAME ;

	returnGraph = 'RETURN' 'GRAPH' subgraphConstruction ;
	subgraphConstruction = nodeConstruction linkedSubGraphConstruction? whereConstructionClause?;
	nodeConstruction = nodeTypeConstructionExpression '{' nodeIdentityExpressionConstruction nodePropertyAssignmentExpressionList? '}' nameDefinition?;
	nodeTypeConstructionExpression = nodeTypeReference < expression ;
    nodeIdentityExpressionConstruction = expression ;
    nodePropertyAssignmentExpressionList = '|' nodePropertyAssignmentExpression+ ;
    nodePropertyAssignmentExpression = NAME ':=' expression ;

	linkedSubGraphConstruction = linkedNodeConstructionExpression+ ;
	linkedNodeConstructionExpression = linkedNodeConstructionPath | linkedNodeConstructionGroup ;
	linkedNodeConstructionPath = linkedNodeConstruction+ ;
	linkedNodeConstruction = 'LINKED' 'VIA' linkTypeReference ('TO'|'FROM') nodeConstruction ;
	linkedNodeConstructionGroup = '(' linkedNodeConstructionGroupItem ')' ;
	linkedNodeConstructionGroupItem = linkedNodeConstructionPath |  linkedNodeConstructionGroupItemOperator ;
	linkedNodeConstructionGroupItemOperator =  linkedNodeConstructionExpression 'AND' linkedNodeConstructionExpression ;

	whereConstructionClause = 'WHERE' constructionExpression ;

	constructionExpression = andConstructionExpression
	                       < propertyAssignment
                           ;

	propertyAssignment = propertyCall ':=' expression ;
	andConstructionExpression = constructionExpression 'AND' constructionExpression ;

	aggregateFunctionCallOrExpression = aggregateFunctionCall | expression ;
	aggregateFunctionCall = aggregateFunctionName '(' expression ')' ;
    expression	= groupExpression
				< literalValue
				< root
				< propertyCall
				< methodCall
				< infixFunction
				< conditionalExpression
            ;

    nameDefinition = 'AS' NAME ;

    groupExpression = '(' expression ')'  ;
	root = NAME;
	propertyCall = expression '.' NAME ;
	methodCall = expression '.' NAME '(' argList ')';
	argList = [expression / ',']* ;
	infixFunction = expression operator expression ;
	operator = arithmeticOperator | comparisonOperator | logicalOperator ;
	arithmeticOperator = '+' | '-' | '*' | '/' ;
    comparisonOperator = '==' | '!=' | '<' | '>' | '<=' | '>=' ;
    logicalOperator= 'AND' | 'OR' | 'XOR' ;
    conditionalExpression = expression '?' expression ':' expression ;


	orderBy = 'ORDER' 'BY' [columnOrder / ',']+ ;
	columnOrder = NAME ('ASCENDING' | 'DESCENDING')? ;

	ANY_NODE_TYPE = '*' ;
	NONE_NODE_TYPE = 'none' ;
	nodeTypeReference = NAME ;
	ANY_LINK_TYPE = '*' ;
	linkTypeReference = NAME ;
	aggregateFunctionName = NAME ;
	STORED_QUERY_ID = "([a-zA-Z_][a-zA-Z0-9_]*)([.][a-zA-Z_][a-zA-Z0-9_]*)?" ; // project metricDef OR metricSet.metricDef
	NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
	POSITIVE_INT = "[0-9]+" ;
	UNLIMITED_POSITIVE_INT = "[0-9]+" | '*' ;

    literalValue = BOOLEAN | SINGLE_QUOTE_STRING | INTEGER | REAL | NULL ;
    BOOLEAN = 'true' | 'false' ;
    SINGLE_QUOTE_STRING = "'(?:\\?.)*?'" ;
    INTEGER = "[0-9]+" ;
    REAL = "[0-9]+[.][0-9]+" ;
    NULL = 'null' ;
}
        """.trimIndent()

        private val p = processor(grammarStr)
    }

    @Test
    fun NULL() {
        p.parse("NULL", "null")
    }

    @Test
    fun REAL_0() {

        val e = assertFailsWith(ParseFailedException::class) {
            p.parse("REAL", "0")
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun REAL_p0() {
        val e = assertFailsWith(ParseFailedException::class) {
            p.parse("REAL", ".0")
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun REAL_0p0() {
        p.parse("REAL", "0.0")
    }

    @Test
    fun REAL_3p14() {
        p.parse("REAL", "3.14")
    }


    @Test
    fun MATCH_any_RETURN_true() {
        p.parse("query", "MATCH * RETURN true")
    }
}