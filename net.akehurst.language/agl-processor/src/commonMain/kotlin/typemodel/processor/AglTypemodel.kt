/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.processor

import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel

object AglTypemodel : LanguageObjectAbstract<TypeModel, ContextFromGrammar>() {

    const val goalRuleName = "unit"

    override val grammarString = """namespace net.akehurst.language
  grammar Typemodel : Base {
    override definition
      = singletonDefinition
      | primitiveDefinition
      | enumDefinition
      | valueDefinition
      | collectionDefinition
      | dataDefinition
      | interfaceDefinition
      | unionDefinition
      ;
    singletonDefinition = 'singleton' IDENTIFIER ;
    primitiveDefinition = 'primitive' IDENTIFIER ;
    enumDefinition = 'enum' IDENTIFIER ;
    valueDefinition = 'value' IDENTIFIER ;
    collectionDefinition = 'collection' IDENTIFIER '<' typeParameterList '>' ;
    dataDefinition = 'data' IDENTIFIER supertypes? '{' property* '}' ;
    interfaceDefinition = 'interface' IDENTIFIER supertypes? '{' property* '}' ;
    unionDefinition = 'union' IDENTIFIER '{' alternatives '}' ;
    alternatives = [typeReference / '|']+ ;
    
    typeParameterList = [ IDENTIFIER / ',']+ ;
    supertypes = ':' [ typeReference / ',']+ ;
    property = characteristic IDENTIFIER ':' typeReference ;
    typeReference = possiblyQualifiedName typeArgumentList? '?'?;
    typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    characteristic
       = 'reference-val'    // reference, constructor argument
       | 'reference-var'    // reference mutable property
       | 'composite-val'    // composite, constructor argument
       | 'composite-var'    // composite mutable property
       | 'dis'    // disregard / ignore
       ;

  }"""
    override val kompositeString = """namespace net.akehurst.language.typemodel.api
    interface TypeInstance {
        cmp typeArguments
    }
    interface TypeDeclaration {
        cmp supertypes
        cmp typeParameters
    }
    interface ValueType {
        cmp constructors
    }
    interface DataType {
        cmp constructors
    }
    interface UnnamedSupertypeType {
        cmp subtypes
    }
    interface PropertyDeclaration {
        cmp typeInstance
    }
    interface ConstructorDeclaration {
        cmp parameters
    }
    interface MethodDeclaration {
        cmp parameters
    }
    interface ParameterDeclaration {
        cmp typeInstance
    }

namespace net.akehurst.language.typemodel.asm
    class TypeNamespaceAbstract {
        cmp ownedUnnamedSupertypeType
        cmp ownedTupleTypes
    }
    class TypeDeclarationSimpleAbstract {
        cmp propertyByIndex
    }
    class TypeInstanceSimple {
        // must be explicitly cmp because it is declared an interface
        cmp qualifiedOrImportedTypeName
    }

namespace net.akehurst.language.grammarTypemodel.api
    interface GrammarTypeNamespace {
        cmp allRuleNameToType
    }

"""

    override val grammarModel: GrammarModel by lazy {
        grammarModel("TypeModel") {
            namespace("net.akehurst.language") {
                grammar("Typemodel") {
                    extendsGrammar(AglBase.targetGrammar.selfReference)

                    concatenation("unit") { ref("namespace"); lst(1, -1) { ref("definition") } }
                    choice("definition") {
                        ref("singletonDefinition")
                        ref("primitiveDefinition")
                        ref("enumDefinition")
                        ref("valueDefinition")
                        ref("collectionDefinition")
                        ref("dataDefinition")
                        ref("interfaceDefinition")
                        ref("unionDefinition")
                    }
                    concatenation("singletonDefinition") { lit("singleton"); ref("IDENTIFIER") }
                    concatenation("primitiveDefinition") { lit("primitive"); ref("IDENTIFIER") }
                    concatenation("enumDefinition") { lit("enum"); ref("IDENTIFIER") }
                    concatenation("valueDefinition") { lit("value"); ref("IDENTIFIER") }
                    //concatenation("collectionDefinition") { lit("collection"); ref("IDENTIFIER") '<' typeParameterList '>' ; }
                    //concatenation("dataDefinition") { lit("data"); ref("IDENTIFIER") supertypes ? '{' property* '}' ; }
                    //concatenation("interfaceDefinition") { lit("interface"); ref("IDENTIFIER") supertypes ? '{' property* '}' ; }
                   // concatenation("unionDefinition") { lit("union"); ref("IDENTIFIER") '{' alternatives* '}' ; }
                }
            }
        }
    }

