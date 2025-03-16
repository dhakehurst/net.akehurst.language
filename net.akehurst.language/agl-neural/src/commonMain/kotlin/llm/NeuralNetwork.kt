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

import kotlin.random.Random

// The NeuralNetwork class defines the overall architecture and training logic for the neural
// net, including initializing the layered topology, handling forward and backward passes,
// making predictions, and encapsulating the learning related hyperparameters
class NeuralNetwork(shape: IntArray, learningRate: Double)  {
    private val layers: MutableList<DenseLayer> // Stores the sequence of DenseLayer objects that defines the network topology
    private lateinit var embeddingWeights: Array<DoubleArray?> // Dimensionality of feature vectors representing words

    // Instantiates layers based on provided layer sizes
    init {
        layers = ArrayList<DenseLayer>(shape.size - 1)
        for (i in 1..<shape.size) {
            val inputSize = shape[i - 1] // Set input size to the number of nodes in the previous layer
            layers.add(DenseLayer(shape[i], inputSize, learningRate)) // shape[i] is the number of nodes in the
            // current layer
        }
    }

    // Runs a forward pass, chaining layer propagations
    private fun forwardPropagation(input: DoubleArray): DoubleArray {
        var output = input
        for (layer in layers) {
            output = layer.forwardPropagation(output)
        }
        return output
    }

    // Initialize embeddingWeights
    fun initializeEmbeddingWeights(vocabSize: Int, embeddingSize: Int) {
        this.embeddingWeights = Array<DoubleArray?>(vocabSize) { DoubleArray(embeddingSize) }
        for (i in 0..<vocabSize) {
            for (j in 0..<embeddingSize) {
                this.embeddingWeights[i]!![j] = Random.nextDouble()
            }
        }
    }

    fun getWordEmbedding(wordIndex: Int): DoubleArray {
        checkNotNull(embeddingWeights) { "Embedding weights have not been initialized." }
        require(!(wordIndex < 0 || wordIndex >= embeddingWeights.size)) { "Invalid word index" }
        return embeddingWeights[wordIndex]!!
    }

    // Compares output and target arrays to compute error signal
    fun calculateError(output: DoubleArray, target: DoubleArray): DoubleArray {
        require(output.size == target.size) { "Output and target arrays must have the same length" }
        val errors = DoubleArray(output.size)
        for (i in output.indices) {
            errors[i] = target[i] - output[i]
        }
        return errors
    }

    // Performs a forward pass, backward pass, and layer update loop
    fun train(input: DoubleArray, target: DoubleArray) {
        // Forward pass
        val output = forwardPropagation(input)

        // Backward pass
        var error = calculateError(output, target)
        for (i in layers.indices.reversed()) {
            // Update each layer using backpropagation
            layers.get(i).backwardPropagation(error)
            // Calculate the error for the previous layer, if necessary
            if (i > 0) {
                error = layers.get(i - 1).calculatePreviousLayerError(error) // pass the current error
            }
        }
    }

    // Runs a forward pass to generate output for given input
    fun predict(input: DoubleArray): DoubleArray {
        return forwardPropagation(input)
    }

}