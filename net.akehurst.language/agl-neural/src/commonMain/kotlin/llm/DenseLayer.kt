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

import kotlin.math.exp
import kotlin.random.Random

// The DenseLayer defines a fully-connected neural network layer with input/output nodes,
// weight parameters, bias terms, and methods for the forward and backpropagation computational
// logic during network operations
class DenseLayer(
    // Stores the number of nodes (neurons) in this dense layer
    private val numNodes: Int, inputSize: Int,
    // The learning rate hyperparameter for gradient updates
    private val learningRate: Double
) {
    // The weight parameters between inputs and output nodes
    private val weights: Array<DoubleArray?> = Array<DoubleArray?>(numNodes) { DoubleArray(inputSize) }
    // The bias terms associated with each output node
    private val biases: DoubleArray = DoubleArray(numNodes)
    // Cache of input values from last forward pass
    private lateinit var inputs: DoubleArray
    // Cache of output values from last forward pass
    private lateinit var outputs: DoubleArray

    init {
        // Randomly initializes weight and bias values
        for (i in 0..<numNodes) {
            for (j in weights[i]!!.indices) {
                // Random initialization to prevent each neuron in the network layer from
                // learning the same features during training

                weights[i]!![j] = Random.nextDouble()
            }

            biases[i] = Random.nextDouble()
        }
    }

    // Computes standard sigmoid activation function
    private fun sigmoid(x: Double): Double {
        return 1 / (1 + exp(-x))
    }

    // Computes derivative of sigmoid for backPropagation
    private fun derivativeSigmoid(x: Double): Double {
        val sigmoid = sigmoid(x)
        return sigmoid * (1 - sigmoid)
    }

    // Performs gradient update of parameters
    private fun updateWeightsAndBiases(weightGradients: Array<DoubleArray?>, biasGradients: DoubleArray) {
        for (i in 0..<numNodes) {
            for (j in inputs.indices) {
                weights[i]!![j] += learningRate * weightGradients[i]!![j]
            }
            biases[i] += learningRate * biasGradients[i]
        }
    }

    // Forward propagation to compute layer outputs
    fun forwardPropagation(inputs: DoubleArray): DoubleArray {
        this.inputs = inputs
        this.outputs = DoubleArray(numNodes)
        for (i in 0..<numNodes) {
            outputs[i] = 0.0
            for (j in inputs.indices) {
                outputs[i] += weights[i]!![j] * inputs[j]
            }
            outputs[i] += biases[i]
            outputs[i] = sigmoid(outputs[i])
        }
        return outputs
    }

    // Backward propagation to compute parameter gradients
    fun backwardPropagation(expectedOutputs: DoubleArray) {
        val errors = DoubleArray(numNodes)
        val weightGradients = Array<DoubleArray?>(numNodes) { DoubleArray(inputs.size) }
        val biasGradients = DoubleArray(numNodes)
        for (i in 0..<numNodes) {
            errors[i] = expectedOutputs[i] - outputs[i]
            for (j in inputs.indices) {
                weightGradients[i]!![j] = errors[i] * derivativeSigmoid(outputs[i]) * inputs[j]
            }
            biasGradients[i] = errors[i] * derivativeSigmoid(outputs[i])
        }
        updateWeightsAndBiases(weightGradients, biasGradients)
    }

    // Compute error to propagate further back
    fun calculatePreviousLayerError(nextLayerError: DoubleArray): DoubleArray {
        val previousLayerError = DoubleArray(inputs.size)
        for (i in inputs.indices) {
            for (j in 0..<numNodes) {
                // Propagate error back to the previous layer
                previousLayerError[i] += nextLayerError[j] * weights[j]!![i] * derivativeSigmoid(outputs[j])
            }
        }
        return previousLayerError
    }

}