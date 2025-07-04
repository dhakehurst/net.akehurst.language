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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.api.AsmList
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_SemanticAnalyserSimple_sql {

    private companion object {
        val grammarStr = GrammarString("""
namespace net.akehurst.language
grammar SQL {
    skip WS = "\s+" ;

    statementList = terminatedStatement+ ;

    terminatedStatement = statement ';' ;
    statement
        = select
        | update
        | delete
        | insert
        | tableDefinition
        ;

    select = SELECT columns FROM tableRef ;
    update = UPDATE tableRef SET columnValueList ;
    delete = DELETE FROM tableRef  ;
    insert = INSERT INTO tableRef '(' columns ')' VALUES '(' values ')' ;

    columns = [columnRefOrAny / ',']+ ;
    columnValueList = [columnValue/ ',']+ ;
    columnValue = columnRef '=' value ;

    values = [value /',']+ ;
    value
        = INTEGER
        | STRING
        ;

    tableDefinition = CREATE TABLE table-id '(' columnDefinitionList ')' ;
    columnDefinitionList = [columnDefinition / ',']+ ;
    columnDefinition = column-id datatype-ref datatype-size? ;
    datatype-size = '(' INTEGER ')' ;

    columnRefOrAny = columnAny | columnRef ;

    tableRef = REF ;
    columnRef = REF ;
    columnAny = '*' ;

    leaf table-id = ID ;
    leaf column-id = ID ;
    leaf REF = ID ;
    leaf datatype-ref = ID ;
    leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
    leaf INTEGER = "[0-9]+" ;
    leaf STRING = "'[^']*'";

    leaf CREATE = "create|CREATE" ;
    leaf TABLE  = "table|TABLE" ;
    leaf SELECT = "select|SELECT" ;
    leaf UPDATE = "update|UPDATE" ;
    leaf DELETE = "delete|DELETE" ;
    leaf INSERT = "insert|INSERT" ;
    leaf INTO   = "into|INTO"   ;
    leaf SET    = "set|SET"   ;
    leaf FROM   = "from|FROM"   ;
    leaf VALUES = "values|VALUES"   ;
}
        """.trimIndent())
        val crossReferenceModelStr = CrossReferenceString("""
            namespace net.akehurst.language.SQL
                identify TableDefinition by table-id
                scope TableDefinition {
                    identify ColumnDefinition by column-id
                }
                references {
                    in Select {
                        property tableRef.ref refers-to TableDefinition
                        forall columns of-type ColumnRef {
                            property ref refers-to ColumnDefinition from tableRef.ref
                        }
                    }
                    in Update {
                        property tableRef.ref refers-to TableDefinition
                        forall columnValueList {
                            property columnRef.ref refers-to ColumnDefinition from tableRef.ref
                        }
                    }
                    in Insert {
                        property tableRef.ref refers-to TableDefinition
                        forall columns of-type ColumnRef {
                            property ref refers-to ColumnDefinition from tableRef.ref
                        }
                    }
                }
        """.trimIndent())
        val processor = Agl.processorFromStringSimple(
            grammarDefinitionStr = (grammarStr),
            referenceStr = (crossReferenceModelStr)
        ).processor!!
        val typeModel = processor.typesModel
        val crossReferenceModel = processor.crossReferenceModel

        operator fun AsmStructure.get(value:String) = this.getProperty(PropertyValueName(value))
        fun <T> Any.ass() = this as T
    }

    @Test
    fun syntaxAnalyserPaths() {
        //TODO: this test should not really be here, it is a SyntaxAnalyser test
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT col1 FROM table1 ;
        """.trimIndent()
        val result = processor.process(sentence)

        val expected = asmSimple(typeModel = typeModel) {
            element("StatementList") {
                propertyListOfElement("terminatedStatement") {
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "TableDefinition") {
                            propertyString("create", "CREATE")
                            propertyString("table", "TABLE")
                            propertyString("table-id", "table1")
                            propertyListOfElement("columnDefinitionList") {
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col1")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col2")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col3")
                                    propertyString("datatype-ref", "varchar")
                                    propertyElementExplicitType("datatype-size", "Datatype-size") {
                                        propertyString("integer", "255")
                                    }
                                }
                            }
                        }
                    }
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "Select") {
                            propertyString("select", "SELECT")
                            propertyListOfElement("columns") {
                                element("ColumnRef") {
                                    propertyString("ref", "col1")
                                }
                            }
                            propertyString("from", "FROM")
                            propertyElementExplicitType("tableRef", "TableRef") {
                                propertyString("ref", "table1")
                            }
                        }
                    }
                }
            }
        }
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))

        //check paths
        val asmRoot = result.asm!!.root[0] as AsmStructure
        assertEquals("/0/terminatedStatement/0", asmRoot["terminatedStatement"].ass<AsmList>().elements[0].ass<AsmStructure>().parsePath.toString())
    }

    @Test
    fun check_crossReferenceModel() {
        val context = ContextFromTypeModel(processor.typesModel)
        val res = CrossReferenceModelDefault.fromString(context, crossReferenceModelStr)
        assertTrue(res.allIssues.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun tableDef() {
        val sentence = """
        CREATE TABLE table1 (
            col1 int,
            col2 int,
            col3 varchar(255)
        );
        """.trimIndent()

        val result = processor.process(sentence)

        val expected = asmSimple(typeModel = typeModel, crossReferenceModel = crossReferenceModel, context = contextAsmSimple()) {
            element("StatementList") {
                propertyListOfElement("terminatedStatement") {
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "TableDefinition") {
                            propertyString("create", "CREATE")
                            propertyString("table", "TABLE")
                            propertyString("table-id", "table1")
                            propertyListOfElement("columnDefinitionList") {
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col1")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col2")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col3")
                                    propertyString("datatype-ref", "varchar")
                                    propertyElementExplicitType("datatype-size", "Datatype-size") {
                                        propertyString("integer", "255")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun select_with_one_column_ref() {
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT col1 FROM table1 ;
        """.trimIndent()

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(contextAsmSimpleWithAsmPath()) } })

        val expected = asmSimple(typeModel = typeModel, crossReferenceModel = crossReferenceModel, context = contextAsmSimpleWithAsmPath()) {
            element("StatementList") {
                propertyListOfElement("terminatedStatement") {
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "TableDefinition") {
                            propertyString("create", "CREATE")
                            propertyString("table", "TABLE")
                            propertyString("table-id", "table1")
                            propertyListOfElement("columnDefinitionList") {
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col1")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col2")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col3")
                                    propertyString("datatype-ref", "varchar")
                                    propertyElementExplicitType("datatype-size", "Datatype-size") {
                                        propertyString("integer", "255")
                                    }
                                }
                            }
                        }
                    }
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "Select") {
                            propertyString("select", "SELECT")
                            propertyListOfElement("columns") {
                                element("ColumnRef") {
                                    reference("ref", "col1")
                                }
                            }
                            propertyString("from", "FROM")
                            propertyElementExplicitType("tableRef", "TableRef") {
                                reference("ref", "table1")
                            }
                        }
                    }
                }
            }
        }

        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun select_with_one_column_ref_fail() {
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT col7 FROM table1 ;
        """.trimIndent()

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(contextAsmSimpleWithAsmPath()) } })

        val expected = asmSimple(
            typeModel = typeModel,
            crossReferenceModel = crossReferenceModel, context = contextAsmSimple(),
            failIfIssues = false //there are failing references expected
        ) {
            element("StatementList") {
                propertyListOfElement("terminatedStatement") {
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "TableDefinition") {
                            propertyString("create", "CREATE")
                            propertyString("table", "TABLE")
                            propertyString("table-id", "table1")
                            propertyListOfElement("columnDefinitionList") {
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col1")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col2")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col3")
                                    propertyString("datatype-ref", "varchar")
                                    propertyElementExplicitType("datatype-size", "Datatype-size") {
                                        propertyString("integer", "255")
                                    }
                                }
                            }
                        }
                    }
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "Select") {
                            propertyString("select", "SELECT")
                            propertyListOfElement("columns") {
                                element("ColumnRef") {
                                    reference("ref", "col7")
                                }
                            }
                            propertyString("from", "FROM")
                            propertyElementExplicitType("tableRef", "TableRef") {
                                reference("ref", "table1")
                            }
                        }
                    }
                }
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(83, 8, 7, 4, null),
                "Reference 'col7' not resolved, to type(s) [ColumnDefinition] in scope '/table1'"
            )
        )

        assertEquals(expIssues, result.allIssues.toSet())
        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun select_with_one_column_any() {
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT * FROM table1 ;
        """.trimIndent()

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(contextAsmSimple()) } })

        assertTrue(result.allIssues.isEmpty(), result.allIssues.toString())
    }

    @Test
    fun select_with_multiple_column_refs() {
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT col1,col2,col3 FROM table1 ;
        """.trimIndent()

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(contextAsmSimple()) } })

        val expected = asmSimple(typeModel = typeModel, crossReferenceModel = crossReferenceModel, context = contextAsmSimple()) {
            element("StatementList") {
                propertyListOfElement("terminatedStatement") {
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "TableDefinition") {
                            propertyString("create", "CREATE")
                            propertyString("table", "TABLE")
                            propertyString("table-id", "table1")
                            propertyListOfElement("columnDefinitionList") {
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col1")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col2")
                                    propertyString("datatype-ref", "int")
                                    propertyString("datatype-size", null)
                                }
                                element("ColumnDefinition") {
                                    propertyString("column-id", "col3")
                                    propertyString("datatype-ref", "varchar")
                                    propertyElementExplicitType("datatype-size", "Datatype-size") {
                                        propertyString("integer", "255")
                                    }
                                }
                            }
                        }
                    }
                    element("TerminatedStatement") {
                        propertyElementExplicitType("statement", "Select") {
                            propertyString("select", "SELECT")
                            propertyListOfElement("columns") {
                                element("ColumnRef") {
                                    reference("ref", "col1")
                                }
                                element("ColumnRef") {
                                    reference("ref", "col2")
                                }
                                element("ColumnRef") {
                                    reference("ref", "col3")
                                }
                            }
                            propertyString("from", "FROM")
                            propertyElementExplicitType("tableRef", "TableRef") {
                                reference("ref", "table1")
                            }
                        }
                    }
                }
            }
        }

        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

}