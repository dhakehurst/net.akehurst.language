/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.grammarTypeModel.GrammarTypeNamespaceSimple
import net.akehurst.language.agl.language.expressions.typeOfExpressionFor
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple

object TypeModelFromAsmTransform {

    fun build(grammarList: List<Grammar>, asmTransformList: List<AsmTransformModel>): ProcessResult<TypeModel> {
        val grmrTypeModel = TypeModelSimple(asmTransformList.last().name)
        grmrTypeModel.addNamespace(SimpleTypeModelStdLib)
        check(grammarList.size == asmTransformList.size) { "Must pass in same number of Grammars and AsmTransforms" }
        for (i in asmTransformList.indices) {
            val trm = asmTransformList[i]
            val grm = grammarList[i]
            val ns = buildNamespace(grm, trm)
            grmrTypeModel.addNamespace(ns)
        }
        grmrTypeModel.resolveImports()
        val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)
        return ProcessResultDefault(grmrTypeModel, issues)
    }

    fun buildNamespace(grammar: Grammar, trm: AsmTransformModel): GrammarTypeNamespace {
        val nsFromGrmr = GrammarTypeNamespaceFromGrammar(grammar)
        val ns = GrammarTypeNamespaceSimple(
            qualifiedName = trm.qualifiedName,
            imports = mutableListOf(SimpleTypeModelStdLib.qualifiedName)
        )
        for (tr in trm.createObjectRules) {
            //TODO: allow non-owned types to be used
            val dt = ns.findOwnedOrCreateDataTypeNamed(tr.typeName)
            ns.allRuleNameToType[tr.grammarRuleName] = dt.type()
        }
        for (tr in trm.modifyObjectRules) {
            val dt = ns.findOwnedTypeNamed(tr.typeName) ?: error("No type named ${tr.typeName}")
            for (ass in tr.modifyStatements) {
                val grmRule = grammar.findAllResolvedGrammarRule(tr.grammarRuleName) ?: error("No rule found with name ${tr.grammarRuleName}")
                val defaultType = nsFromGrmr.typeForGrammarRule(grmRule)
                dt.appendPropertyPrimitive(
                    name = ass.lhsPropertyName,
                    typeInstance = ass.rhs.typeOfExpressionFor(defaultType) ?: error("should not happen"),
                    description = ""
                )
            }
        }
        return ns
    }

}