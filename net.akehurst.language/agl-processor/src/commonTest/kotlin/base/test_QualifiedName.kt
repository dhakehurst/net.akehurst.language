package net.akehurst.language.base.api

import kotlin.test.Test
import kotlin.test.assertEquals

class test_QualifiedName {

    /** front and last give the same as from parts (they no longer split the whole name) */
    @Test
    fun front_and_last_same_as_from_parts() {
        listOf("a", "a.b", "a.b.c", "", ".", "a.", ".a", "a..b", "net.akehurst.language.Square").forEach { v ->
            val qn = QualifiedName(v)
            assertEquals(qn.parts.last(), qn.last, v)
            assertEquals(QualifiedName(qn.parts.dropLast(1).joinToString(separator = ".")), qn.front, v)
        }
    }

    @Test
    fun simpleName_toString_quotes_whitespace() {
        assertEquals("abc", SimpleName("abc").toString())
        assertEquals("'a b'", SimpleName("a b").toString())
        assertEquals("'a\tb'", SimpleName("a\tb").toString())
    }
}
