/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.api.parser;

import net.akehurst.language.api.sppt.SharedPackedParseTree

//class ParserException(message: String) : RuntimeException(message)

class ParserTerminatedException(message: String) : RuntimeException(message)

class ParseFailedException(
    message: String,
    val longestMatch: SharedPackedParseTree?,
    val location: InputLocation,
    val expected: Set<String>,
    val contextInText: String
) : RuntimeException("$message, at line ${location.line} column ${location.column}, expected one of ${expected}\n...${contextInText}...")
