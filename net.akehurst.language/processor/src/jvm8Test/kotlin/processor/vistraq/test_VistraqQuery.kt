package net.akehurst.language.processor.vistraq

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.processor.processor

@RunWith(Parameterized::class)
class test_QueryParserValid(val data:Data) {

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
				< conditionalExpression
				< infixFunction
				< literalValue
				< root
				< propertyCall
				< methodCall
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
        var processor: LanguageProcessor = tgqlprocessor()

        var sourceFiles = arrayOf("vistraq/sampleValidQueries.txt")

        fun tgqlprocessor() : LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return processor(grammarStr)
         }

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in test_QueryParserValid.sourceFiles) {
                val `is` = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile)
                val br = BufferedReader(InputStreamReader(`is`))
                var line: String? = br.readLine()
                while (null != line) {
                    line = line.trim { it <= ' ' }
                    if (line.isEmpty()) {
                        // blank line
                        line = br.readLine()
                    } else if (line.startsWith("//")) {
                        // comment
                        line = br.readLine()
                    } else {
                        col.add(arrayOf(Data(sourceFile, line)))
                        line = br.readLine()
                    }
                }
            }
            return col
        }
    }

    class Data(val sourceFile: String, val queryStr: String) {

        // --- Object ---
        override fun toString(): String {
            return this.sourceFile + ": " + this.queryStr
        }
    }

    @Test
    fun test() {
        val queryStr = this.data.queryStr
        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }



}
