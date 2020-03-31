/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.editor.common

import net.akehurst.language.api.sppt.SPPTLeaf

class AglStyleHandler(
        val languageId:String,
        val cssClassPrefixStart:String = "agl"
) {
    var nextCssClassNum = 1
    val cssClassPrefix:String = "${cssClassPrefixStart}-${languageId}-"
    val tokenToClassMap = mutableMapOf<String, String>()

    private fun mapTokenTypeToClass(tokenType: String): String? {
        var cssClass = this.tokenToClassMap.get(tokenType)
        return cssClass
    }

    private fun mapToCssClasses(leaf: SPPTLeaf): List<String> {
        val metaTagClasses = leaf.metaTags.mapNotNull { this.mapTokenTypeToClass(it) }
        val otherClasses = if (!leaf.tagList.isEmpty()) {
            leaf.tagList.mapNotNull { this.mapTokenTypeToClass(it) }
        } else {
            listOf(this.mapTokenTypeToClass(leaf.name)).mapNotNull { it }
        }
        val classes = metaTagClasses + otherClasses
        return if (classes.isEmpty()) {
            listOf("nostyle")
        } else {
            classes.toSet().toList()
        }
    }

    fun transformToTokens(leafs: List<SPPTLeaf>): List<AglToken> {
        return leafs.map { leaf ->
            val cssClasses = this.mapToCssClasses(leaf)
            var beforeEOL = leaf.matchedText
            val eolIndex = leaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                beforeEOL = leaf.matchedText.substring(0, eolIndex);
            }
            AglToken(
                    cssClasses.toSet().toTypedArray(),
                    beforeEOL,
                    leaf.location.line, //ace first line is 0
                    leaf.location.column
            )
        }
    }

    fun reset() {
        this.tokenToClassMap.clear()
        nextCssClassNum = 1
    }

    fun mapClass(aglSelector:String) : String {
        var cssClass = this.tokenToClassMap.get(aglSelector)
        if (null == cssClass) {
            cssClass = this.cssClassPrefix + this.nextCssClassNum++
            this.tokenToClassMap.set(aglSelector, cssClass)
        }
        return cssClass
    }
}