/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.regex.agl

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The AGL regex must give the same result as Kotlin's Regex (on the JVM, i.e. java.util.regex)
 */
class test_RegexMatcher_sameAsKotlin {

    private companion object {
        val FUZZ_COUNT = System.getenv("FUZZ_COUNT")?.toIntOrNull() ?: 3000
        val FUZZ_SEED = System.getenv("FUZZ_SEED")?.toIntOrNull() ?: 20261029

        // the matched text, null if no match, ERROR if the pattern is not valid
        fun kotlinResult(pattern: String, text: String, at: Int): String? = try {
            Regex(pattern).matchAt(text, at)?.value
        } catch (e: IllegalArgumentException) {
            "ERROR"
        }

        fun aglResult(pattern: String, text: String, at: Int): String? = try {
            regexMatcher(pattern).match(text, at)?.matchedText
        } catch (e: IllegalStateException) {
            "ERROR"
        }

        fun show(s: String?) = s?.replace("\n", "\\n")?.replace("\r", "\\r")

        fun differences(pattern: String, texts: List<String>): List<String> {
            val diffs = mutableListOf<String>()
            for (text in texts) {
                for (at in 0..text.length) {
                    val expected = kotlinResult(pattern, text, at)
                    val actual = aglResult(pattern, text, at)
                    if (expected != actual) diffs.add("pattern '${show(pattern)}' text '${show(text)}' at $at: expected '${show(expected)}' but was '${show(actual)}'")
                }
            }
            return diffs
        }

        fun test(pattern: String, vararg texts: String) {
            val diffs = differences(pattern, texts.toList())
            assertTrue(diffs.isEmpty(), diffs.joinToString("\n"))
        }
    }

    @Test
    fun choice_first_alternative_wins() {
        test("a|ab", "ab", "a", "b")
        test("ab|a", "ab", "a")
        test("a?|b", "b", "a", "")
        test("(a|ab)(c|bcd)", "abcd")
        test("(a|)b", "b", "ab")
        test("(|a)", "a")
        test("a|", "b", "a")
    }

    @Test
    fun greedy_and_lazy() {
        test("a*", "aaa", "")
        test("a*?", "aaa")
        test("a+?", "aaa")
        test("a??", "a")
        test("a*?b", "aab")
        test("a{2,3}?", "aaa")
        test("a{2,}?", "aaaa")
        test("<.*>", "<a><b>")
        test("<.*?>", "<a><b>")
        test("(a|ab)*c", "ababc", "abac")
    }

    @Test
    fun repetition() {
        test("a{2}", "aaa", "a")
        test("a{2,}", "aaaa", "a")
        test("a{1,3}", "aaaa", "")
        test("x{0}a", "a")
        test("(ab){0,2}c", "ababc", "abc", "c")
        test("[ab]{2,3}b", "abab", "aab")
    }

    @Test
    fun empty_matches() {
        test("", "", "a")
        test("()", "")
        test("()a", "a")
        test("(a?)*b", "aab", "b")
        test("(a*)*b", "aab", "b")
        test("(a|)*b", "aab")
        test("(a*)*?b", "aab")
        test("(a?)+", "aa", "")
        test("(b|\\B){2,3}", "ab")
        test("(b|){2,3}", "b", "bbbb")
        test("(|b){2}", "b")
        test("(|b){2,3}?", "bb")
        test("(|b)+", "bb")
        test("(|b){1,}", "bb")
        test("(|b){2,}", "bb")
        test("(b?){2}", "bb", "b")
        test("(a?()|b)*", "ab", "aab")
        test("(b*|c)*", "bcc")
        test("(b*|\\R)*", "b\n\n b")
        test("b((|b)a*)+", "babab", "bab")
    }

    @Test
    fun anchors() {
        test("^a", "a", "ba")
        test("a$", "a", "ab", "a\nb", "a\n", "a\r\n", "a\r", "a\u0085", "a\u2028")
        test("$", "\r\n", "\n", "")
        test("\\Aa", "a", "ba")
        test("a\\z", "a", "a\n")
        test("a\\Z", "a", "a\n", "a\r\n", "a\n\n")
        test("\\Ga", "ba", "a")
        test("(?m)^b", "a\nb", "a\r\nb", "a\rb")
        test("(?m)a$", "a\r\nb", "a\nb", "ab")
        test("(?m)$", "\r\n")
        test("(?m)^", "a\n")
    }

    @Test
    fun word_boundary() {
        test("\\bab", "ab", "cab", " ab")
        test("a\\b", "a", "ab", "a ", "a\u00e9")
        test("\\b\u00e9", "\u00e9")
        test("a\\B", "ab", "a ")
    }

    @Test
    fun dot() {
        test(".", "a", "\n", "\r", "\u0085", "\u2028", "\u2029")
        test("(?s).", "\n")
        test("a.c", "abc", "a\nc")
    }

