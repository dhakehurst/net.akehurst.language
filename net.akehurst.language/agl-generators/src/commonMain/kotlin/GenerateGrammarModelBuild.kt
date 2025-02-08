/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.generators

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.expressions.processor.TypedObjectByReflection
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarNamespace
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.TypeModel

class GenerateGrammarModelBuild(
    val grammarTypeModel: TypeModel = AglGrammar.typeModel
) {

    companion object {
        val generatedFormat = $$"""
            namespace net.akehurst.language.grammar
              format Asm {
                //OptionHolder -> ""
                
                SimpleName -> value    // TODO: move these to something we extend
                QualifiedName -> value
                
                GrammarModel -> "
                  grammarModel(\"$name\") {
                    $options
                    $[namespace / '\\n']
                  }
                "
                GrammarNamespace -> "
                  namespace(\"$qualifiedName\", $import) {
                    $options
                    $[definition / '\\n']
                  }
                "
                Grammar -> "
                  grammar(\"$name\") {
                    $options
                    $extends
                    $grammarRule
                    $preferenceRule
                  }
                "
              }
        """
        val formatModel by lazy {
            val res = Agl.registry.agl.format.processor!!.process(generatedFormat)
            check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
            res.asm!!
        }
        val formatSet = formatModel.findDefinitionOrNullByQualifiedName("net.akehurst.language.grammar.Asm".asQualifiedName)!!
    }

    fun generateFromString(grammarString: GrammarString):String {
        val res = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarString.value,
            options = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            }
        )
        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        val asm = res.asm!!
        return generateFromAsm(asm)
    }

    fun generateFromAsm(grammarModel: GrammarModel): String {
        val issues = IssueHolder(LanguageProcessorPhase.FORMAT)
        val og = ObjectGraphByReflection(AglGrammar.typeModel, issues)
        val formatter = FormatterOverTypedObject<Any>(formatSet, og)

        val tp = grammarTypeModel.findFirstByNameOrNull(SimpleName("GrammarModel"))!!.type()
        val tobj = TypedObjectByReflection(tp, grammarModel)
        val res = formatter.format(tobj)
        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        //val str = grammarModel.namespace.joinToString(separator = "\n\n") { generateNamespace(it) }
        return res.sentence!!
    }

    fun generateNamespace(asm: GrammarNamespace): String {
        val sb = StringBuilder()

        asm.options
        sb.append(asm.import.joinToString(separator = "\n", postfix = "\n") { "import(\"${it}\")" })


        return sb.toString()
    }
}