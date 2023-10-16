/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.regex

// see https://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript/63838890#63838890
//actual fun String.asRegexLiteral() :Regex {
//    val p = this
//    //val special = ".*+-?^\${}()|[]\"
//    val escaped = js("String(p).replace(/[\\.\\*\\-\\?\\^\\\$\\{\\}\\(\\)\\|\\[\\]\\\\]/g,'\\\\$1')")
//    return Regex(escaped)
//}
