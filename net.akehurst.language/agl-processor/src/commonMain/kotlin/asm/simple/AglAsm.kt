package net.akehurst.language.asm.simple

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.typeModel

object AglAsm {

    val komposite = """namespace net.akehurst.language.asm.api
interface Asm {
  cmp root
}
interface AsmStructure {
  cmp path
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
        typeModel("Asm", true, AglBase.typeModel.namespace) {
            namespace("net.akehurst.language.asm.api", listOf("std", "net.akehurst.language.base.api", "net.akehurst.language.collections")) {
                valueType("PropertyValueName") {

                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                interfaceType("AsmValue") {

                }
                interfaceType("AsmTreeWalker") {

                }
                interfaceType("AsmStructureProperty") {

                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "value", "AsmValue", false)
                }
                interfaceType("AsmStructure") {
                    supertype("AsmValue")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "path", "AsmPath", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "property", "Map", false){
                        typeArgument("PropertyValueName")
                        typeArgument("AsmStructureProperty")
                    }
                }
                interfaceType("AsmReference") {

                }
                interfaceType("AsmPrimitive") {
                    supertype("AsmValue")
                }
                interfaceType("AsmPath") {

                }
                interfaceType("AsmNothing") {
                    supertype("AsmValue")
                }
                interfaceType("AsmListSeparated") {
                    supertype("AsmList")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "elements", "ListSeparated", false){
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                    }
                }
                interfaceType("AsmList") {
                    supertype("AsmValue")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "elements", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                interfaceType("AsmAny") {
                    supertype("AsmValue")
                }
                interfaceType("Asm") {

                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "root", "List", false){
                        typeArgument("AsmValue")
                    }
                }
            }
            namespace("net.akehurst.language.asm.simple", listOf("net.akehurst.language.asm.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.collections")) {
                singleton("AsmNothingSimple")
                singleton("AglAsm")
                dataType("AsmValueAbstract") {
                    supertype("AsmValue")
                    constructor_ {}
                }
                dataType("AsmStructureSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmStructure")
                    constructor_ {
                        parameter("path", "AsmPath", false)
                        parameter("qualifiedTypeName", "QualifiedName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "path", "AsmPath", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "property", "Map", false){
                        typeArgument("PropertyValueName")
                        typeArgument("AsmStructureProperty")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedTypeName", "QualifiedName", false)
                }
                dataType("AsmStructurePropertySimple") {
                    supertype("AsmStructureProperty")
                    constructor_ {
                        parameter("name", "PropertyValueName", false)
                        parameter("index", "Integer", false)
                        parameter("value", "AsmValue", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "index", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "PropertyValueName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "value", "AsmValue", false)
                }
                dataType("AsmSimpleKt") {

                }
                dataType("AsmSimple") {
                    supertype("Asm")
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "elementIndex", "Map", false){
                        typeArgument("AsmPath")
                        typeArgument("AsmStructure")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "root", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                dataType("AsmReferenceSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmReference")
                    constructor_ {
                        parameter("reference", "String", false)
                        parameter("value", "AsmStructure", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "reference", "String", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "value", "AsmStructure", false)
                }
                dataType("AsmPrimitiveSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmPrimitive")
                    constructor_ {
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "Any", false)
                }
                dataType("AsmPathSimple") {
                    supertype("AsmPath")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                dataType("AsmListSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmList")
                    constructor_ {
                        parameter("elements", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "elements", "List", false){
                        typeArgument("AsmValue")
                    }
                }
                dataType("AsmListSeparatedSimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmListSeparated")
                    constructor_ {
                        parameter("elements", "ListSeparated", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "elements", "ListSeparated", false){
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                        typeArgument("AsmValue")
                    }
                }
                dataType("AsmAnySimple") {
                    supertype("AsmValueAbstract")
                    supertype("AsmAny")
                    constructor_ {
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "Any", false)
                }
            }
            namespace("net.akehurst.language.collections", listOf("std")) {
                interfaceType("ListSeparated") {
                    typeParameters("E", "I", "S")
                    supertype("List"){ ref("E") }
                }
            }}
    }

}