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

import kotlin.test.Test

class test_Main {

    @Test
    fun main() {

        print("\n\nEnter to continue: ")
        //val input: String = scanner.nextLine()
        val input = readln()


        val epochs = 5 // 20;
        val embeddingSize = 150
        var neuralnetworkLayer1 = 0 // Sized to the vocabulary
        var neuralnetworkLayer2 = 500
        var neuralnetworkLayer3 = 300
        var neuralnetworkLayer4 = 0 // Sized to the vocabulary
        val learningRate = 0.005 // Learning rate for gradient updates
        val batchSize = 16 // Number of training samples processed
        val temperature = 0.75 // Randomness in the prediction process

        var neuralNetwork: NeuralNetwork? = null
        var generator: TextGenerator? = null

        println("==========================================================")
        println("LLMed | Large Language Model for Educational Understanding")
        println("==========================================================")


        // Attempt to load a persisted neural network model
//        try {
//            neuralNetwork = ModelManager.loadModel("neuralnetwork.model")
//        } catch (e: IOException) {
//            // Handle the exception, e.g., print an error message or log it
//
//            println("Error loading model: " + e.getMessage())
//        } catch (e: java.lang.ClassNotFoundException) {
//            println("Error loading model: " + e.getMessage())
//        }


        // Create text corpus
        val corpus = TextCorpus()
        println("Load corpus text")
        corpus.loadText("A-Tale-of-Two-Cities-by-Charles-Dickens.txt")
        println("Vocabulary size is: " + corpus.vocabSize)

        if (neuralNetwork == null) {
            println("A NeuralNetwork model was not found. Training a new one now.")
            neuralnetworkLayer1 = corpus.vocabSize
            neuralnetworkLayer4 = corpus.vocabSize

            // Construct network
            println("Create NeuralNetwork")
            val neuralLayers = intArrayOf(neuralnetworkLayer1, neuralnetworkLayer2, neuralnetworkLayer3, neuralnetworkLayer4)
            neuralNetwork = NeuralNetwork(neuralLayers, learningRate)

            // Initialize embeddings
            println("Initialize embeddings")
            neuralNetwork.initializeEmbeddingWeights(corpus.vocabSize, embeddingSize)

            // Create text generator
            println("Create TextGenerator")
            generator = TextGenerator(neuralNetwork, corpus, embeddingSize, temperature)

            // Initialize trainer
            println("Create ModelTrainer")
            val trainer = ModelTrainer(corpus, neuralNetwork, generator, batchSize)

            // Train the model
            println("Train model")
            trainer.trainModel(epochs)
        } else {
            println("Loading NeuralNetwork model.")

            // Create text generator
            println("Create TextGenerator")
            generator = TextGenerator(neuralNetwork, corpus, embeddingSize, temperature)
        }


        // Test completion function
        println("\n\nComplete this text: In his expostulation he dropped his cleaner hand...")
        val completed = generator.completeText("In his expostulation he dropped his cleaner hand", 65)
        println(completed)


        // Test generation function
        println("\n\nGenerate some text: This dialogue had been held in so very low a whisper...")
        val generated = generator.generateText("This dialogue had been held in so very low a whisper", 24)
        println(generated)


        // Prompt user to enter text for completion
//        val scanner: Scanner = Scanner(java.lang.System.`in`)

        while (true) {
            print("\n\nEnter text to complete (quit|save): ")
            //val input: String = scanner.nextLine()
            val input = readln()

            if (input.equals("quit", ignoreCase = true)) {
                break
            }

//            if (input.equals("save", ignoreCase = true)) {
//                try {
//                    ModelManager.saveModel(neuralNetwork, "neuralnetwork.model")
//                } catch (e: IOException) {
//                    println("Error saving model: " + e.getMessage())
//                }
//                break
//            }

            val response = generator.completeText(input, 25)

            println("\n" + response)
        }

//        scanner.close()
    }

}