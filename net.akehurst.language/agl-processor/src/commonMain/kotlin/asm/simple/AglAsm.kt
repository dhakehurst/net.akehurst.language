package net.akehurst.language.asm.simple

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel

object AglAsm {

    val komposite = """namespace net.akehurst.language.asm.api
interface Asm {
  cmp root
}
interface AsmStructure {
  cmp semanticPath
  cmp property
}
interface AsmStructureProperty {
  cmp value
}
interface AsmList {
  cmp elements
}
interface AsmListSeparated {
  cmp elements
}
"""

    val typeModel: TypeModel by lazy {
        typeModel("Asm", true, AglBase.typesModel.namespace) {
            namespace("net.akehurst.language.asm.api", listOf("net.akehurst.language.base.api", "std", "net.akehurst.language.collections")) {
                value("PropertyValueName") {
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                interface_("AsmValue") {

                }
                interface_("AsmTreeWalker") {

                }
                interface_("AsmStructureProperty") {

                    propertyOf(setOf(VAL, CMP, STR), "value", "AsmValue", false)
                }
                interface_("AsmStructure") {
                    supertype("AsmValue")
                    propertyOf(setOf(VAR, CMP, STR), "property", "Map", false){
                        typeArgument("PropertyValueName")
                        typeArgument("AsmStructureProperty")
                    }
                }
                interface_("AsmReference") {

                }
                interface_("AsmPrimitive") {
                    supertype("AsmValue")
                }
                interface_("AsmPath") {

                }
                interface_("AsmNothing") {
                    supertype("AsmValue")
                }
                interface_("AsmListSeparated") {
                    supertype("AsmList")
                    propertyOf(setOf(VAR, CMP, STR), "elements", "ListSeparated", false){
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                    }
                }
                interface_("AsmList") {
                    supertype("AsmValue")
                    propertyOf(setOf(VAR, CMP, STR), "elements", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                interface_("AsmLambda") {
                    supertype("AsmValue")
                }
                interface_("AsmAny") {
                    supertype("AsmValue")
                }
                interface_("Asm") {

                    propertyOf(setOf(VAR, CMP, STR), "root", "List", false){
                        typeArgument("AsmValue")
                    }
                }
            }
            namespace("net.akehurst.language.asm.simple", listOf("net.akehurst.language.asm.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.collections")) {
                singleton("AsmNothingSimple")
                singleton("AglAsm")
                data("AsmValueAbstract") {
                    supertype("AsmValue")
                    constructor_ {}
                }
                data("AsmStructureSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmStructure")
                    constructor_ {
                        parameter("qualifiedTypeName", "QualifiedName", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "parsePath", "String", false)
                    propertyOf(setOf(VAR, CMP, STR), "property", "Map", false){
                        typeArgument("PropertyValueName")
                        typeArgument("AsmStructureProperty")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "semanticPath", "AsmPath", false)
                }
                data("AsmStructurePropertySimple") {
                    supertype("AsmStructureProperty")
                    constructor_ {
                        parameter("name", "PropertyValueName", false)
                        parameter("index", "Integer", false)
                        parameter("value", "AsmValue", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "index", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "PropertyValueName", false)
                    propertyOf(setOf(VAR, CMP, STR), "value", "AsmValue", false)
                }
                data("AsmSimpleKt") {

                }
                data("AsmSimple") {
                    supertype("Asm")
                    constructor_ {}
                    propertyOf(setOf(VAR, REF, STR), "elementIndex", "Map", false){
                        typeArgument("AsmPath")
                        typeArgument("AsmStructure")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "root", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                data("AsmReferenceSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmReference")
                    constructor_ {
                        parameter("reference", "String", false)
                        parameter("value", "AsmStructure", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "reference", "String", false)
                    propertyOf(setOf(VAR, REF, STR), "value", "AsmStructure", false)
                }
                data("AsmPrimitiveSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmPrimitive")
                    constructor_ {
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAL, REF, STR), "value", "Any", false)
                }
                data("AsmPathSimple") {
                    supertype("AsmPath")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                data("AsmListSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmList")
                    constructor_ {
                        parameter("elements", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "elements", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                data("AsmListSeparatedSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmListSeparated")
                    constructor_ {
                        parameter("elements", "ListSeparated", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "elements", "ListSeparated", false){
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                    }
                }
                data("AsmLambdaSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmLambda")
                    constructor_ {
                        parameter("lambda", "LambdaType", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "lambda", "LambdaType", false){
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedTypeName", "QualifiedName", false)
                }
                data("AsmAnySimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmAny")
                    constructor_ {
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "Any", false)
                }
            }
            namespace("net.akehurst.language.collections", listOf("std")) {
                interface_("ListSeparated") {
                    typeParameters("E", "I", "S")
                    supertype("List"){ ref("E") }
                }
            }
        }
    }

}