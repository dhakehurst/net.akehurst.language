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

package net.akehurst.language.api.processor

import net.akehurst.language.api.parser.InputLocation

class LanguageProcessorOptions<AsmType : Any,ContextType : Any> {
    val parser = ParserOptions()
    val syntaxAnalyser = SyntaxAnalyserOptions<AsmType,ContextType>()
    val semanticAnalyser = SemanticAnalyserOptions<AsmType,ContextType>()
}

class ParserOptions {
    var goalRuleName: String? = null
    var automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1
}

class SyntaxAnalyserOptions<AsmType : Any,ContextType : Any>() {
    var active = true
    var context: ContextType? = null
}

class SemanticAnalyserOptions<AsmType : Any,ContextType : Any>() {
    var active = true
    var locationMap = emptyMap<Any, InputLocation>()
}

@DslMarker
annotation class LanguageProcessorOptionsDslMarker

fun <AsmType : Any,ContextType : Any> aglOptions(init: LanguageProcessorOptionsBuilder<AsmType,ContextType>.() -> Unit): LanguageProcessorOptions<AsmType,ContextType> {
    val b = LanguageProcessorOptionsBuilder<AsmType,ContextType>()
    b.init()
    return b.build()
}

fun parserOptions(init: ParserOptionsBuilder.() -> Unit):ParserOptions {
    val b = ParserOptionsBuilder()
    b.init()
    return b.build()
}

@LanguageProcessorOptionsDslMarker
class LanguageProcessorOptionsBuilder<AsmType : Any,ContextType : Any>() {

    private val _options = LanguageProcessorOptions<AsmType,ContextType>()

    fun parser(init: ParserOptionsBuilder.() -> Unit):ParserOptions {
        val b = ParserOptionsBuilder()
        b.init()
        return b.build()
    }

    fun syntaxAnalyser(init: SyntaxAnalyserOptionsBuilder<AsmType,ContextType>.() -> Unit):SyntaxAnalyserOptions<AsmType,ContextType> {
        val b = SyntaxAnalyserOptionsBuilder<AsmType,ContextType>()
        b.init()
        return b.build()
    }

    fun semanticAnalyser(init: SemanticAnalyserOptionsBuilder<AsmType,ContextType>.() -> Unit):SemanticAnalyserOptions<AsmType,ContextType> {
        val b = SemanticAnalyserOptionsBuilder<AsmType,ContextType>()
        b.init()
        return b.build()
    }

    fun build() : LanguageProcessorOptions<AsmType,ContextType> {
        return _options
    }
}

@LanguageProcessorOptionsDslMarker
class ParserOptionsBuilder() {

    private val _options = ParserOptions()

    fun goalRule(name:String) {
        _options.goalRuleName = name
    }

    fun build() : ParserOptions {
        return _options
    }
}

@LanguageProcessorOptionsDslMarker
class SyntaxAnalyserOptionsBuilder<AsmType : Any,ContextType : Any>() {

    private val _options = SyntaxAnalyserOptions<AsmType,ContextType>()

    fun active(value:Boolean) {
        _options.active = value
    }

    fun context(value:ContextType) {
        _options.context = value
    }

    fun build() : SyntaxAnalyserOptions<AsmType,ContextType> {
        return _options
    }
}

@LanguageProcessorOptionsDslMarker
class SemanticAnalyserOptionsBuilder<AsmType : Any,ContextType : Any>() {

    private val _options = SemanticAnalyserOptions<AsmType,ContextType>()

    fun active(value:Boolean) {
        _options.active = value
    }

    fun build() : SemanticAnalyserOptions<AsmType,ContextType> {
        return _options
    }
}