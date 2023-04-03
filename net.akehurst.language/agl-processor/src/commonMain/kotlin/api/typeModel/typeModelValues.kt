/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.typeModel

abstract class AnyValue(
    open val type: RuleType
) {
    abstract val asValue: Any?
}

abstract class PrimitiveValue(
    override val type:RuleType
): AnyValue(type) {
}

object NothingValue : PrimitiveValue(NothingType) {
    override val asValue: Nothing? = null
    override fun toString(): String = "NOTHING"
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when(other) {
        !is NothingValue -> false
        else -> true
    }
}

class StringValue(
    override val asValue:String
) : PrimitiveValue(StringType) {
    override fun toString(): String = "'$asValue'"
    override fun hashCode(): Int = asValue.hashCode()
    override fun equals(other: Any?): Boolean = when(other) {
        !is StringValue -> false
        else -> this.asValue==other.asValue
    }
}

class ListValue<E>(
    override val type: ListSimpleType,
    override val asValue:List<E>
): AnyValue(type) {
    override fun toString(): String = "[${asValue.joinToString(separator = ",") { "$it" }}]"
    override fun hashCode(): Int = asValue.hashCode()
    override fun equals(other: Any?): Boolean = when(other) {
        !is ListValue<*> -> false
        (this.type != other.type) -> false
        else -> this.asValue==other.asValue
    }
}

class ListSeparatedValue<I,S>(
    override val type: ListSeparatedType,
    override val asValue:List<Any>
): AnyValue(type) {
    override fun toString(): String = "[${asValue.joinToString(separator = ",") { "$it" }}]"
    override fun hashCode(): Int = asValue.hashCode()
    override fun equals(other: Any?): Boolean = when(other) {
        !is ListSeparatedValue<*,*> -> false
        (this.type != other.type) -> false
        else -> this.asValue==other.asValue
    }
}

abstract class StructuredValue(
    override val type:StructuredRuleType
): AnyValue(type) {
}

class TupleValue(
    override val type:TupleType,
    override val asValue:Map<String,Any>
): StructuredValue(type) {

    override fun toString(): String = "{\n  ${asValue.entries.joinToString(separator = "\n  ") { "${it.key}=${it.value}" }}}"
    override fun hashCode(): Int = asValue.hashCode()
    override fun equals(other: Any?): Boolean = when(other) {
        !is TupleValue -> false
        (this.type != other.type) -> false
        else -> this.asValue==other.asValue
    }
}