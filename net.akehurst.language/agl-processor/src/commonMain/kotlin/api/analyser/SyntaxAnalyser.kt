/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.syntaxAnalyser

import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sppt.api.ParsePath
import net.akehurst.language.sppt.api.SharedPackedParseTree

/**
 * stateless set of functions that construct elements of an ASM
 */
interface AsmFactory<AsmType : Any, AsmValueType : Any> : ObjectGraph<AsmValueType> {

    fun constructAsm(): AsmType
    fun rootList(asm: AsmType): List<AsmValueType>
    fun addRoot(asm: AsmType, root: AsmValueType): Boolean
    fun removeRoot(asm: AsmType, root: AsmValueType): Boolean

    //fun toTypedObject(self: AsmValueType, selfType: TypeInstance): TypedObject<AsmValueType>
    //fun nothingValue(): AsmValueType
    //fun anyValue(value: Any): AsmValueType
    //fun primitiveValue(qualifiedTypeName: QualifiedName, value: Any): AsmValueType
    //fun listOfValues(elements: List<AsmValueType>): AsmValueType
    // fun listOfSeparatedValues(elements: ListSeparated<AsmValueType, AsmValueType, AsmValueType>): AsmValueType

    //fun constructStructure(qualifiedTypeName: QualifiedName, vararg args:Any): AsmStructureType
    //fun setProperty(self: AsmStructureType, index: Int, propertyName: String, value: AsmValueType)

}

interface AsmWalker<AsmType : Any, AsmValueType : Any, AsmStructureType : AsmValueType, PropertyType, ListValueType> {
    fun beforeRoot(root: AsmValueType)
    fun afterRoot(root: AsmValueType)
    fun onNothing(owningProperty: PropertyType?, value: AsmValueType)
    fun onPrimitive(owningProperty: PropertyType?, value: AsmValueType)
    fun beforeStructure(owningProperty: PropertyType?, value: AsmStructureType)
    fun onProperty(owner: AsmStructureType, property: PropertyType)
    fun afterStructure(owningProperty: PropertyType?, value: AsmStructureType)
    fun beforeList(owningProperty: PropertyType?, value: ListValueType)
    fun afterList(owningProperty: PropertyType?, value: ListValueType)
}

interface LocationMap {
    fun clear()
    fun add(path: ParsePath, obj: Any, location: InputLocation)
    operator fun get(obj: Any?): InputLocation?
    fun getByPath(obj: Any?, path: ParsePath): InputLocation?
}

/**
 *
 * A Syntax Analyser converts a Parse Tree (in this case a SharedPackedParseTree) into a "Syntax Tree/Model".
 * i.e. it will map the parse tree to some other data structure that abstracts away unwanted concrete syntax information
 * e.g. as whitesapce
 *
 */
interface SyntaxAnalyser<AsmType : Any> { //TODO: make transform type argument here maybe!

    /**
     * Map of ASM items to an InputLocation. Should contain content after 'process' is called
     */
    val locationMap: LocationMap

    /**
     * map of Extends GrammarName -> SyntaxAnalyser for extended Language
     */
    val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>>

    /**
     * map of Embedded GrammarName -> SyntaxAnalyser for embedded Language
     */
    val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>>

    /**
     * reset the sppt2ast, clearing any cached values
     */
    // SyntaxAnalyser<*> not exportable - * projections not exportable? TODO: check this
    fun <T : Any> clear(done: Set<SyntaxAnalyser<T>> = emptySet())

    /**
     * configure the SyntaxAnalyser
     */
    //fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any> = emptyMap()): List<LanguageIssue>

    /**
     * map the tree into an instance of the targetType
     *
     */
    fun transform(sppt: SharedPackedParseTree): SyntaxAnalysisResult<AsmType>
}
