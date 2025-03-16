/*
 * based on https://github.com/jkanalakis/LLMed
 *
 * Copyright 2023 John Kanalakis
 * LLMed | Large Language Model for Educational Understanding
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.akehurst.language.agl.llm

import korlibs.io.async.runBlockingNoJs
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// The TextCorpus provides functionality to store textual data corpora for the model, including
// utilities to access this training data, vocabulary metadata, individual words/samples, and
// key attributes of the corpus
class TextCorpus {
    private val textData: MutableList<String?> = ArrayList<String?>() // Stores the textual training data
    private val vocabList: MutableList<String?> = ArrayList<String?>() // Stores the vocabulary (unique words)

    // Retrieves current vocabulary size
    val vocabSize: Int get() = vocabList.size

    // Gets number of text samples loaded
    val textSize: Int get() = textData.size

    // Cleans/normalizes a text sample
    private fun preprocessText(text: String): String {
        // Remove Unicode BOM character if present

        var text = text
        text = text.replace("\ufeff", "")

        // Normalize whitespace
        text = text.replace("\\s+".toRegex(), " ")

        // Convert to lower case
        text = text.lowercase()

        // Normalize accented characters
//        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        text = text.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")

        // Replace hyphens with space
        text = text.replace("-".toRegex(), " ")

        // Remove punctuation and non-letter characters
        text = text.replace("[^a-z ]".toRegex(), "")

        return text
    }

    // Processes text and tracks unique words
    fun updateVocabulary(text: String) {
        // Split the text into words

        val words: Array<String?> = text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // Split by whitespace

        for (word in words) {
            // Add each word to the vocabulary if it's not already present
            if (!vocabList.contains(word)) {
                vocabList.add(word)
            }
        }
    }

    // Loads text data from files into storage
    fun loadText(file: String) {
        try {
            runBlockingNoJs {
                resourcesVfs[file].readLines().forEach { line ->
                    val cleanedText = preprocessText(line)

                    // Update data and vocab
                    textData.add(cleanedText)
                    updateVocabulary(cleanedText)
                }
            }
        } catch (e: Exception) {
            println("Error reading file.")
            e.printStackTrace()
        }
    }

    // Retrieves word for given index
    fun getVocabWord(index: Int): String {
        return if (index >= 0 && index < vocabList.size) {
            vocabList[index]!!
        } else {
            ""
        }
    }

    // Looks up index for given word
    fun getWordIndex(word: String?): Int {
        return vocabList.indexOf(word)
    }

    // Accesses stored text sample by index
    fun getTextSample(index: Int): String? {
        return textData.get(index)
    }
}