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
import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.expressions.processor.TypedObjectAny
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain

class GenerateGrammarDomainBuild(
    val grammarTypesDomain: TypesDomain = AglGrammar.typesDomain
) {

    companion object Companion {
        val generatedFormat = $$"""
            namespace net.akehurst.language.grammar
              format Asm {
                OptionHolder -> ""
                
                SimpleName -> value    // TODO: move these to something we extend
                QualifiedName -> value
                
                GrammarDomain -> "
                  grammarDomain(\"$name\") {
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
                    $[extends / '\\n']
                    $[grammarRule / '\\n']
                    $preferenceRule
                  }
                "
                GrammarReference -> "extends(\"${resolved.qualifiedName.value}\")"
                GrammarRule -> when {
                  rhs is Concatenation -> "concatenation(\"$name\") { $rhs }"
                  rhs is Choice -> "choice(\"$name\") { $rhs }"
                  rhs is SimpleList -> "list(\"$name\") { $rhs }"
                  rhs is SeparatedList -> "separatedList(\"$name\", ${rhs.min}, ${rhs.max}) { ${rhs.item}; ${rhs.separator} }"
                  else -> "???(\"$name\") { $rhs }"
                }
                
                RuleItem -> when {
                  $self is EmptyRule -> ''
                  $self is Terminal -> when {
                    $self.isPattern -> "pat(\"$value\")"
                    else -> "lit(\"$value\")"
                  }
                  $self is NonTerminal -> "ref(\"${ruleReference.value}\")"
                  $self is Embedded -> "ebd()"
                  $self is Concatenation -> "$[items / '; ']"
                  $self is Choice -> "
                    $[alternative / '\\n']
                  "
                  $self is Group -> "grp() { $[groupedContent / '; '] }"
                  $self is OptionalItem -> "opt { $item }"
                  $self is SimpleList -> "lst($min, $max) { $item }"
                  $self is SeparatedList -> "sLst($min, $max) { $item; $separator }"
                  else -> "??"
                }
              }
        """
    }

    val formatDomain by lazy {
        val res = Agl.formatDomain(FormatString(generatedFormat),grammarTypesDomain )
        check(res.allIssues.errors.isEmpty()) { println(res.allIssues.errors) } //TODO: handle issues
        res.asm!!
    }
    val formatSet get() = formatDomain.findDefinitionByQualifiedNameOrNull("net.akehurst.language.grammar.Asm".asQualifiedName)!!

    fun generateFromString(grammarString: GrammarString):String {
        val res = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarString.value,
            options = Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry())
                }
            }
        )
        check(res.allIssues.errors.isEmpty()) { println(res.allIssues.errors) } //TODO: handle issues
        val asm = res.asm!!
        return generateFromAsm(asm)
    }

    fun generateFromAsm(grammarDomain: GrammarDomain): String {
        val issues = IssueHolder(LanguageProcessorPhase.FORMAT)
        val og = ObjectGraphByReflection<Any>(AglGrammar.typesDomain, issues)
        val formatter = FormatterOverTypedObject<Any>(formatDomain, og,issues)

        val tp = grammarTypesDomain.findFirstDefinitionByNameOrNull(SimpleName("GrammarDomain"))!!.type()
        val tobj = TypedObjectAny(tp, grammarDomain)
        val res = formatter.formatSelf(formatSet.qualifiedName, tobj)
        check(res.issues.errors.isEmpty()) { println(res.issues.errors) } //TODO: handle issues
        //val str = grammarModel.namespace.joinToString(separator = "\n\n") { generateNamespace(it) }
        return res.sentence!!
    }
}