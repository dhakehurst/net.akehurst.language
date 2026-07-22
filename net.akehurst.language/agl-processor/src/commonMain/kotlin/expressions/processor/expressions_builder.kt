package net.akehurst.language.agl.expressions.processor

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.FunctionDefinitionFloating
import net.akehurst.language.expressions.api.FunctionParameter
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.FunctionDefinitionFloatingDefault
import net.akehurst.language.expressions.asm.FunctionParameterDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.asm.TypeReferenceDefault
import net.akehurst.language.types.asm.StdLibDefault

@DslMarker
annotation class ExpressionsDslMarker

class CustomFunctionBuilder(
    name: String,
) {

    private var _name: SimpleName = SimpleName(name)
    private var _parameters = mutableListOf<FunctionParameter>()
    private var _returnTypeRef: TypeReference = TypeReferenceDefault(StdLibDefault.NothingType.qualifiedTypeName, emptyList(), false)
    private var _bodyExpr: Expression  = RootExpressionDefault.NOTHING
    private var _execution: ((args: List<*>) -> Any?)? = null
    private var _executionSuspend: (suspend (args: List<*>) -> Any?)? = null

    fun parameter(name: String, typeName:String) {
        //TODO: type reference builder
        val typeRef = TypeReferenceDefault(typeName.asPossiblyQualifiedName, emptyList(), false)
        val defVal = null
        _parameters.add(FunctionParameterDefault(name, typeRef, defVal))
    }

    fun execution(block: (args: List<*>) -> Any?) {
        _execution = block
    }

    fun executionSuspend(block: suspend (args: List<*>) -> Any?) {
        _executionSuspend = block
    }

    fun build() = FunctionDefinitionFloatingDefault(_name, _parameters, _returnTypeRef, _bodyExpr).also {
        it.execution = _execution
        it.executionSuspend = _executionSuspend
    }
}