/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.style.builder

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.asm.AglStyleModelDefault

fun styleModel(name:String, init:StyleModelBuilder.()->Unit) : AglStyleModel {
    val b = StyleModelBuilder(SimpleName(name))
    b.init()
    return b.build()
}


class StyleModelBuilder(
    name:SimpleName
) {

    private val _model = AglStyleModelDefault(name)

    fun build() : AglStyleModel  = _model
}