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

import kotlin.math.abs
import kotlin.random.Random

// Handles fetching batch data examples from the TextCorpus data source, forwarding them through
// the NeuralNetwork to train the model weights via backpropagation based on the loss, and
// tracking the model accuracy over training epochs
class ModelTrainer(
    // Stores reference to the TextCorpus data source
    private val corpus: TextCorpus,
    // Stores reference to the NeuralNetwork to train
    private val  network: NeuralNetwork,
    // Used to encode text to vector representations
    private val textGenerator: TextGenerator,
    // Stores reference to the batch size
    private val batchSize: Int
) {

    // Random number generator, used for sampling batches
    private val rng: Random =  Random(0)

    private val nextBatch: Array<Array<DoubleArray>>
        // Retrieves next batch of training examples from corpus
        get() {
            val vocabSize: Int = corpus.vocabSize
            // 3D array to hold pairs of input and target
            val batch: Array<Array<DoubleArray>> = Array<Array<DoubleArray>>(batchSize) { Array<DoubleArray>(2) { DoubleArray(vocabSize) } }

            var i = 0
            while (i < batchSize) {
                val index: Int = rng.nextInt(corpus.textSize - 1) // Ensure there's a next word

                val words = corpus.getTextSample(index)!!.split(" ")

                if (words.size >= 2) {
                    val currentWord = words[0] // Current word
                    val nextWord = words[1] // Next word

                    batch[i]!![0] = textGenerator.encodeTextAsVector(currentWord) // Encode current word as input
                    batch[i]!![1] = textGenerator.encodeTextAsVector(nextWord) // Encode next word as target

                    // System.out.println("Input: " + currentWord + ", Target: " + nextWord);
                } else {
                    // Handle edge case where there's only one word or empty text

                    i-- // Redo this iteration with a different random index
                }
                i++
            }

            return batch
        }

    // Calculate the mean error of the predictions for each batch
    private fun computeMeanError(errors: DoubleArray): Double {
        var sum = 0.0

        for (error in errors) {
            sum += abs(error) // Using absolute error; you could also square the errors for Mean Squared Error
        }

        return sum / errors.size
    }

    // Main training loop - gets batches, trains network
    fun trainModel(epochs: Int) {
        for (epoch in 0..<epochs) {
            var totalError = 0.0

            for (batch in 0..<batchSize) {
                val batchData = this.nextBatch
                var batchError = 0.0

                for (j in 0..<batchSize) {
                    val input = batchData[j]!![0]
                    val target = batchData[j]!![1]
                    network.train(input, target)
                    val output = network.predict(input)
                    val error = network.calculateError(output, target)
                    batchError += computeMeanError(error) // Implement computeMeanError to calculate mean error
                }

                batchError /= batchSize.toDouble()
                totalError += batchError
                println(
                    "Epoch " + (epoch + 1) + ", Batch " + (batch + 1) +
                            ", Average Error: " + batchError
                )
            }

            val averageEpochError = totalError / batchSize
            println("End of Epoch " + (epoch + 1) + ", Average Error: " + averageEpochError)
        }
    }
}