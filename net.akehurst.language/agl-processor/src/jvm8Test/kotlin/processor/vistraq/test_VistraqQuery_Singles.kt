package net.akehurst.language.processor.vistraq

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_VistraqQuery_Singles {

    companion object {

        private val grammarStr = """
namespace com.itemis.typedgraph.query

grammar Query {

    skip WHITE_SPACE = "\s+" ;
	skip SINGLE_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
	skip MULTI_LINE_COMMENT = "//.*?${'$'}" ;

    query = singleQuery | compositeQuery ;
    singleQuery = timespanDefinition? querySource? returnDefinition? ;
    compositeQuery = query compositionOperator query ;
    compositionOperator = 'UNION' | 'JOIN' ;

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
    nodeTypeReferenceExpression
        = nodeTypeReference
        < NONE_NODE_TYPE
        < ANY_NODE_TYPE
        < negatedNodeTypeReferenceExpression
        < nodeTypeReferenceGroup
        ;
    negatedNodeTypeReferenceExpression = 'NOT' nodeTypeReferenceExpression ;
    nodeTypeReferenceGroup = '(' [nodeTypeReferenceExpression / 'OR' ]+ ')' ;

    linkedNodeSelector = linkSelectorExpression ;
    linkedNodeSelectorNegated = linkSelectorNegated ;
	linkSelectorExpression = linkSelector | linkSelectorGroupedPath ;
	linkSelector = 'LINKED' links? multiplicity? via? ('TO'|'FROM'|'WITH') nodeSelector ;

	links = 'USING' range 'LINKS' ;
	multiplicity = range 'TIMES' ;
    range = POSITIVE_INT '..' UNLIMITED_POSITIVE_INT | UNLIMITED_POSITIVE_INT ;

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
	nodeTypeConstructionExpression = expression < nodeTypeReference ;
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
    expression
		= conditionalExpression
		< infixFunction
		< propertyCall
		< methodCall
		< root
		< literalValue
    	< groupExpression
        ;

    nameDefinition = 'AS' NAME ;

    groupExpression = '(' expression ')'  ;
	root = NAME;
	propertyCall = expression '.' NAME ;
	methodCall = expression '.' NAME '(' argList ')';
	argList = [expression / ',']* ;
	infixFunction
        = logicalInfixFunction
        < arithmeticInfixFunction
        < comparisonInfixFunction
        ;

    logicalInfixFunction
        = expression ('AND' expression)+  //[ expression / 'AND' expression]2+
        < expression ('OR' expression)+
        < expression ('XOR' expression)+
        ;

    arithmeticInfixFunction
        = expression (('+'|'-') expression)+
        < expression (('*'|'/') expression)+
        ;

    comparisonInfixFunction
        = expression (comparisonOperator expression)+
        ;

    comparisonOperator = '==' | '!=' | '<' | '>' | '<=' | '>=' ;
    logicalOperator = 'AND' | 'OR' | 'XOR' ;
    conditionalExpression = expression '?' expression ':' expression ;


	orderBy = 'ORDER' 'BY' [columnOrder / ',']+ ;
	columnOrder = NAME ('ASCENDING' | 'DESCENDING')? ;

	ANY_NODE_TYPE = 'any' ;
	NONE_NODE_TYPE = 'none' ;
	nodeTypeReference = NAME ;
	ANY_LINK_TYPE = 'any' ;
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
        var processor: LanguageProcessor = tgqlprocessor()

        fun tgqlprocessor() : LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processor(grammarStr).build()
         }

    }

    @Test
    fun NULL() {
        processor.parse("NULL", "null")
    }

    @Test
    fun REAL_0() {

        val e = assertFailsWith(ParseFailedException::class) {
            processor.parse("REAL", "0")
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun REAL_p0() {
        val e = assertFailsWith(ParseFailedException::class) {
            processor.parse("REAL", ".0")
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun REAL_0p0() {
        processor.parse("REAL", "0.0")
    }

    @Test
    fun REAL_3p14() {
        processor.parse("REAL", "3.14")
    }


    @Test
    fun expression_1() {
        val queryStr = "1"
        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun literalValue_0p1() {
        val queryStr = "0.1"
        val result = processor.parse("literalValue", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_0p1() {
        val queryStr = "0.1"
        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun nodeSelector_NOT_A() {
        val queryStr = "NOT A"
        val result = processor.parse("nodeSelector", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }


    @Test
    fun pathExpression_NOT_A() {
        val queryStr = "NOT A"
        val result = processor.parse("pathExpression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery_MATCH_NOT_A() {
        val queryStr = "MATCH NOT A"
        val result = processor.parse("pathQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun expression_literalValue_true() {
        val queryStr = "true"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun logicalInfixFunction_long() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p AND g.p AND h.p AND i.p AND j.p AND k.p AND l.p"

        val result = processor.parse("logicalInfixFunction", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout=5000)
    fun expression_long_3() {
        val queryStr = "a.p AND b.p AND c.p AND d.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout=5000)
    fun expression_long_5() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout=5000)
    fun expression_long_11() {
        val queryStr = "a.p AND b.p AND c.p AND d.p AND e.p AND f.p AND g.p AND h.p AND i.p AND j.p AND k.p AND l.p"

        val result = processor.parse("expression", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }


    @Test
    fun whereClause_expression_literalValue_true() {
        val queryStr = "WHERE true"

        val result = processor.parse("whereClause", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun pathQuery1() {
        val queryStr = "MATCH Milestone WHERE true"

        val result = processor.parse("pathQuery", queryStr)

        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery1() {
        val queryStr = "MATCH Milestone RETURN 1"

        val result = processor.parse("singleQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test
    fun singleQuery2() {
        val queryStr = """
   MATCH Milestone WHERE true RETURN 1
        """.trimIndent()

        val result = processor.parse("singleQuery", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun query1() {
        val queryStr = """
   MATCH A WHERE a == b AND a == b AND true RETURN TABLE COLUMN a CONTAINING a
        """.trimIndent()

        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 5000)
    fun fromBlog() {
        val queryStr = """
FOR TIMESPAN '01-Jan-2017' UNTIL '31-Dec-2017' EVERY month
 MATCH Milestone AS ms
   WHERE ms.dueDate <= now
   RETURN TABLE COLUMN Due CONTAINING COUNT(ms)
 JOIN
   MATCH Milestone AS ms
    ( LINKED * TIMES WITH Bug AS bug
      LINKED USING 1..* LINKS WITH TestResult AS bResult
    OR LINKED WITH Feature AS ft
      ( LINKED * TIMES WITH SubTask AS task
      OR LINKED WITH Requirement AS req
        ( LINKED USING 1..* LINKS WITH TestResult AS iResult
        AND
          LINKED TO CodeFile AS code
          LINKED USING 1..* LINKS WITH TestResult AS uResult
        ) ) )
    LINKED 1 TIMES TO Build AS build
    LINKED 1 TIMES TO Binary AS binary
   WHERE
     ms.released == true AND ft.status == 'done' AND task.status == 'done'
     AND iResult.value == 'pass' AND uResult.value == 'pass'
     AND build.testCoverage >= 80 AND bug.status == 'done'
     AND bResult.value == 'pass' AND build.buildDate < ms.dueDate
     AND build.version == ms.version AND binary.version == ms.version
     AND binary.publishedDate < ms.dueDate
   RETURN TABLE COLUMN Met CONTAINING COUNT(ms)
 JOIN
   RETURN TABLE COLUMN Percent CONTAINING (Met / Due)* 100
        """.trimIndent()

        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }
}
