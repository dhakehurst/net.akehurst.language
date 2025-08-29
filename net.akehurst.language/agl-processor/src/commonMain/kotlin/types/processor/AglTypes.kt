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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.format.builder.formatDomain
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

object AglTypes : LanguageObjectAbstract<TypesDomain, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Types"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) }

    override val grammarString = """
        namespace $NAMESPACE_NAME
          grammar $NAME : Base {
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
            enumDefinition = 'enum' IDENTIFIER enumLiterals? ;
            enumLiterals = '{' [IDENTIFIER / ',']+ '}' ;
            valueDefinition = 'value' IDENTIFIER supertypes? '(' constructorParameter ')' ;
            collectionDefinition = 'collection' IDENTIFIER typeParameterList ;
            dataDefinition =
              'data' IDENTIFIER typeParameterList? supertypes? '{'
                constructor*
                property*
              '}'
            ;
            interfaceDefinition = 'interface' IDENTIFIER typeParameterList? supertypes? interfaceBody? ;
            interfaceBody = '{' property* '}' ;
            unionDefinition = 'union' IDENTIFIER '{' alternatives '}' ;
            alternatives = [typeReference / '|']+ ;
            typeParameterList = '<' [IDENTIFIER / ',']+ '>' ;
            supertypes = ':' [typeReference / ',']+ ;
            constructor = 'constructor' '(' constructorParameter* ')' ;
            constructorParameter = cmp_ref? val_var? IDENTIFIER ':' typeReference ;
            property = cmp_ref? val_var IDENTIFIER ':' typeReference ;
            typeReference = possiblyQualifiedName typeArgumentList? '?'?;
            typeArgumentList = '<' [ typeReference / ',']+ '>' ;
            cmp_ref = 'cmp' | 'ref' ;
            val_var = 'val' | 'var' ;
          }
      """.trimIndent()

    override val typesString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    // TODO: This is only used when generating the typesModel I think, do we need it here ? but not sure where else to put it!
    override val kompositeString = """
        namespace net.akehurst.language.types.api
            interface TypeInstance {
                cmp typeArguments
            }
            interface TypeDefinition {
                cmp supertypes
                cmp typeParameters
            }
            interface ValueType {
                cmp constructors
            }
            interface DataType {
                cmp constructors
            }
            interface UnionType {
                cmp alternatives
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

        namespace net.akehurst.language.types.asm
            class TypesNamespaceAbstract {
                cmp ownedUnnamedSupertypeType
                cmp ownedTupleTypes
            }
            class TypeDefinitionSimpleAbstract {
                cmp propertyByIndex
            }
            class TypeInstanceSimple {
                // must be explicitly cmp because it is declared an interface
                cmp qualifiedOrImportedTypeName
            }
        
        namespace net.akehurst.language.grammarTypemodel.api
            interface GrammarTypesNamespace {
                cmp allRuleNameToType
            }
    """.trimIndent()

    override val asmTransformString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace ${NAMESPACE_NAME}
            // TODO
    """.trimIndent()

    override val styleString = """
        namespace ${NAMESPACE_NAME}
          styles ${NAME} {
            $$ "'[^']+'" {
              foreground: darkgreen;
              font-weight: bold;
            }
          }        
    """

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val grammarDomain by lazy {
        grammarDomain(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglBase.defaultTargetGrammar.selfReference)
                    choice("definition", overrideKind = OverrideKind.REPLACE) {
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
                    concatenation("enumDefinition") { lit("enum"); ref("IDENTIFIER"); opt { ref("enumLiterals") } }
                    concatenation("enumLiterals") { lit("{"); spLst(1, -1) { ref("IDENTIFIER"); lit(",") }; lit("}") }
                    concatenation("valueDefinition") { lit("value"); ref("IDENTIFIER"); opt { ref("supertypes") }; lit("("); ref("constructorParameter"); lit(")") }
                    concatenation("collectionDefinition") { lit("collection"); ref("IDENTIFIER"); ref("typeParameterList") }
                    concatenation("dataDefinition") {
                        lit("data"); ref("IDENTIFIER"); opt { ref("typeParameterList") }; opt { ref("supertypes") }; lit("{");
                        lst(0, -1) { ref("constructor") };
                        lst(0, -1) { ref("property") };
                        lit("}")
                    }
                    concatenation("interfaceDefinition") { lit("interface"); ref("IDENTIFIER"); opt { ref("typeParameterList") }; opt { ref("supertypes") }; opt { ref("interfaceBody") } }
                    concatenation("interfaceBody") { lit("{"); lst(0, -1) { ref("property") }; lit("}") }
                    concatenation("unionDefinition") { lit("union"); ref("IDENTIFIER"); lit("{"); ref("alternatives"); lit("}") }
                    separatedList("alternatives", 1, -1) { ref("typeReference"); lit("|") }
                    concatenation("typeParameterList") { lit("<"); spLst(1, -1) { ref("IDENTIFIER"); lit(",") }; lit(">"); }
                    concatenation("supertypes") { lit(":"); spLst(1, -1) { ref("typeReference"); lit(",") } }
                    concatenation("constructor") { lit("constructor"); lit("("); lst(0, -1) { ref("constructorParameter") }; lit(")") }
                    concatenation("constructorParameter") { opt { ref("cmp_ref") }; opt { ref("val_var") }; ref("IDENTIFIER"); lit(":"); ref("typeReference") }
                    concatenation("property") { opt { ref("cmp_ref") }; ref("val_var"); ref("IDENTIFIER"); lit(":"); ref("typeReference") }
                    concatenation("typeReference") { ref("possiblyQualifiedName"); opt { ref("typeArgumentList") }; opt { lit("?") } }
                    concatenation("typeArgumentList") { lit("<"); spLst(1, -1) { ref("typeReference"); lit(",") }; lit(">") }
                    choice("cmp_ref") {
                        lit("cmp")
                        lit("ref")
                    }
                    choice("val_var") {
                        lit("val")
                        lit("var")
                    }
                }
            }
        }
    }

    override val typesDomain by lazy {
        typesDomain(NAME, true, AglGrammar.typesDomain.namespace) {
            namespace("net.akehurst.language.types.api", listOf("std", "net.akehurst.language.base.api")) {
                enum("PropertyCharacteristic", listOf("REFERENCE", "COMPOSITE", "READ_ONLY", "READ_WRITE", "STORED", "DERIVED", "PRIMITIVE", "CONSTRUCTOR", "IDENTITY"))
                value("PropertyName") {
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                value("TmParameterName") {
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                value("MethodName") {
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                interface_("ValueType") {
                    supertype("StructuredType")
                    propertyOf(setOf(VAR, CMP, STR), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                }
                interface_("UnionType") {
                    supertype("TypeDefinition")
                }
                interface_("TypeParameter") {

                }
                interface_("TypesNamespace") {
                    supertype("Namespace") { ref("TypeDefinition") }
                }
                interface_("TypesDomain") {
                    supertype("Domain") { ref("TypesNamespace"); ref("TypeDefinition") }
                }
                interface_("TypeInstance") {

                    propertyOf(setOf(VAR, CMP, STR), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                }
                interface_("TypeDefinition") {
                    supertype("Definition") { ref("TypeDefinition") }
                }
                interface_("TypeArgumentNamed") {
                    supertype("TypeArgument")
                }
                interface_("TypeArgument") {

                }
                interface_("TupleTypeInstance") {
                    supertype("TypeInstance")
                    propertyOf(setOf(VAR, CMP, STR), "typeArguments", "List", false) {
                        typeArgument("TypeArgumentNamed")
                    }
                }
                interface_("TupleType") {
                    supertype("TypeDefinition")
                }
                interface_("StructuredType") {
                    supertype("TypeDefinition")
                }
                interface_("SpecialType") {
                    supertype("TypeDefinition")
                }
                interface_("SingletonType") {
                    supertype("TypeDefinition")
                }
                interface_("PropertyDeclarationResolved") {
                    supertype("PropertyDeclaration")
                }
                interface_("PropertyDeclaration") {

                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                interface_("PrimitiveType") {
                    supertype("TypeDefinition")
                }
                interface_("ParameterDeclaration") {

                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                interface_("MethodDeclarationResolved") {
                    supertype("MethodDeclaration")
                }
                interface_("MethodDeclarationPrimitive") {
                    supertype("MethodDeclaration")
                }
                interface_("MethodDeclarationDerived") {
                    supertype("MethodDeclaration")
                }
                interface_("MethodDeclaration") {

                    propertyOf(setOf(VAR, CMP, STR), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                interface_("InterfaceType") {
                    supertype("StructuredType")
                }
                interface_("EnumType") {
                    supertype("TypeDefinition")
                }
                interface_("DataType") {
                    supertype("StructuredType")
                    propertyOf(setOf(VAR, CMP, STR), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                }
                interface_("ConstructorDeclaration") {

                    propertyOf(setOf(VAR, CMP, STR), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                interface_("CollectionType") {
                    supertype("StructuredType")
                }
            }
            namespace("net.akehurst.language.types.asm", listOf("net.akehurst.language.types.api", "net.akehurst.language.base.api", "std", "net.akehurst.language.base.asm")) {
                singleton("TypeParameterMultiple")
                singleton("StdLibDefault")
                data("ValueTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("ValueType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("UnionTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("UnionType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "alternatives", "List", false) {
                        typeArgument("TypeInstance")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("TypeParameterSimple") {
                    supertype("TypeParameter")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                }
                data("TypeParameterReference") {
                    supertype("TypeInstanceAbstract")
                    supertype("TypeInstance")
                    constructor_ {
                        parameter("context", "TypeDefinition", false)
                        parameter("typeParameterName", "SimpleName", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "context", "TypeDefinition", false)
                    propertyOf(setOf(VAL, REF, STR), "isNullable", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STR), "resolvedDeclarationOrNull", "TypeDefinition", false)
                    propertyOf(setOf(VAR, CMP, STR), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "typeParameterName", "SimpleName", false)
                }
                data("TypesNamespaceSimple") {
                    supertype("TypesNamespaceAbstract")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("TypesNamespaceAbstract") {
                    supertype("TypesNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.types.api.TypeDefinition") }
                    constructor_ {
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "ownedTupleTypes", "List", false) {
                        typeArgument("TupleType")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "ownedUnnamedSupertypeType", "List", false) {
                        typeArgument("UnionType")
                    }
                }
                data("TypesDomainSimpleAbstract") {
                    supertype("TypesDomain")
                    constructor_ {}
                    propertyOf(setOf(VAR, CMP, STR), "namespace", "List", false) {
                        typeArgument("TypesNamespace")
                    }
                }
                data("TypesDomainSimple") {
                    supertype("TypesDomainSimpleAbstract")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("options", "OptionHolder", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                data("TypeInstanceSimple") {
                    supertype("TypeInstanceAbstract")
                    constructor_ {
                        parameter("contextQualifiedTypeName", "QualifiedName", false)
                        parameter("namespace", "TypesNamespace", false)
                        parameter("qualifiedOrImportedTypeName", "PossiblyQualifiedName", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "contextQualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAL, REF, STR), "isNullable", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedOrImportedTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "typeArguments", "List", false) {
                        typeArgument("TypeArgument")
                    }
                }
                data("TypeInstanceAbstract") {
                    supertype("TypeInstance")
                    constructor_ {}
                }
                data("TypeDefinitionSimpleAbstract") {
                    supertype("TypeDefinition")
                    constructor_ {
                        parameter("options", "OptionHolder", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "metaInfo", "Map", false) {
                        typeArgument("String")
                        typeArgument("String")
                    }
                    propertyOf(setOf(VAR, REF, STR), "method", "List", false) {
                        typeArgument("MethodDeclaration")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                    propertyOf(setOf(VAR, CMP, STR), "propertyByIndex", "Map", false) {
                        typeArgument("Integer")
                        typeArgument("PropertyDeclaration")
                    }
                    propertyOf(setOf(VAR, REF, STR), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "supertypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "typeParameters", "List", false) {
                        typeArgument("TypeParameter")
                    }
                }
                data("TypeArgumentSimple") {
                    supertype("TypeArgument")
                    constructor_ {
                        parameter("type", "TypeInstance", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "type", "TypeInstance", false)
                }
                data("TypeArgumentNamedSimple") {
                    supertype("TypeArgumentNamed")
                    constructor_ {
                        parameter("name", "PropertyName", false)
                        parameter("type", "TypeInstance", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyName", false)
                    propertyOf(setOf(VAL, REF, STR), "type", "TypeInstance", false)
                }
                data("TupleTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("TupleType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                    propertyOf(setOf(VAR, REF, STR), "typeParameters", "List", false) {
                        typeArgument("TypeParameterMultiple")
                    }
                }
                data("TupleTypeInstanceSimple") {
                    supertype("TypeInstanceAbstract")
                    supertype("TupleTypeInstance")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "isNullable", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                    propertyOf(setOf(VAL, REF, STR), "resolvedDeclaration", "TypeDefinition", false)
                    propertyOf(setOf(VAR, CMP, STR), "typeArguments", "List", false) {
                        typeArgument("TypeArgumentNamed")
                    }
                }
                data("StructuredTypeSimpleAbstract") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("StructuredType")
                    constructor_ {}
                }
                data("SpecialTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("SpecialType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("SingletonTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("SingletonType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("PropertyDeclarationStored") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "StructuredType", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("characteristics", "Set", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "characteristics", "Set", false) {
                        typeArgument("PropertyCharacteristic")
                    }
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "index", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyName", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "StructuredType", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                data("PropertyDeclarationResolvedSimple") {
                    supertype("PropertyDeclarationAbstract")
                    supertype("PropertyDeclarationResolved")
                    constructor_ {
                        parameter("original", "PropertyDeclaration", false)
                        parameter("owner", "TypeDefinition", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("characteristics", "Set", false)
                        parameter("description", "String", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "characteristics", "Set", false) {
                        typeArgument("PropertyCharacteristic")
                    }
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyName", false)
                    propertyOf(setOf(VAL, REF, STR), "original", "PropertyDeclaration", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                data("PropertyDeclarationPrimitive") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "TypeDefinition", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("description", "String", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "index", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyName", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                data("PropertyDeclarationDerived") {
                    supertype("PropertyDeclarationAbstract")
                    constructor_ {
                        parameter("owner", "TypeDefinition", false)
                        parameter("name", "PropertyName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("description", "String", false)
                        parameter("expression", "String", false)
                        parameter("index", "Integer", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "expression", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "index", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyName", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                data("PropertyDeclarationAbstract") {
                    supertype("PropertyDeclaration")
                    constructor_ {}
                    propertyOf(setOf(VAR, REF, STR), "metaInfo", "Map", false) {
                        typeArgument("String")
                        typeArgument("String")
                    }
                }
                data("PrimitiveTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("PrimitiveType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("ParameterDefinitionSimple") {
                    supertype("ParameterDeclaration")
                    constructor_ {
                        parameter("name", "ParameterName", false)
                        parameter("typeInstance", "TypeInstance", false)
                        parameter("defaultValue", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "defaultValue", "String", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "ParameterName", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeInstance", "TypeInstance", false)
                }
                data("MethodDeclarationResolvedSimple") {
                    supertype("MethodDeclarationAbstract")
                    supertype("MethodDeclarationResolved")
                    constructor_ {
                        parameter("original", "MethodDeclaration", false)
                        parameter("owner", "TypeDefinition", false)
                        parameter("name", "MethodName", false)
                        parameter("parameters", "List", false)
                        parameter("returnType", "TypeInstance", false)
                        parameter("description", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "MethodName", false)
                    propertyOf(setOf(VAL, REF, STR), "original", "MethodDeclaration", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAR, CMP, STR), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                    propertyOf(setOf(VAL, REF, STR), "returnType", "TypeInstance", false)
                }
                data("MethodDeclarationDerivedSimple") {
                    supertype("MethodDeclarationAbstract")
                    supertype("MethodDeclarationDerived")
                    constructor_ {
                        parameter("owner", "TypeDefinition", false)
                        parameter("name", "MethodName", false)
                        parameter("parameters", "List", false)
                        parameter("returnType", "TypeInstance", false)
                        parameter("description", "String", false)
                        parameter("body", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "body", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "description", "String", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "MethodName", false)
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAR, CMP, STR), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                    propertyOf(setOf(VAL, REF, STR), "returnType", "TypeInstance", false)
                }
                data("MethodDeclarationAbstract") {
                    supertype("MethodDeclaration")
                    constructor_ {}
                }
                data("InterfaceTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("InterfaceType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                    propertyOf(setOf(VAR, REF, STR), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                data("EnumTypeSimple") {
                    supertype("TypeDefinitionSimpleAbstract")
                    supertype("EnumType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("literals", "List", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "literals", "List", false) {
                        typeArgument("String")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
                data("DataTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("DataType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "constructors", "List", false) {
                        typeArgument("ConstructorDeclaration")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                    propertyOf(setOf(VAR, REF, STR), "subtypes", "List", false) {
                        typeArgument("TypeInstance")
                    }
                }
                data("ConstructorDeclarationSimple") {
                    supertype("ConstructorDeclaration")
                    constructor_ {
                        parameter("owner", "TypeDefinition", false)
                        parameter("parameters", "List", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "owner", "TypeDefinition", false)
                    propertyOf(setOf(VAR, CMP, STR), "parameters", "List", false) {
                        typeArgument("ParameterDeclaration")
                    }
                }
                data("CollectionTypeSimple") {
                    supertype("StructuredTypeSimpleAbstract")
                    supertype("CollectionType")
                    constructor_ {
                        parameter("namespace", "TypesNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "TypesNamespace", false)
                }
            }
            namespace("net.akehurst.language.grammarTypemodel.api", listOf("net.akehurst.language.types.api", "std", "net.akehurst.language.grammar.api")) {
                interface_("GrammarTypesNamespace") {
                    supertype("TypesNamespace")
                    propertyOf(setOf(VAR, CMP, STR), "allRuleNameToType", "Map", false) {
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
                    "net.akehurst.language.types.asm",
                    "net.akehurst.language.grammarTypemodel.api",
                    "net.akehurst.language.grammar.api",
                    "net.akehurst.language.types.api"
                )
            ) {
                data("GrammarTypesNamespaceSimple") {
                    supertype("GrammarTypesNamespaceAbstract")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("GrammarTypesNamespaceAbstract") {
                    supertype("TypesNamespaceAbstract")
                    supertype("GrammarTypesNamespace")
                    constructor_ {
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "allRuleNameToType", "Map", false) {
                        typeArgument("GrammarRuleName")
                        typeArgument("TypeInstance")
                    }
                }
            }
        }
    }

    override val asmTransformDomain by lazy {
        asmTransform(
            name = NAME,
            typesDomain = typesDomain,
            createTypes = false
        ) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                ruleSet(NAME) {
                    //TODO
                }
            }
        }
    }

    override val crossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {

        }
    }

    override val styleDomain by lazy {
        styleDomain(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    metaRule("'[^']+'") {
                        declaration("foreground", "darkgreen")
                        declaration("font-weight", "bold")
                    }
                }
            }
        }
    }

    override val formatDomain by lazy {
        formatDomain(NAME) {
//            TODO("not implemented")
        }
    }

    override val defaultTargetGrammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<TypesDomain> by lazy { TypesSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<TypesDomain, ContextWithScope<Any, Any>>? by lazy { TypemodelSemanticAnalyser() }
    override val completionProvider: CompletionProvider<TypesDomain, ContextWithScope<Any, Any>>? by lazy { TypemodelCompletionProvider() }

}