package net.akehurst.language.agl.regex

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsLiteral
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import kotlin.test.Test
import kotlin.test.assertEquals

class test_std_RegEx {

    @Test
    fun t() {
        //'(', ID, chSep, ';'
        val rr = RuntimeRule(0,0,"x",false).also {
            it.setRhs(RuntimeRuleRhsPattern(it,"|"))
        }
        val text = "?"
        val result = (rr.rhs as RuntimeRuleRhsPattern).regex.matchesAt(text, 0)

        assertEquals(false, result)
    }

}