    override val typeModel by lazy {
        typeModel("Typemodel", true, AglGrammar.typeModel.namespace) {
            namespace("net.akehurst.language.typemodel.api", listOf("std", "net.akehurst.language.base.api")) {
                enumType("PropertyCharacteristic", listOf("REFERENCE", "COMPOSITE", "READ_ONLY", "READ_WRITE", "STORED", "DERIVED", "PRIMITIVE", "CONSTRUCTOR", "IDENTITY"))
                valueType("PropertyName") {

                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                valueType("ParameterName") {

                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                valueType("MethodName") {

                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                interfaceType("ValueType") {
                    supertype("StructuredType")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                }
                interfaceType("UnnamedSupertypeType") {
                    supertype("TypeDeclaration")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                interfaceType("TypeParameter") {

                }
                interfaceType("TypeNamespace") {
                    supertype("Namespace") { ref("TypeDeclaration") }
                }
                interfaceType("TypeModel") {
                    supertype("Model") { ref("TypeNamespace"); ref("TypeDeclaration") }
                }
                interfaceType("TypeInstance") {

                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                }
                interfaceType("TypeDeclaration") {
                    supertype("Definition") { ref("TypeDeclaration") }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "supertypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeParameters", "List", false) {
                        typeArgument("TypeParameter")
                    }
                }
                interfaceType("TypeArgumentNamed") {
                    supertype("TypeArgument")
                }
                interfaceType("TypeArgument") {

                }
                interfaceType("TupleTypeInstance") {
                    supertype("TypeInstance")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgumentNamed")
                    }
                }
                interfaceType("TupleType") {
                    supertype("TypeDeclaration")
                }
                interfaceType("StructuredType") {
                    supertype("TypeDeclaration")
                }
                interfaceType("SingletonType") {
                    supertype("TypeDeclaration")
                }
                interfaceType("PropertyDeclarationResolved") {
                    supertype("PropertyDeclaration")
                }
                interfaceType("PropertyDeclaration") {

                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                interfaceType("PrimitiveType") {
                    supertype("TypeDeclaration")
                }
                interfaceType("ParameterDeclaration") {

                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                interfaceType("MethodDeclaration") {

                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                interfaceType("InterfaceType") {
                    supertype("StructuredType")
                }
                interfaceType("EnumType") {
                    supertype("TypeDeclaration")
                }
                interfaceType("DataType") {
                    supertype("StructuredType")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                }
                interfaceType("ConstructorDeclaration") {

                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                interfaceType("CollectionType") {
                    supertype("StructuredType")
                }
            }
            namespace("net.akehurst.language.typemodel.asm", listOf("net.akehurst.language.typemodel.api", "net.akehurst.language.base.api", "std", "net.akehurst.language.base.asm")) {
                singleton("TypeParameterMultiple")
                singleton("SimpleTypeModelStdLib")
                dataType("ValueTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("ValueType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                }
                dataType("UnnamedSupertypeTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("UnnamedSupertypeType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("id", "Integer", false)
                        parameter("subtypes", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "id", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                dataType("UnnamedSupertypeTypeInstance") {
                    supertype("TypeInstanceAbstract")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("declaration", "UnnamedSupertypeType", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "declaration", "UnnamedSupertypeType", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isNullable", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                }
                dataType("TypeParameterSimple") {
                    supertype("TypeParameter")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                }
                dataType("TypeParameterReference") {
                    supertype("TypeInstanceAbstract")
                    supertype("TypeInstance")
                    constructor_ {
                        parameter("context", "TypeDeclaration", false)
                        parameter("typeParameterName", "SimpleName", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "context", "TypeDeclaration", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isNullable", "Boolean", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "typeOrNull", "TypeDeclaration", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeParameterName", "SimpleName", false)
                }
                dataType("TypeNamespaceSimple") {
                    supertype("TypeNamespaceAbstract")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
                }
                dataType("TypeNamespaceAbstract") {
                    supertype("TypeNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.typemodel.api.TypeDeclaration") }
                    constructor_ {
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "ownedTupleTypes", "List", false) {
                        typeArgument("TupleType")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "ownedUnnamedSupertypeType", "List", false) {
                        typeArgument("UnnamedSupertypeType")
                    }
                }
                dataType("TypeModelSimpleAbstract") {
                    supertype("TypeModel")
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "namespace", "List", false) {
                        typeArgument("TypeNamespace")
                    }
                }
                dataType("TypeModelSimple") {
                    supertype("TypeModelSimpleAbstract")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                }
                dataType("TypeInstanceSimple") {
                    supertype("TypeInstanceAbstract")
                    constructor_ {
                        parameter("contextQualifiedTypeName", "QualifiedName", false)
                        parameter("namespace", "TypeNamespace", false)
                        parameter("qualifiedOrImportedTypeName", "PossiblyQualifiedName", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "contextQualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isNullable", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedOrImportedTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                }
                dataType("TypeInstanceAbstract") {
                    supertype("TypeInstance")
                    constructor_ {}
                }
                dataType("TypeDeclarationSimpleAbstract") {
                    supertype("TypeDeclaration")
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "metaInfo", "Map", false) {
                        typeArgument("String")
                        typeArgument("String")
                    }
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "method", "List", false) {
                        typeArgument("MethodDeclaration")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "propertyByIndex", "Map", false) {
                        typeArgument("Integer")
                        typeArgument("PropertyDeclaration")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "supertypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeParameters", "List", false) {
                        typeArgument("TypeParameter")
                    }
                }
                dataType("TypeArgumentSimple") {
                    supertype("TypeArgument")
                    constructor_ {
                        parameter("type", "TypeInstance", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "type", "TypeInstance", false)
                }
                dataType("TypeArgumentNamedSimple") {
                    supertype("TypeArgumentNamed")
                    constructor_ {
                        parameter("name", "PropertyName", false)
                        parameter("type", "TypeInstance", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "type", "TypeInstance", false)
                }
                dataType("TupleTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("TupleType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeParameters", "List", false) {
                        typeArgument("TypeParameterMultiple")
                    }
                }
                dataType("TupleTypeInstanceSimple") {
                    supertype("TypeInstanceAbstract")
                    supertype("TupleTypeInstance")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isNullable", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeArguments", "List", false) {
                        typeArgument("TypeArgumentNamed")
                    }
                }
                dataType("StructuredTypeSimpleAbstract") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("StructuredType")
                    constructor_ {}
                }
                dataType("SpecialTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                }
                dataType("SingletonTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("SingletonType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                }
                dataType("PropertyDeclarationStored") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "StructuredType", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("characteristics", "Set", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "characteristics", "Set", false) {
                        typeArgument("PropertyCharacteristic")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "description", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "index", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "StructuredType", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                dataType("PropertyDeclarationResolvedSimple") {
                    supertype("PropertyDeclarationAbstract")
                    supertype("PropertyDeclarationResolved")
                    constructor_ {
                        parameter("owner", "TypeDeclaration", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("characteristics", "Set", false)
                        parameter("description", "String", false)
                    }
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "characteristics", "Set", false) {
                        typeArgument("PropertyCharacteristic")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "description", "String", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "TypeDeclaration", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                dataType("PropertyDeclarationPrimitive") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "TypeDeclaration", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("description", "String", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "description", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "index", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "TypeDeclaration", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                dataType("PropertyDeclarationDerived") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "TypeDeclaration", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("description", "String", false)
                        parameter("expression", "String", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "description", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "expression", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "index", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "TypeDeclaration", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                dataType("PropertyDeclarationAbstract") {
                    supertype("PropertyDeclaration")
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "metaInfo", "Map", false) {
                        typeArgument("String")
                        typeArgument("String")
                    }
                }
                dataType("PrimitiveTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("PrimitiveType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                }
                dataType("ParameterDefinitionSimple") {
                    supertype("ParameterDeclaration")
                    constructor_ {
                        parameter("name", "ParameterName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("defaultValue", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "defaultValue", "String", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "ParameterName", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeInstance", "TypeInstance", false)
                }
                dataType("MethodDeclarationDerived") {
                    supertype("MethodDeclaration")
                    constructor_ {
                        parameter("owner", "TypeDeclaration", false)
                        parameter("name", "MethodName", false)
                        parameter("parameters", "List", false)
                        parameter("description", "String", false)
                        parameter("body", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "body", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "description", "String", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "MethodName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "TypeDeclaration", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                dataType("InterfaceTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("InterfaceType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                dataType("EnumTypeSimple") {
                    supertype("TypeDeclarationSimpleAbstract")
                    supertype("EnumType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("literals", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "literals", "List", false) {
                        typeArgument("String")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                }
                dataType("DataTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("DataType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                dataType("ConstructorDeclarationSimple") {
                    supertype("ConstructorDeclaration")
                    constructor_ {
                        parameter("owner", "TypeDeclaration", false)
                        parameter("parameters", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "owner", "TypeDeclaration", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                dataType("CollectionTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("CollectionType")
                    constructor_ {
                        parameter("namespace", "TypeNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("typeParameters", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "TypeNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "typeParameters", "List", false) {
                        typeArgument("TypeParameter")
                    }
                }
            }
            namespace("net.akehurst.language.grammarTypemodel.api", listOf("net.akehurst.language.typemodel.api", "std", "net.akehurst.language.grammar.api")) {
                interfaceType("GrammarTypeNamespace") {
                    supertype("TypeNamespace")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "allRuleNameToType", "Map", false) {
                        typeArgument("GrammarRuleName")
                        typeArgument("TypeInstance")
                    }
                }
            }
            namespace(
                "net.akehurst.language.grammarTypemodel.asm",
                listOf(
                    "net.akehurst.language.base.api",
                    "std",
                    "net.akehurst.language.typemodel.asm",
                    "net.akehurst.language.grammarTypemodel.api",
                    "net.akehurst.language.grammar.api",
                    "net.akehurst.language.typemodel.api"
                )
            ) {
                dataType("GrammarTypeNamespaceSimple") {
                    supertype("GrammarTypeNamespaceAbstract")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
                }
                dataType("GrammarTypeNamespaceAbstract") {
                    supertype("TypeNamespaceAbstract")
                    supertype("GrammarTypeNamespace")
                    constructor_ {
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "allRuleNameToType", "Map", false) {
                        typeArgument("GrammarRuleName")
                        typeArgument("TypeInstance")
                    }
                }
            }
        }
    }

    override val asmTransformModel: TransformModel get() = TODO("not implemented")

    override val crossReferenceModel: CrossReferenceModel get() = TODO("not implemented")

    override val styleModel: AglStyleModel get() = TODO("not implemented")

    override val formatModel: AglFormatModel get() = TODO("not implemented")


    val targetGrammar get() = grammarModel.findDefinitionOrNullByQualifiedName(QualifiedName("net.akehurst.language.TypeModel"))!!
}