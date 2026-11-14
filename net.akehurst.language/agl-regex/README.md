# AGL Regex

A regular expression engine written in Kotlin common code, so that it is the same on every platform (JVM, JS, wasmJs),
and a generator of strings that match a regular expression.

It gives the same results as Kotlin's `Regex` on the JVM (`java.util.regex.Pattern`), for the syntax it supports.
It does not backtrack. It runs the pattern's NFA with all choices in parallel, so the time to match is linear in the
length of the text.

The module also holds the regex API that AGL uses (`RegexEngine`, `Regex`, `MatchResult`), with two engines:
`RegexEnginePlatform` (Kotlin's `Regex` of the platform) and `RegexEngineAgl` (this engine).
Which one a language processor uses is chosen with `RegexEngineKind` in its configuration. The default is PLATFORM.

## Use

```kotlin
// match
val m = regexMatcher("[a-z]+[0-9]*")          // net.akehurst.language.regex.agl
m.matchAt("abc12 x", 0)?.matchedText           // "abc12"
m.matchesAt("12", 0)                           // false

// through the engine API, as the scanners do
val r = RegexEngineAgl.createFor("[a-z]+")
r.matchAt("abc", 0)?.matchedText               // "abc"

// generate strings that match
val p = RegexValueProvider("[a-z]{2,4}[0-9]?", 'X')
p.provide()                                    // "aa": always the same value
p.provideRandom(Random(1), maxLength = 10)     // a random value, e.g. "qbm7"
```

`matchAt(text, at)` is as Kotlin's `Regex.matchAt`: the match must start at `at`, and the text before `at` is still seen
by lookbehind, `\b` and `\G` (transparent bounds), but `^` and `\A` match only at the start of the text.
A matcher can be used by several threads at once.

## Syntax

The syntax and meaning are those of `java.util.regex.Pattern`.

| Kind | Supported |
|------|-----------|
| Characters | any char; `\t \n \r \f \a \e`; `\0ooo`; `\xhh`; `\x{h...h}`; `\uhhhh`; `\cX`; `\` before any non-letter (e.g. `\. \( \) \{ \- \/`); `\Q...\E` |
| Predefined classes | `.` (not line terminators, unless `(?s)`); `\d \D \s \S \w \W` (ASCII, as Kotlin/JVM); `\h \H \v \V`; `\R` |
| Properties | `\p{..}` and `\P{..}`, also `\pL`: POSIX classes (`Lower Upper ASCII Alpha Digit Alnum Punct Graph Print Blank Cntrl XDigit Space`), Unicode general categories (`L`, `Lu`, `IsLu`, `gc=Lu`, `general_category=Lu`, `LC`) |
| Character classes | `[abc]`, `[^abc]`, ranges `[a-z]`, escapes inside (`[\]\-\^]`, `[\d\s\w]`, `[\p{Lu}]`), union `[a-d[m-p]]`, intersection `[a-z&&[^aeiou]]`, `[]a]` |
| Anchors | `^ $` (with `(?m)`: at line terminators), `\A \z \Z \G`, `\b \B` |
| Groups | `(X)`, `(?:X)`, `(?<name>X)` (groups are not captured, the name is not used) |
| Lookaround | `(?=X)`, `(?!X)`, `(?<=X)`, `(?<!X)` |
| Quantifiers | `? * + {n} {n,} {n,m}` and the lazy forms `?? *? +? {n,m}?` |
| Choice | `X\|Y`, empty alternatives |
| Flags | `(?imsx-imsx)` and `(?imsx-imsx:X)`: `i` case-insensitive (US-ASCII), `m` multiline, `s` dot-all, `x` comments |

Line terminators are `\n`, `\r`, `\r\n`, `\u0085`, ` `, ` `, as in Kotlin/JVM without `UNIX_LINES`.

Not supported. These give an error when the pattern is parsed:
- back references (`\1`, `\k<name>`) and atomic groups (`(?>X)`), as they need backtracking
- possessive quantifiers (`*+`, `++`, `?+`, `{n,m}+`)
- the flags `d u U c`
- Unicode scripts, blocks and binary properties (`\p{IsLatin}`, `\p{InGreek}`, `\p{IsAlphabetic}`, `\p{javaLowerCase}`)
- `\X`, `\N{..}`, `\b{g}`

Known differences, which give no error:
- `.` and negated classes match one char (UTF-16 code unit). Kotlin/JVM matches a whole code point (a surrogate pair).
  A literal char above `￿` in the pattern does match as one unit.
- with `(?i)`, `\p{Lower}` and `\p{Upper}` are not case-insensitive (in Kotlin/JVM they are).

Kotlin's `Regex` on JS uses the JavaScript engine, which differs from the JVM in places (e.g. `$`, `\Z`, possessive quantifiers).
This engine always behaves as the JVM.

## Generating strings

`RegexValueProvider(pattern, anyChar)` gives strings that match the whole pattern (as `Regex.matches`):
- `provide()` always gives the same value: the first alternative of each choice, the fewest repetitions, the first char of each
  character class, and `anyChar` for `.`. Completion uses it to show an example value for a pattern terminal
  (`provideValuesForPatternTerminals`).
- `provideRandom(random, maxLength)` gives a random value: random choices, random repetitions, and random chars (printable ASCII,
  plus the chars named in the pattern). After about `maxLength` chars it takes the shortest way to the end of the pattern.

Each value is checked to match. Assertions and lookaround can make a choice fail, so another is tried. If none is found,
the value of `provide()` is given.

## How it works

- `RegexParser` (recursive descent) builds an NFA with `RegexMatcherBuilder`.
  - Split states hold the choices, in priority order.
  - Each other state either matches one char or is a zero-width assertion.
  - `{n,m}` is expanded into n copies followed by nested optionals.
- `RegexMatcherImpl` runs the NFA (a Pike VM). The states reached at each position are kept in priority order, each state once.
  When a state reaches the end of the pattern, the lower-priority states are dropped. This gives the match a backtracking matcher
  would find first (first alternative, greedy or lazy repetition), in time linear in the text.
- As in Kotlin/JVM, a loop iteration that matches nothing ends the loop, e.g. `(|b)+` matches "" in "bb".
- Lookahead and lookbehind run the NFA of the looked-at pattern from the current position.

## Requirements

Status: **done** = implemented on `2.4.0-inwork`; **todo** = agreed, not yet implemented.

| Id | Requirement | Status |
|----|-------------|--------|
| R1 | The engine is Kotlin common code and behaves the same on every platform (JVM, JS, wasmJs). | done (tests run on JVM only; JS and wasm compile) |
| R2 | For every pattern it supports, it gives the same result as Kotlin's `Regex` on the JVM: the same match (or no match) for `matchAt(text, at)`. | done |
| R3 | A pattern it does not support, or that Kotlin/JVM rejects, gives an error when it is parsed. The error says what and where. | done |
| R4 | It does not backtrack. The time to match is linear in the length of the text, for a given pattern. | done |
| R5 | Features that need backtracking (back references, atomic groups, possessive quantifiers) are not supported. | done (they give an error) |
| R6 | One matcher can be used by several threads at once. | done |
| R7 | The syntax in the table above is supported. | done |
| R8 | `RegexValueProvider.provide()` gives a string that matches the whole pattern, and the same string every time. | done |
| R9 | `RegexValueProvider.provideRandom(random, maxLength)` gives a random string that matches the whole pattern, of about `maxLength` chars at most. | done |
| R10 | The behaviour is checked against Kotlin's `Regex` by tests: hand-picked cases and random patterns (`test_RegexMatcher_sameAsKotlin`, set `FUZZ_SEED` and `FUZZ_COUNT` to run more). | done |
