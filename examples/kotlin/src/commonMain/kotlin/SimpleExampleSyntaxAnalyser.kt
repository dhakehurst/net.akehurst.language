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

package net.akehurst.language.examples.simple

import net.akehurst.language.agl.analyser.BranchHandler
import net.akehurst.language.agl.analyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree

class SimpleExampleSyntaxAnalyser : SyntaxAnalyserAbstract() {
    init {
        register("unit", this::unit as BranchHandler<SimpleExampleUnit>)
        register("definition", this::definition as BranchHandler<Definition>)
        register("classDefinition", this::classDefinition as BranchHandler<ClassDefinition>)
        register("propertyDefinition", this::propertyDefinition as BranchHandler<PropertyDefinition>)
        register("methodDefinition", this::methodDefinition as BranchHandler<MethodDefinition>)
        register("parameterDefinition", this::parameterDefinition as BranchHandler<ParameterDefinition>)
    }

    override fun clear() {
        // do nothing
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return transform<T>(sppt.root.asBranch, "") as T
    }

    // unit = definition* ;
    fun unit(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): SimpleExampleUnit {
        val definitions = children[0].branchNonSkipChildren.map {
            transform<Definition>(it, arg)
        }
        return SimpleExampleUnit(definitions)
    }

    // definition = classDefinition ;
    fun definition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Definition {
        return transform(children[0], arg)
    }

    // classDefinition =
    //                'class' NAME '{'
    //                    propertyDefinition*
    //                    methodDefinition*
    //                '}'
    //            ;
    fun classDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): ClassDefinition {
        val name = children[0].nonSkipMatchedText
        val propertyDefinitionList = children[1].branchNonSkipChildren.map {
            transform<PropertyDefinition>(it, arg)
        }
        val methodDefinitionList = children[2].branchNonSkipChildren.map {
            transform<MethodDefinition>(it, arg)
        }
        val classDefinition = ClassDefinition(name)
        classDefinition.properties.addAll(propertyDefinitionList)
        classDefinition.methods.addAll(methodDefinitionList)
        return classDefinition
    }

    //propertyDefinition = NAME ':' NAME ;
    fun propertyDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): PropertyDefinition {
        val name = children[0].nonSkipMatchedText
        val typeName = children[1].nonSkipMatchedText
        return PropertyDefinition(name, typeName)
    }

    //methodDefinition = NAME '(' parameterList ')' body ;
    //parameterList = [ parameterDefinition / ',']* ;
    fun methodDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): MethodDefinition {
        val name = children[0].nonSkipMatchedText
        val paramList = children[1].branchNonSkipChildren[0].branchNonSkipChildren.map {
            transform<ParameterDefinition>(it, arg)
        }
        val method = MethodDefinition(name, paramList)
        // body!
        return method
    }


    //parameterDefinition = NAME ':' NAME ;
    fun parameterDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): ParameterDefinition {
        val name = children[0].nonSkipMatchedText
        val typeName = children[1].nonSkipMatchedText
        return ParameterDefinition(name, typeName)
    }

    //body = '{' statement* '}' ;
}
