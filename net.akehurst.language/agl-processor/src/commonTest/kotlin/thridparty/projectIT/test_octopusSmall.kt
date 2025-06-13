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
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_octopusSmall {

    private companion object {

        val grammarStr = """
            namespace OctopusLanguage
            grammar OctopusGrammar {
                            
            // rules for "OclPart"
            OclPart = 'OCL' 'expressions' 'for' identifier
                 OclContext* ;
            
            // rules for "UmlPart"
            UmlPart = UmlPackage ;
            
            UmlPackage = '<package>' identifier ImportedElement* IClassifier*
                 '<associations>'
                 ( __pi_super_Association ';' )*
                 '<endpackage>' ;
            
            Association = AssociationEnd '<->' AssociationEnd ;
            
            AssociationClass = VisibilityKind? '<associationclass>' identifier
                 AssociationEnd '<->' AssociationEnd ( '<attributes>' Attribute* )?
                 ( '<operations>' Operation* )?
                 ( '<states>' ( State ';' )*
                 )?
                 ( '<invariants>' ( OclExpression ';' )*
                 )?
                 '<endassociationclass>' ;
            
            AssociationEnd = VisibilityKind? __pi_reference '.' identifier MultiplicityKind ;
            
            MultiplicityKind = '[' numberLiteral ( '..' UpperBound )?
                 ']' ;
            
            NumberUpperBound = numberLiteral ;
            
            StarUpperBound = '*' ;
            
            Attribute = VisibilityKind? identifier MultiplicityKind? ':' __pi_reference ';' ( 'init' OclStatement )?
                 ( 'derive' OclStatement )? ;
            
            OclStatement = ( identifier ':' )?
                 OclExpression ;
            
            Operation = VisibilityKind? identifier '(' [ Parameter / ',' ]* ')' ( ':' __pi_reference )?
                 ';' OclPreStatement* OclPostStatement* ;
            
            Parameter = ParameterDirectionKind? identifier ':' __pi_reference ;
            
            OclPreStatement = 'pre' ( identifier ':' )?
                 OclExpression ;
            
            OclPostStatement = 'post' ( identifier ':' )?
                 OclExpression ;
            
            State = 'State' identifier
                 'subStates'
                 State*
                 'visibility' VisibilityKind ;
            
            DataType = VisibilityKind? '<datatype>' identifier ( '<specializes>' [ __pi_reference / ',' ]* )?
                 ( '<implements>' [ __pi_reference / ',' ]* )?
                 ( '<attributes>' Attribute* )?
                 ( '<operations>' Operation* )?
                 ( '<invariants>' ( OclExpression ';' )*
                 )?
                 '<enddatatype>' ;
            
            EnumerationType = VisibilityKind? '<enumeration>' identifier
                 '<values>' ( EnumLiteral ';' )*
                 '<endenumeration>' ;
            
            EnumLiteral = identifier ;
            
            PrimitiveType = 'PrimitiveType' identifier
                 'isAbstract' booleanLiteral
                 'attributes'
                 Attribute*
                 'operations'
                 Operation*
                 'navigations'
                 AssociationEnd*
                 'states'
                 State*
                 'classAttributes'
                 Attribute*
                 'classOperations'
                 Operation*
                 'invariants'
                 OclExpression*
                 'generalizations'
                 __pi_reference*
                 'subClasses'
                 __pi_reference*
                 'interfaces'
                 __pi_reference*
                 'visibility' VisibilityKind ;
            
            UmlInterface = VisibilityKind? '<interface>' identifier ( '<specializes>' [ __pi_reference / ',' ]* )?
                 ( '<attributes>' Attribute* )?
                 ( '<operations>' Operation* )?
                 ( '<invariants>' ( OclExpression ';' )*
                 )?
                 '<endinterface>' ;
            
            UmlClass = VisibilityKind? '<abstract>'? '<class>' identifier ( '<specializes>' [ __pi_reference / ',' ]* )?
                 ( '<implements>' [ __pi_reference / ',' ]* )?
                 ( '<attributes>' Attribute* )?
                 ( '<operations>' Operation* )?
                 ( '<states>' ( State ';' )*
                 )?
                 ( '<invariants>' ( OclExpression ';' )*
                 )?
                 '<endclass>' ;
            
            ImportedElement = 'ImportedElement' identifier
                 'element' IModelElement ;
            
            StructuralFeature = 'StructuralFeature' identifier
                 'multiplicity' MultiplicityKind
                 'type' __pi_reference
                 'visibility' VisibilityKind ;
            
            IClassifier = AssociationClass 
                | UmlClass 
                | __pi_super_Association 
                | __pi_super_DataType 
                | UmlInterface  ;
            
            UpperBound = NumberUpperBound 
                | StarUpperBound  ;
            
            IModelElement = Operation 
                | State 
                | __pi_super_StructuralFeature 
                | EnumLiteral 
                | ImportedElement 
                | UmlPackage 
                | Parameter 
                | OclContext 
                | VariableDeclaration  ;
            
            __pi_super_Association = Association 
                | AssociationClass  ;
            
            __pi_super_DataType = DataType 
                | EnumerationType 
                | PrimitiveType  ;
            
            __pi_super_StructuralFeature = StructuralFeature 
                | AssociationEnd 
                | Attribute  ;
            
            VisibilityKind = '+'
                | '-'
                | '#' ;
            
            ParameterDirectionKind = '<in>'
                | '<out>'
                | '<inout>' ;
            
            // common rules
            OclContext = 'context' IModelElementReference
                 'inv:' OclExpression ;
            
            IfExp = 'if' OclExpression 'then' OclExpression ( 'else' OclExpression )?
                 'endif' ;
            
            IterateExp = 'IterateExp'
                 'isMarkedPre' booleanLiteral
                 'isImplicit' booleanLiteral
                 'result' VariableDeclaration
                 'body' OclExpression
                 'iterators'
                 VariableDeclaration*
                 'source' OclExpression
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            VariableDeclaration = 'var' ':' identifier ( '=' OclExpression )? ;
            
            IteratorExp = 'IteratorExp'
                 'isMarkedPre' booleanLiteral
                 'isImplicit' booleanLiteral
                 'body' OclExpression
                 'iterators'
                 VariableDeclaration*
                 'source' OclExpression
                 'appliedProperty' PropertyCallExp?
                 'referredIterator' __pi_reference
                 'type' __pi_reference ;
            
            ModelPropertyCallExp = 'ModelPropertyCallExp'
                 'isMarkedPre' booleanLiteral
                 'isImplicit' booleanLiteral
                 'source' OclExpression
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            AttributeCallExp = __pi_reference ;
            
            NavigationCallExp = __pi_reference ;
            
            AssociationClassCallExp = __pi_reference ;
            
            AssociationEndCallExp = __pi_reference ;
            
            OperationCallExp = __pi_reference '(' [ OclExpression / ',' ]* ')' ;
            
            LetExp = 'let' [ VariableDeclaration / ',' ]*
                 'in' OclExpression ;
            
            LiteralExp = 'LiteralExp'
                 'isImplicit' booleanLiteral
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            CollectionLiteralExp = 'Collection' '\{' [ __pi_super_CollectionLiteralPart / ',' ]* '}' ;
            
            CollectionLiteralPart = 'CollectionLiteralPart' ;
            
            CollectionItem = OclExpression ;
            
            CollectionRange = OclExpression '..' OclExpression ;
            
            EnumLiteralExp = __pi_reference ;
            
            IntegerLiteralExp = numberLiteral ;
            
            OclStateLiteralExp = __pi_reference ;
            
            OclTypeLiteralExp = __pi_reference ;
            
            PrimitiveLiteralExp = 'PrimitiveLiteralExp'
                 'isImplicit' booleanLiteral
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            BooleanLiteralExp = booleanLiteral ;
            
            NumericLiteralExp = 'NumericLiteralExp'
                 'isImplicit' booleanLiteral
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            OclUndefinedLiteralExp = stringLiteral ;
            
            RealLiteralExp = numberLiteral ;
            
            StringLiteralExp = stringLiteral ;
            
            TupleLiteralExp = 'Tuple' '\{' [ VariableDeclaration / ',' ]* '}' ;
            
            OclMessageExp = '<' OclExpression '>^^' __pi_reference '(' [ OclExpression / ',' ]* ')' ;
            
            UnspecifiedValueExp = 'UnspecifiedValueExp'
                 'isImplicit' booleanLiteral
                 'appliedProperty' PropertyCallExp?
                 'type' __pi_reference ;
            
            VariableExp = __pi_reference ;
            
            IModelElementReference = 'ERROR' ; // there are no concepts that implement this interface or extend this abstract concept
            
            OclExpression = IfExp 
                | LetExp 
                | __pi_super_LiteralExp 
                | OclMessageExp 
                | UnspecifiedValueExp 
                | VariableExp 
                | PropertyCallExp  ;
            
            PropertyCallExp = LoopExp 
                | __pi_super_ModelPropertyCallExp  ;
            
            LoopExp = IterateExp 
                | IteratorExp  ;
            
            __pi_super_ModelPropertyCallExp = ModelPropertyCallExp 
                | AttributeCallExp 
                | __pi_super_NavigationCallExp 
                | OperationCallExp  ;
            
            __pi_super_NavigationCallExp = NavigationCallExp 
                | AssociationClassCallExp 
                | AssociationEndCallExp  ;
            
            __pi_super_LiteralExp = LiteralExp 
                | CollectionLiteralExp 
                | EnumLiteralExp 
                | OclStateLiteralExp 
                | OclTypeLiteralExp 
                | __pi_super_PrimitiveLiteralExp 
                | TupleLiteralExp 
                | IntegerLiteralExp 
                | RealLiteralExp 
                | StringLiteralExp  ;
            
            __pi_super_CollectionLiteralPart = CollectionLiteralPart 
                | CollectionItem 
                | CollectionRange  ;
            
            __pi_super_PrimitiveLiteralExp = PrimitiveLiteralExp 
                | NumericLiteralExp 
                | BooleanLiteralExp 
                | OclUndefinedLiteralExp  ;   
            
            __pi_reference = [ identifier / '::' ]+ ;
                    
            // white space and comments
            skip WHITE_SPACE = "\s+" ;
            skip SINGLE_LINE_COMMENT = "//[^\r\n]*" ;
            skip MULTI_LINE_COMMENT = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
                    
            // the predefined basic types   
            leaf identifier          = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            leaf stringLiteral       = "\"([^\"\\\\]|\\.)*\"";
            leaf numberLiteral       = "[0-9]+";
            leaf booleanLiteral      = 'false' | 'true';
                        
            }
        """.trimIndent()

        val processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        const val goal = "UmlPart"
    }

    @Test
    fun Book() {
        val sentence = """
            <package> Book
            
            <class> Bookpart
            <attributes> + title: String;
                         + nrOfPages: Integer;
                         - startPageNr: Integer;
            //             - /startPageNr: Integer;
            <operations> + determineStartPageNumber(): Integer;
                         + getLastPageNumber(): Integer;
                         + getNext(): Bookpart;
            <endclass>
            
            <class> Prependix  <specializes> Bookpart
            <attributes> + kind: PrependixKind;
            <operations> + needsEmptyLastPage(): Boolean;
                         + isFirstBookpart(): Boolean;
            <endclass>
            
            <class> Chapter <specializes> Bookpart
            <attributes> + author: String;
                         + subject: String;
            <operations> + determineDurationTillFinished(from: Date): Period;
            //<states> ready;
            //         inwriting;
            //         inwriting::draft;
            //         inwriting::finaldraft;
            <endclass>
            
            <class> Appendix  <specializes> Bookpart <implements> TwoColumnPart
            <attributes> + kind: AppendixKind;
                         - twoColumn: Boolean;
            <operations> + needsEmptyLastPage(): Boolean;
            <endclass>
            
            <interface> TwoColumnPart
            <operations> + transformToTwoColumn();
                         + transformToOneColumn();
            <endinterface>
            
            <datatype> Period
            <attributes> + nrOfDays: Integer;
                         + nrOfWorkingDays: Integer;
            <enddatatype>
            
            <datatype> Date
            <attributes> + day: String;
                         + month: Real;
                         + year: Integer;
                         + another : Boolean;
            <enddatatype>
            
            <enumeration> PrependixKind
            <values> contents;
                     figures;
                     tables;
                     preface;
            <endenumeration>
            
            <enumeration> AppendixKind
            <values> index;
                     bibliography;
                     glossary;
            <endenumeration>
            
            <associationclass> ChapterDependency
                               + Chapter.sourceChapter[0..1]  <-> + Chapter.dependantChapters[0..*]
            <attributes> sameAuthor: Boolean;
                         sameSubject: Boolean;
            <endassociationclass>
            
            <associations>
                + Chapter.prevChap[0..1]   <-> + Chapter.nextChap[0..1];
                + Chapter.prevChap[0..1]   <-> + Appendix.nextApp[0..1];
                + Appendix.prevApp[0..1]  <-> + Appendix.nextApp[0..1];
                + Prependix.prevPrep[0..1] <-> + Chapter.nextChap[0..1];
                + Prependix.prevPrep[0..1] <-> + Prependix.nextPrep[0..1];
            
            <endpackage>
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun trainWagon() {
        val sentence = """
            <package> trainWagon
            
            <class> Train
            <operations> 
            + unlink(w:Wagon);
            <endclass> 
            
            <class> Wagon
            <attributes> 
            + id: String;
            + bar: Boolean;
            <endclass> 
            
            <class> SleepingCar <specializes> Wagon
            <attributes> 
            + numberOfBeds: Integer;
            <endclass>
            
            <associations> 
            + Train.train[1] <-> + Wagon.wagon[0..*];
            + Wagon.pred[0..1] <-> + Wagon.succ[0..1];
            <endpackage> 
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun catalog() {
        val sentence = """
            <package> catalog
             + <class> Artist
            <attributes>
             + name: java::lang::String;
             + solo: java::lang::Boolean;
            <endclass>
             + <class> Clip
            <attributes>
             + genre: Genre;
             + duration: java::lang::Integer;
             + price: java::lang::Integer;
             + title: java::lang::String;
            <operations>
             + play(<in> i: java::lang::Integer): java::lang::Boolean;
            <endclass>
             + <enumeration> Genre
            <values>
            classic;
            folk;
            pop;
            jazz;
            <endenumeration>
            <associations>
             + Artist.artist [0..*]    <->  + Clip.clips [0..*]   ;
            <endpackage>
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun orders() {
        val sentence = """
            <package> orders
             + <class> Creditcard
            <attributes>
             + expiration: java::lang::String;
             + name: java::lang::String;
             + number: java::lang::Integer;
            <endclass>
             + <class> Order
            <attributes>
             + date: java::lang::String;
             + number: java::lang::Integer;
             + price: java::lang::Integer;
            <endclass>
             + <class> Dvd
            <attributes>
             + duration: java::lang::Integer;
             + label: java::lang::String;
             + price: java::lang::Integer;
            <endclass>
             + <class> Customer
            <attributes>
             + address: java::lang::String;
             + age: java::lang::Integer;
             + name: java::lang::String;
            <endclass>
            <associations>
             + Creditcard.creditcard [1..1]    <->  + Customer.customer [1..1]   ;
             + Customer.customer [1..1]    <->  + Order.orders [0..*]   ;
             + Order.order [1..1]    <->  + Dvd.disks [1..*]   ;
             + Dvd.dvd [0..*]    <->  + catalog::Clip.clips [1..*]   ;
            <endpackage>
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }
}