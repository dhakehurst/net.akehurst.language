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

import korlibs.memory.arraycopy
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random

// Responsible for integrating with the trained neural network to encode input text into vector
// representations, make probabilistic predictions for the next words, decode the output vectors
// back into readable text, and handle generating or completing text sequences
class TextGenerator(
    // Reference to the trained neural network model
    private val network: NeuralNetwork,
    // Reference to the text corpus
    private val corpus: TextCorpus,
    // Size of the word embeddings
    private val embeddingSize: Int,
    private val temperature: Double
) {
    // Random number generator
    private val rng = Random(0L) //TODO: seed ?

    // Helper to locate maximum value index in a vector
    private fun findMaxIndex(vector: DoubleArray): Int {
        var maxValue = Double.Companion.NEGATIVE_INFINITY
        var maxIndex = -1

        for (i in vector.indices) {
            if (vector[i] > maxValue) {
                maxValue = vector[i]
                maxIndex = i
            }
        }

        return maxIndex
    }

    // Samples next word index based on prediction probabilities
    private fun sampleTopKFromDistribution(distribution: DoubleArray, k: Int): Int {
        // Create a list of word indices and their corresponding probabilities

        var candidates: MutableList<Pair<Int, Double>> = ArrayList<Pair<Int, Double>>()

        for (i in distribution.indices) {
            candidates.add(Pair(i, distribution[i]))
        }

        // Sort and retain top k candidates
        candidates.sortWith { e1, e2 -> e2.second compareTo e1.second }
        candidates = candidates.subList(0, min(k, candidates.size))

        // Normalize probabilities of top k candidates
        val sum: Double = candidates.sumOf { it.second }
        candidates = candidates.map{ Pair(it.first, it.second/sum) }.toMutableList()

        // Sample from top k candidates
        val randomValue: Double = rng.nextDouble()
        var cumulativeProbability = 0.0

        for (entry in candidates) {
            cumulativeProbability += entry.second
            if (randomValue < cumulativeProbability) {
                return entry.first
            }
        }

        // Fallback (should not happen, but just in case)
        return candidates.get(rng.nextInt(candidates.size)).first
    }

    private fun softmaxWithTemperature(logits: DoubleArray): DoubleArray {
        val softened = DoubleArray(logits.size)
        var sum = 0.0
        for (i in logits.indices) {
            softened[i] = exp(logits[i] / temperature)
            sum += softened[i]
        }
        for (i in softened.indices) {
            softened[i] /= sum
        }
        return softened
    }

    // Encodes text into an embedding vector
    fun encodeText(text: String): DoubleArray {
        val words: Array<String> = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val embedding = DoubleArray(embeddingSize * words.size)

        for (i in words.indices) {
            val index: Int = corpus.getWordIndex(words[i])

            if (index != -1) {
                val wordEmbedding = network.getWordEmbedding(index)
                arraycopy(wordEmbedding, 0, embedding, i * embeddingSize, embeddingSize)
            }
        }

        return embedding
    }

    // Encodes text into one-hot vector
    fun encodeTextAsVector(text: String): DoubleArray {
        val vocabSize: Int = corpus.vocabSize // Get the size of the vocabulary
        val vector = DoubleArray(vocabSize)

        val index: Int = corpus.getWordIndex(text) // Get the index of the word in the vocabulary

        if (index != -1) {
            vector[index] = 1.0 // Set the element at the index of the word to 1
        }

        return vector
    }

    // Decodes output vector into words
    fun decodeVector(vector: DoubleArray): String {
        var decoded = ""
        var maxIndex = findMaxIndex(vector)
        var iterations = 0

        while (maxIndex != -1 && iterations < vector.size) { // Avoid infinite loops

            val word = corpus.getVocabWord(maxIndex)
            decoded += " $word"
            vector[maxIndex] = Double.Companion.NEGATIVE_INFINITY // Mark as used and ensure it's the lowest value
            maxIndex = findMaxIndex(vector)
            iterations++
        }

        return decoded.trim { it <= ' ' }  // Trim to remove leading space
    }

    // Gets next word predictions from network
    fun predictNext(encodedText: DoubleArray): DoubleArray {
        val output = network.predict(encodedText)
        return softmaxWithTemperature(output)
    }

    // Generates text token by token using network
    fun generateText(initialText: String, length: Int): String {
        var encoded = encodeText(initialText)
        var generated = initialText

        for (i in 0..<length) {
            val output = predictNext(encoded)

            val sampleIndex = sampleTopKFromDistribution(output, 10)

            val word = corpus.getVocabWord(sampleIndex)
            generated += " $word"
            encoded = encodeText(word) // Update encoding with the new word
        }

        return generated
    }

    // Predicts completion for partial text
    fun completeText(initialText: String, maxLength: Int): String {
        // Encode seed text

        val encoded = encodeText(initialText)

        // Predict next words
        val output = predictNext(encoded)

        // Decode predictions
        val completion = decodeVector(output)

        // Combine initial text with completion
        val combinedText = initialText + " " + completion

        // Split the text into words and limit to 25 words
        val words: Array<String> = combinedText.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // Split on spaces
        val limitedText = StringBuilder()

        for (i in 0..<min(words.size, maxLength)) {
            limitedText.append(words[i]).append(" ")
        }

        // Return the truncated or limited text
        return limitedText.toString().trim { it <= ' ' }  // Trim to remove the last space
    }
}