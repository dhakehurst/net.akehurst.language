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
import net.akehurst.language.agl.GrammarString
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_parserBasicProperties {

    private companion object {

        val grammarStr = """
            namespace TestParserLanguage
            grammar TestParserGrammar {
                            
            // rules for "PrimitivesTest"
            PrimitivesTest = 'PrimitivesTest' identifier
                 'prim' Prim
                 'primExtra' PrimExtra
                 'primOpt' PrimOptional
                 'primExtraOpt' PrimExtraOptional
                 'primOptPresent' PrimOptional
                 'primExtraOptPresent' PrimExtraOptional
                 'separator' PrimOptionalSeparator
                 'terminator' PrimOptionalTerminator ;
            
            Prim = 'Prim'
                 'primIdentifier' identifier
                 'primNumber' numberLiteral
                 'primString' stringLiteral
                 'primBoolean' booleanLiteral
                 'primListIdentifier' [ identifier / ',' ]*
                 'primListNumber' [ numberLiteral / ',' ]*
                 'primListString' [ stringLiteral / ',' ]*
                 'primListBoolean' [ booleanLiteral / ',' ]* ;
            
            PrimExtra = 'before' identifier 'after'
                 'before' numberLiteral 'after'
                 'before' stringLiteral 'after'
                 'before' booleanLiteral 'after'
                 'before' [ identifier / ',' ]* 'after'
                 'before' [ numberLiteral / ',' ]* 'after'
                 'before' [ stringLiteral / ',' ]* 'after'
                 'before' [ booleanLiteral / ',' ]* 'after' ;
            
            PrimOptional = 'PrimOptional'
                 'primIdentifier' identifier?
                 'primNumber' numberLiteral?
                 'primString' stringLiteral?
                 'primBoolean' booleanLiteral?
                 'primListIdentifier' [ identifier / ',' ]*
                 'primListNumber' [ numberLiteral / ',' ]*
                 'primListString' [ stringLiteral / ',' ]*
                 'primListBoolean' [ booleanLiteral / ',' ]* ;
            
            PrimExtraOptional = 'PrimExtraOptional' ( 'before' identifier 'after' )?
                 ( 'before' numberLiteral 'after' )?
                 ( 'before' stringLiteral 'after' )?
                 ( 'before' booleanLiteral 'after' )?
                 ( 'before' [ identifier / ',' ]* 'after' )?
                 ( 'before' [ numberLiteral / ',' ]* 'after' )?
                 ( 'before' [ stringLiteral / ',' ]* 'after' )?
                 ( 'before' [ booleanLiteral / ',' ]* 'after' )? ;
            
            PrimOptionalSeparator = 'PrimOptionalSeparator' ( 'before' [ identifier / ',' ]* 'after' )?
                 ( 'before' [ numberLiteral / ',' ]* 'after' )?
                 ( 'before' [ stringLiteral / ',' ]* 'after' )?
                 ( 'before' [ booleanLiteral / ',' ]* 'after' )? ;
            
            PrimOptionalTerminator = 'PrimOptionalTerminator'
                 ( 'before' ( identifier '!' )* 'after' )?
                 ( 'before' ( numberLiteral '!' )* 'after' )?
                 ( 'before' ( stringLiteral '!' )* 'after' )?
                 ( 'before' ( booleanLiteral '!' )* 'after' )? ;
            
            // rules for "WithKeywordProj"
            WithKeywordProj = 'WithKeywordProj' identifier
                 'primWith'
                 PrimWith* ;
            
            PrimWith = 'PrimWithKeywordProj' '<BOOL>'? ;
            
            // rules for "LimitedTest"
            LimitedTest = 'LimitedTest' identifier
                 'limitedNonOpt' LimitedNonOptional
                 'limitedOpt' LimitedOptional
                 'limitedOpt2' LimitedOptional ;
            
            LimitedNonOptional = 'LimitedNonOptional'
                 VisibilityKind ';'
                 [ VisibilityKind / ',' ]* ';'
                 'before' VisibilityKind 'after' ';'
                 'before' [ VisibilityKind / ',' ]* 'after' ';'
                 OtherLimited ';'
                 [ OtherLimited / ',' ]* ';' ;
            
            LimitedOptional = 'LimitedOptional' ( VisibilityKind ';' )?
                 ( [ VisibilityKind / ',' ]* ';' )?
                 ( 'before' VisibilityKind 'after' ';' )?
                 ( 'before' [ VisibilityKind / ',' ]* 'after' ';' )? ;
            
            OtherLimited = identifier ;
            
            // rules for "PartsTest"
            PartsTest = 'PartsTest' identifier
                 'directParts' WithDirectParts
                 'partsOfParts' WithSubSubParts? ;
            
            WithDirectParts = 'WithDirectParts' 'part' PartConcept ( 'optPart' PartConcept )?
                 'partList' [ PartConcept / ',' ]* ( 'optList' [ PartConcept / ',' ]* )? ;
            
            PartConcept = 'PartConcept'
                 'content' stringLiteral
                 'optContent' numberLiteral?
                 'visibility' VisibilityKind ;
            
            WithSubSubParts = 'WithSubSubParts'
                 'part' SubConcept
                 'optPart' SubConcept?
                 'partList'
                 SubConcept*
                 'optList'
                 SubConcept* ;
            
            SubConcept = 'SubConcept'
                 'normalSub' WithDirectParts
                 'optionalSub' WithDirectParts?
                 'listSub'
                 WithDirectParts*
                 'optListSub'
                 WithDirectParts* ;
            
            // rules for "RefsTest"
            RefsTest = 'RefsTest' identifier
                 'directRefs' WithDirectRefs
                 'indirectRefs' WithSubSubRefs?
                 'withSeparator' RefsWithSeparator? ;
            
            WithDirectRefs = 'WithDirectRefs'
                 'ref' __pi_reference
                 'optRef' __pi_reference?
                 'refList'
                 __pi_reference*
                 'optRefList'
                 __pi_reference* ;
            
            WithSubSubRefs = 'WithSubSubRefs'
                 'part' WithDirectRefs
                 'optPart' WithDirectRefs?
                 'partList'
                 WithDirectRefs*
                 'optList'
                 WithDirectRefs* ;
            
            RefsWithSeparator = [ WithDirectRefs / '!' ]* [ WithDirectRefs / '!!' ]* ;
            
            // common rules
            VisibilityKind = '+'
                | '-'
                | '#' ;   
            
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

        val processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!

    }

    @Test
    fun lim_sentence1() {
        val sentence = """
            LimitedTest anyNameWillDo
                limitedNonOpt LimitedNonOptional
                    +;
                    + , - , #;
                    before # after ;
                    before #,
                    +,
                    -
                    after;
                    FIRST ;
                    SECOND, FIRST, FIRST, SECOND, SECOND  ;
                limitedOpt LimitedOptional
                    +;
                    + , - , #;
                    before # after ;
                    before #, +, - after;
            
                limitedOpt2 LimitedOptional
                    +;
                    + , - , #;
                    before # after ;
                    before #, +, - after;
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("LimitedTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun par_sentence1() {
        val sentence = """
            PartsTest myName
                directParts WithDirectParts
                                part PartConcept
                                            content "stringLiteral"
                                            optContent 10
                                            visibility +
                                partList
                                    PartConcept
                                        content "otherString"
                                        optContent
                                        visibility #  ,
                                    PartConcept
                                        content "stringLiteralXXX"
                                        optContent 123
                                        visibility -  ,
                                    PartConcept
                                        content "stringLiteralYYY"
                                        optContent
                                        visibility +
                partsOfParts
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PartsTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun par_sentence2() {
        val sentence = """
PartsTest myName
	directParts WithDirectParts
                  	part PartConcept
                            	content "stringLiteral"
                            	optContent 10
                            	visibility +
                  	partList
                        PartConcept
                            content "otherString"
                            optContent
                            visibility # ,
                        PartConcept
                            content "stringLiteralXXX"
                            optContent 123
                            visibility - ,
                        PartConcept
                            content "stringLiteralYYY"
                            optContent
                            visibility +
    partsOfParts
        WithSubSubParts
        	part SubConcept
                    	normalSub WithDirectParts
                    	                  	part PartConcept
                                                    	content "stringLiteral"
                                                    	optContent 10
                                                    	visibility +
                                          	partList
                    	optionalSub
                    	listSub
                            WithDirectParts
                                part PartConcept
                                            content "stringLiteralFGT"
                                            optContent 10
                                            visibility +
                                partList
                                    PartConcept
                                        content "otherStringZZZ"
                                        optContent
                                        visibility #
                            WithDirectParts
                                part PartConcept
                                            content "stringLiteralZZZ"
                                            optContent 1045
                                            visibility -
                                partList
                                    PartConcept
                                        content "otherString"
                                        optContent
                                        visibility #
                    	optListSub
        	optPart
        	partList
                SubConcept
                    normalSub WithDirectParts
                                    part PartConcept
                                                content "stringLiteral"
                                                optContent 10
                                                visibility +
                                    partList
                                        PartConcept
                                            content "otherString"
                                            optContent
                                            visibility # ,
                                        PartConcept
                                            content "stringLiteralXXX"
                                            optContent 123
                                            visibility - ,
                                        PartConcept
                                            content "stringLiteralYYY"
                                            optContent
                                            visibility +
                    optionalSub WithDirectParts
                                    part PartConcept
                                                content "stringLiteral"
                                                optContent 10
                                                visibility +
                                    partList
                                        PartConcept
                                            content "otherString"
                                            optContent
                                            visibility # ,
                                        PartConcept
                                            content "stringLiteralXXX"
                                            optContent 123
                                            visibility - ,
                                        PartConcept
                                            content "stringLiteralYYY"
                                            optContent
                                            visibility +
                    listSub
                    WithDirectParts
                        part PartConcept
                            content "stringLiteral"
                            optContent 10
                            visibility +
                        partList
                    optListSub
                SubConcept
                    normalSub WithDirectParts
                        part PartConcept
                            content "stringLiteral"
                            optContent 10
                            visibility +
                        partList
                    optionalSub WithDirectParts
                        part PartConcept
                            content "stringLiteral"
                            optContent 10
                            visibility #
                        partList
                    listSub
                        WithDirectParts
                            part PartConcept
                                content "stringLiteral"
                                optContent 10
                                visibility +
                            partList
                    optListSub
                        WithDirectParts
                            part PartConcept
                                content "stringLit"
                                optContent 1440
                                visibility +
                            partList

            optList
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PartsTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun par_sentence3() {
        val sentence = """
            PartsTest myName
                directParts WithDirectParts
                                part PartConcept
                                            content "stringLiteral"
                                            optContent 10
                                            visibility +
                                optPart
                                    PartConcept
                                        content "stringLiteral"
                                        optContent 10
                                        visibility +
                                partList
                                    PartConcept
                                        content "otherString"
                                        optContent
                                        visibility #  ,
                                    PartConcept
                                        content "stringLiteralXXX"
                                        optContent 123
                                        visibility -  ,
                                    PartConcept
                                        content "stringLiteralYYY"
                                        optContent
                                        visibility +
                                optList
                                    PartConcept
                                        content "stringLiteral"
                                        optContent 10
                                        visibility +  ,
                                    PartConcept
                                        content "stringLiteralYYY"
                                        optContent
                                        visibility +
                partsOfParts
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PartsTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun pri_sentence1() {
        val sentence = """
            PrimitivesTest someName
            prim 
                Prim  
                    primIdentifier EenNaam
                    primNumber 19
                    primString "TEKST"
                    primBoolean true
                    primListIdentifier EenNaam, NogEenNaam, EnNogEen
                    primListNumber 10 , 20
                    primListString "TEKST" , "TEKST"
                    primListBoolean true, false, true, false
            primExtra
                before EenNaam after
                before 45 after
                before "TEKST" after
                before false  after
                before EenNaam , NogEenNaam, EnNogEen after
                before 67 , 98  after
                before "TEKST", "TEXT"  after
                before false, true, false  after
            primOpt PrimOptional
                primIdentifier // not present
                primNumber // not present
                primString // not present
                primBoolean // not present
                primListIdentifier // not present
                primListNumber // not present
                primListString // not present
                primListBoolean // not present
            primExtraOpt PrimExtraOptional // not present
            primOptPresent PrimOptional
                primIdentifier // not present
                primNumber // not present
                primString // not present
                primBoolean // not present
                primListIdentifier // not present
                primListNumber // not present
                primListString // not present
                primListBoolean // not present
            primExtraOptPresent PrimExtraOptional
                before EenNaam after
                before 45 after
                before "TEKST" after
                before false  after
                before EenNaam , NogEenNaam, EnNogEen after
                before 67 , 98  after
                before "TEKST", "TEXT"  after
                before false, true, false  after
            separator PrimOptionalSeparator 
                before EenNaam , NogEenNaam, EnNogEen after
                before 56, 67, 78 after
                before "iets", "wat" after
                before true, false after
            terminator PrimOptionalTerminator
                before EenNaam ! NogEEN ! EnNogEeen ! after
                before 30 ! 40 ! 50 ! 60 ! after
                before "iets" ! "wat" !  after
                before true! false! true! false!	 after
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("PrimitivesTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun ref_sentence1() {
        val sentence = """
            RefsTest myName
                directRefs WithDirectRefs
                                ref someName::otherName::xxx::yyy
                                optRef // not present
                                refList
                                    name1::ff::gg::yuio
                                    name2
                                    name3
                                    name4
                                optRefList // not present
                indirectRefs
                withSeparator WithDirectRefs
                                                    ref someName::otherName::xxx::yyy
                                                    optRef // not present
                                                    refList
                                                      name1::ff::gg::yuio
                                                      name2
                                                      name3
                                                      name4
                                                  optRefList // not present
                                                  !
                                WithDirectRefs
                                    ref someName::otherName::xxx::yyy
                                    optRef // not present
                                    refList
                                        name1::ff::gg::yuio
                                        name2
                                    optRefList // not present
                                WithDirectRefs
                                    ref someName::otherName::xxx::yyy
                                    optRef // not present
                                    refList
                                        name1::ff::gg::yuio
                                        name2
                                    optRefList // not present
                                    !!
                                WithDirectRefs
                                    ref someName::otherName::xxx::yyy
                                    optRef // not present
                                    refList
                                        name1::ff::gg::yuio
                                        name2
                                    optRefList // not present
                                    !!
                                WithDirectRefs
                                    ref someName::otherName::xxx::yyy
                                    optRef // not present
                                    refList
                                        name1::ff::gg::yuio
                                        name2
                                    optRefList // not present
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("RefsTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun ref_sentence2() {
        val sentence = """
            RefsTest myName
                directRefs WithDirectRefs
                                ref someName::x34565
                                optRef nameA
                                refList
                                    name1::d45fgh
                                    name2
                                    name3
                                    name4
                                optRefList nameZ
                                nameY::sdfsgargh::HERA
                                nameX
                indirectRefs WithSubSubRefs 
                                part WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                             name1::WREQWR::gag
                                                             name2
                                                             name3::fghy
                                                             name4
                                                         optRefList nameZ
                                                         nameY::h56
                                                         nameX
                                optPart WithDirectRefs
                                                            ref someName
                                                            optRef nameA
                                                            refList
                                                                name1
                                                                name2
                                                                name3
                                                                name4
                                                            optRefList nameZ
                                                            nameY
                                                            nameX
                                        WithDirectRefs
                                                            ref someName
                                                            optRef nameA
                                                            refList
                                                                name1
                                                                name2
                                                                name3
                                                                name4
                                                            optRefList nameZ
                                                            nameY
                                                            nameX
                                partList 
                                    WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                            name1
                                                            name2
                                                            name3
                                                            name4
                                                        optRefList nameZ
                                                        nameY
                                                        nameX
                                    WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                            name1
                                                            name2
                                                            name3
                                                            name4
                                                        optRefList nameZ
                                                        nameY
                                                        nameX
                                optList 
                                    WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                            name1
                                                            name2
                                                            name3
                                                            name4
                                                        optRefList nameZ
                                                        nameY
                                                        nameX
            withSeparator
                                    WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                            name1
                                                            name2
                                                            name3
                                                            name4
                                                        optRefList nameZ
                                                        nameY
                                                        nameX
                                                        !!
                                    WithDirectRefs
                                                        ref someName
                                                        optRef nameA
                                                        refList
                                                            name1
                                                            name2
                                                            name3
                                                            name4
                                                        optRefList nameZ
                                                        nameY
                                                        nameX
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("RefsTest") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun wit_sentence1() {
        val sentence = """
            WithKeywordProj myName
                primWith
                    PrimWithKeywordProj <BOOL>
                    PrimWithKeywordProj
                    PrimWithKeywordProj <BOOL>
                    PrimWithKeywordProj
                    PrimWithKeywordProj <BOOL>
                    PrimWithKeywordProj
                    PrimWithKeywordProj
                    PrimWithKeywordProj <BOOL>
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName("WithKeywordProj") })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }
}