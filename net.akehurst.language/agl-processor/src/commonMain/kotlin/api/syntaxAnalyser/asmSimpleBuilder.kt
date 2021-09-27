/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

fun asmSimple(init: AsmSimpleBuilder.() -> Unit): AsmSimple {
    val b = AsmSimpleBuilder()
    b.init()
    return b.build()
}

class AsmSimpleBuilder() {

    private val _asm = AsmSimple()

    fun root(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val b = AsmElementSimpleBuilder(this._asm, typeName, true)
        b.init()
        return b.build()
    }

    fun build(): AsmSimple = _asm
}

class AsmElementSimpleBuilder(
    private val _asm: AsmSimple,
    typeName: String,
    isRoot: Boolean
) {
    private val _element = if (isRoot) _asm.createRootElement(typeName) else _asm.createNonRootElement(typeName)

    fun property(name: String, value: Any?) {
        _element.setProperty(name, value,false)
    }

    fun property(name: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple = property(name,name,init)
    fun property(name: String, typeName:String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val b = AsmElementSimpleBuilder(this._asm, typeName, false)
        b.init()
        val el = b.build()
        this._element.setProperty(name,el, false)
        return el
    }

    fun reference(name: String, element:AsmElementSimple) {
        _element.setProperty(name,element,true)
    }

    fun element(typeName: String, init: AsmElementSimpleBuilder.() -> Unit): AsmElementSimple {
        val b = AsmElementSimpleBuilder(this._asm, typeName, false)
        b.init()
        return b.build()
    }

    fun build(): AsmElementSimple = _element
}