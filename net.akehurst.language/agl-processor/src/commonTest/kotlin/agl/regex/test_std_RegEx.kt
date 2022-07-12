package net.akehurst.language.agl.regex

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import kotlin.test.Test
import kotlin.test.assertEquals

class test_std_RegEx {

    @Test
    fun t() {
        //'(', ID, chSep, ';'
        val rr = RuntimeRule(0,0,"x","|",RuntimeRuleKind.TERMINAL,false,false,null,null)
        val text = "?"
        val result = rr.regex.matchesAt(text, 0)

        assertEquals(false, result)
    }

}