    @Test
    fun escapes() {
        test("\\)", ")")
        test("\\(", "(")
        test("\\{", "{")
        test("\\}", "}")
        test("\\-", "-")
        test("\\/", "/")
        test("\\'", "'")
        test("\\\"", "\"")
        test("\\.", ".", "a")
        test("\\u0041", "A")
        test("\\x41", "A")
        test("\\x{41}", "A")
        test("\\x{1F600}", "\uD83D\uDE00")
        test("\\0101", "A")
        test("\\cA", "\u0001")
        test("\\t\\n\\r\\f\\a\\e", "\t\n\r\u000C\u0007\u001B")
        test("\\Qa\\bc\\E", "a\\bc")
        test("\\Q(a)", "(a)")
        test("\\d+", "123a")
        test("\\D", "a", "1")
        test("\\s+", " \t\n\u000B\u000C\r")
        test("\\S", "a", " ")
        test("\\w+", "aZ_9-")
        test("\\W", "-", "a")
        test("\\h", " ", "\u00A0", "\n")
        test("\\v", "\n", " ")
        test("\\R", "\r\n", "\n", "\r", "\u2028")
        test("\\R\\n", "\r\n", "\r\n\n")
        test("\\R{2}", "\r\n", "\r\n\n", "\r\r")
        test("\\R+\\n", "\r\n", "\r\n\n")
        test("}", "}")
        test("]", "]")
        test("\\q", "q")
        test("{", "{")
        test("a{", "a{")
        test("a**", "aa")
        test("x{2,1}", "xx")
    }

