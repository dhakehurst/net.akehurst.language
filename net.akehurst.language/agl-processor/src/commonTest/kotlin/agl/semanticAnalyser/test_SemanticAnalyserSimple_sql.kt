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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.syntaxAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserSimple_sql {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        val grammarStr = """
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
        """.trimIndent()
        val grammar = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!!.first()
        val typeModel by lazy {
            val result = grammarProc.process(grammarStr)
            assertNotNull(result.asm)
            assertTrue(result.issues.none { it.kind == LanguageIssueKind.ERROR }, result.issues.toString())
            TypeModelFromGrammar.create(result.asm!!.last())
        }
        val scopeModel = ScopeModelAgl.fromString(
            ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar)),
            """
                identify TableDefinition by table-id
                scope TableDefinition {
                    identify ColumnDefinition by column-id
                }
                references {
                    in Select {
                        property tableRef.ref refers-to TableDefinition
                        forall columns {
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
                        forall columns {
                            property ref refers-to ColumnDefinition from tableRef.ref
                        }
                    }
                }
            """.trimIndent()
        ).let {
            it.asm ?: error("Unable to parse ScopeModel\n${it.issues}")
        }
        val syntaxAnalyser = SyntaxAnalyserDefault(grammar.qualifiedName, typeModel, scopeModel)
        val processor = Agl.processorFromString<AsmSimple, ContextSimple>(
            grammarStr,
            Agl.configuration {
                scopeModelResolver { ProcessResultDefault(scopeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                typeModelResolver { ProcessResultDefault(typeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { ProcessResultDefault(syntaxAnalyser, IssueHolder(LanguageProcessorPhase.ALL)) }
            }
        ).processor!!
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)

        val expected = asmSimple {

        }

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
    }

    @Test
    fun select_with_references() {
        val sentence = """
            CREATE TABLE table1 (
                col1 int,
                col2 int,
                col3 varchar(255)
            );
            
            SELECT col1 FROM table1 ;
        """.trimIndent()

        val result = processor.process(sentence, Agl.options { semanticAnalysis { context(ContextSimple()) } })
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.toString())

        val expected = asmSimple {

        }

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
    }

}