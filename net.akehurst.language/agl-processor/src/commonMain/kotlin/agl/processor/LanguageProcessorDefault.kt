/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.grammar.processor.ConverterToRuntimeRules
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.api.processor.LanguageProcessorConfiguration
import net.akehurst.language.parser.api.RuleSet

internal class LanguageProcessorDefault<AsmType : Any, ContextType : Any>(
    override val grammar: Grammar,
    override val configuration: LanguageProcessorConfiguration<AsmType, ContextType>,
) : LanguageProcessorAbstract<AsmType, ContextType>() {

    override val ruleSet get() = _runtimeRuleSet
    override val mapToGrammar: (Int, Int) -> RuleItem? = { ruleSetNumber, ruleNumber -> this._originalRuleMap[Pair(ruleSetNumber, ruleNumber)] }

    private var _originalRuleMap: Map<Pair<Int, Int>, RuleItem>
    private var _runtimeRuleSet: RuleSet

    init {
        val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
        _runtimeRuleSet = converterToRuntimeRules.runtimeRuleSet
        _originalRuleMap = converterToRuntimeRules.originalRuleItemMap
    }
}