    @Test
    fun properties() {
        test("\\p{L}+", "a\u00e9B1")
        test("\\pL", "a")
        test("\\p{Lu}", "A", "a")
        test("\\p{IsL}", "a")
        test("\\p{gc=Ll}", "a")
        test("\\P{L}", "1", "a")
        test("\\p{Nd}", "5")
        test("\\p{LC}", "a")
        test("\\p{Lower}", "a", "A")
        test("\\p{Upper}", "A")
        test("\\p{Alpha}", "a")
        test("\\p{Digit}", "1")
        test("\\p{Alnum}", "1")
        test("\\p{Punct}+", "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
        test("\\p{Space}", " ")
        test("\\p{XDigit}+", "09afAFg")
        test("\\p{ASCII}", "a", "\u00e9")
        test("\\p{Cntrl}", "\u0001")
        test("\\p{Blank}", "\t")
        test("\\p{Print}", " ")
        test("\\p{Graph}", " ")
    }

    @Test
    fun character_class() {
        test("[a\\-z]", "m", "-", "z")
        test("[\\]]", "]")
        test("[^\\]]+", "ab]")
        test("[\\^a]", "^")
        test("[\\s]+", " x")
        test("[\\d]", "5")
        test("[\\w.]+", "a.b")
        test("[\\.]", ".")
        test("[]a]", "]", "a")
        test("[^]a]", "b", "]")
        test("[]", "]")
        test("[a-c-e]", "-", "d", "e")
        test("[a-]", "-")
        test("[-a]", "-")
        test("[--/]", ".")
        test("[a-\\d]", "a")
        test("[\\d-z]", "-", "y", "z")
        test("[z-a]", "a")
        test("[\\p{Lu}]", "A")
        test("[\\Q]\\E]", "]")
        test("[a&&b]", "a")
        test("[a&&]", "a")
        test("[&&a]", "a")
        test("[a-z&&[^aeiou]]+", "bca")
        test("[^a-z&&[b]]", "a", "b")
        test("[a[bc]]", "c")
        test("[\\x41-\\x43]", "B")
        test("[\\u0041]", "A")
        test("[\\t]", "\t")
        test("[\\b]", "\b")
        test("[.$^(){}*+?|]+", ".$^(){}*+?|")
    }

    @Test
    fun groups_and_lookaround() {
        test("(?:ab)+", "abab")
        test("(?<name>a)b", "ab")
        test("a(?=b)", "ab", "ac")
        test("a(?!b)", "ab", "ac")
        test("(?<=a)b", "ab", "cb")
        test("(?<!a)b", "ab", "cb")
        test("(?<=a{1,3})b", "aab")
        test("(?<=a+)b", "aab")
        test("(?<=^|,)x", "x", ",x", "ax")
        test("(?=a|ab)\\w+", "ab")
        test("((?=x)y){2}", "yy")
    }

    @Test
    fun flags() {
        test("(?i)k", "K", "\u212A")
        test("(?i)[^a]", "A", "b")
        test("(?i)[a-c]", "B")
        test("(?i)abc", "AbC")
        test("a(?i)b", "aB", "AB")
        test("(?i:a)b", "Ab", "AB")
        test("((?i)a)b", "Ab", "AB")
        test("(?i)a|b", "B")
        test("(?-i)a", "A")
        test("(?x)a b # comment", "ab")
        test("(?x)[a b]", " ")
        test("(?ms)^a.$", "x\na\n")
    }

    @Test
    fun scanner_patterns() {
        test("\"(\\\\\"|[^\"])*\"", "\"ab\\\"c\"d")
        test("'([^'\\\\]|\\\\.)*'", "'a\\'b'")
        test("[a-zA-Z_][a-zA-Z_0-9]*", "abc_12 x")
        test("[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?", "12.5e-3x", "12.x")
        test("//[^\\n]*", "// comment\nx")
        test("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "/* a ** b */x")
        test("\\s+", "  \n x")
    }

    @Test
    fun not_supported_gives_an_error() {
        for (p in listOf("x*+", "(?>a|ab)c", "(a)\\1", "(?<n>a)\\k<n>", "\\X", "(?u)a", "\\p{IsLatin}", "\\p{InGreek}")) {
            val r = aglResult(p, "a", 0)
            assertEquals("ERROR", r, "pattern '$p'")
        }
    }

    @Test
    fun no_exponential_time() {
        val start = System.currentTimeMillis()
        test("(a|a)*b", "a".repeat(30) + "b", "a".repeat(30))
        test("(a|aa)*b", "a".repeat(40) + "b")
        test("[a-z]*[a-z]*[a-z]*", "a".repeat(500))
        val ms = System.currentTimeMillis() - start
        assertTrue(ms < 5000, "took $ms ms")
    }

    @Test
    fun matcher_used_by_two_threads() {
        val m = regexMatcher("[a-z]+[0-9]*")
        val errors = java.util.concurrent.atomic.AtomicInteger()
        val threads = (0 until 4).map { t ->
            Thread {
                for (i in 0 until 2000) {
                    val text = "x".repeat(t + 1) + i
                    if (m.match(text, 0)?.matchedText != text) errors.incrementAndGet()
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(0, errors.get())
    }

    // random small patterns and texts, compared with Kotlin's Regex
    @Test
    fun fuzz() {
        val rnd = Random(FUZZ_SEED)
        val texts = mutableListOf<String>()
        val alphabet = listOf("a", "b", "A", "\n", "\r", " ")
        repeat(40) { texts.add((0 until rnd.nextInt(7)).joinToString("") { alphabet[rnd.nextInt(alphabet.size)] }) }
        texts.add("")
        val failures = mutableListOf<String>()
        var count = 0
        repeat(FUZZ_COUNT) {
            val pattern = RandomPattern(rnd).pattern(3)
            val diffs = differences(pattern, texts.shuffled(rnd).take(6))
            count++
            if (diffs.isNotEmpty()) failures.add(diffs.first())
        }
        if (failures.isNotEmpty()) fail("${failures.size} of $count patterns differ:\n" + failures.take(30).joinToString("\n"))
    }

    private class RandomPattern(val rnd: Random) {
        fun pattern(depth: Int): String {
            val n = 1 + rnd.nextInt(3)
            return (0 until n).joinToString("|") { sequence(depth) }
        }

        fun sequence(depth: Int): String = (0 until rnd.nextInt(4)).joinToString("") { quantified(depth) }

        fun quantified(depth: Int): String {
            val a = atom(depth)
            if (a.startsWith("^") || a.startsWith("$") || a.startsWith("\\b") || a.startsWith("\\B") || a.startsWith("\\A") || a.startsWith("\\z") || a.startsWith("\\Z") || a.startsWith("(?=") || a.startsWith("(?!") || a.startsWith("(?<") || a.startsWith("(?i)")) return a
            val q = when (rnd.nextInt(10)) {
                0 -> "*"
                1 -> "+"
                2 -> "?"
                3 -> "{${rnd.nextInt(3)},${2 + rnd.nextInt(2)}}"
                4 -> "{${rnd.nextInt(3)}}"
                5 -> "{1,}"
                else -> ""
            }
            val lazy = if (q.isNotEmpty() && rnd.nextInt(3) == 0) "?" else ""
            return a + q + lazy
        }

        fun atom(depth: Int): String = when (if (depth <= 0) rnd.nextInt(10) else rnd.nextInt(18)) {
            0, 1, 2 -> "a"
            3 -> "b"
            4 -> "."
            5 -> listOf("[ab]", "[^a]", "[a-b]", "[\\s]", "\\s", "\\w", "[^\\n]")[rnd.nextInt(7)]
            6 -> listOf("^", "$", "\\b", "\\B", "\\A", "\\z", "\\Z")[rnd.nextInt(7)]
            7 -> "\\R"
            8 -> "(?i)"
            9 -> "A"
            10, 11 -> "(${pattern(depth - 1)})"
            12 -> "(?:${pattern(depth - 1)})"
            13 -> "(?=${pattern(depth - 1)})"
            14 -> "(?!${pattern(depth - 1)})"
            15 -> "(?<=${bounded()})"
            16 -> "(?<!${bounded()})"
            else -> "(?m:${pattern(depth - 1)})"
        }

        // lookbehind needs a bounded length
        fun bounded(): String = listOf("a", "b", "a|b", "ab", "a?", "[ab]{1,2}", "\\n", "^", "$")[rnd.nextInt(9)]
    }
}
