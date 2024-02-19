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

package net.akehurst.language.agl.agl.default

import net.akehurst.language.agl.language.asmTransform.TransformationRuleAbstract
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.asmTransform.CreateObjectRule
import net.akehurst.language.api.language.asmTransform.ModifyObjectRule
import net.akehurst.language.api.language.grammar.Grammar

class AsmTransformModelDefault(
    val grammar: Grammar
) : AsmTransformModel {
    override val qualifiedName: String get() = this.grammar.qualifiedName
    override val name: String get() = this.qualifiedName.split(".").last()

    override val rules = mutableListOf<TransformationRuleAbstract>()
    override val modifyObjectRules: List<ModifyObjectRule>
        get() = TODO("not implemented")
    override val createObjectRules: List<CreateObjectRule>
        get() = TODO("not implemented")
}