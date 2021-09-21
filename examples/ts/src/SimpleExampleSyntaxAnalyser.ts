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

// const agl_module = require('net.akehurst.language-agl-processor');
// see https://kotlinlang.org/docs/js-to-kotlin-interop.html#jsexport-annotation

import {net} from "net.akehurst.language-agl-processor";
import {
    ClassDefinition,
    Definition, Leaf,
    MethodDefinition,
    ParameterDefinition,
    PropertyDefinition, SimpleExampleType,
    SimpleExampleUnit
} from "./ASM";
import SyntaxAnalyser = net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser;
import SyntaxAnalyserException = net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyserException;
import SharedPackedParseTree = net.akehurst.language.api.sppt.SharedPackedParseTree;
import SPPTBranch = net.akehurst.language.api.sppt.SPPTBranch;
import SPPTLeaf = net.akehurst.language.api.sppt.SPPTLeaf;
import SPPTNode = net.akehurst.language.api.sppt.SPPTNode;
import SyntaxAnalyserAbstract = net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract;

export class SimpleExampleSyntaxAnalyser implements SyntaxAnalyser {
    constructor() {
    }

    locationMap: any;

    clear(): void {
        throw new Error("Method not implemented.");
    }

    transform<T>(sppt: SharedPackedParseTree): T {
        console.log(`sppt: ${sppt.root.name}, maxNumHeads: ${sppt.maxNumHeads}, countTrees: ${sppt.countTrees}, root: ${sppt.root}`);

        if (!!sppt.root) {
            return this.transformNode(sppt.root) as unknown as T;
        } else {
            return null;
        }
    }

    private transformNode(node: SPPTNode, arg?: any): any {
        if (node.isLeaf) {
            return this.transformLeaf(node as SPPTLeaf, arg)
        } else if (node.isBranch) {
            return this.transformBranch(node as SPPTBranch, arg)
        } else {
            //should error
            return null;
        }
    }

    private transformBranch(branch: SPPTBranch, arg?: any): any {
        var brName = branch.name;
        if('unit' == brName) {
            return this.unit(branch)
        } else if ('definition' == brName) {
            return this.definition(branch)
        } else if ('classDefinition' == brName) {
            return this.classDefinition(branch)
        } else if ('propertyDefinition' == brName) {
            return this.propertyDefinition(branch)
        } else if ('methodDefinition' == brName) {
            return this.methodDefinition(branch)
        } else if ('parameterDefinition' == brName) {
            return this.parameterDefinition(branch)
        } else {
            throw `Error: ${brName} not handled`;
        }

    }

    private transformLeaf(leaf: SPPTLeaf, arg?: any): string {
        return leaf.matchedText;
    }

    // unit = definition* ;
    unit(branch: SPPTBranch): SimpleExampleUnit {
        let result: SimpleExampleUnit = new SimpleExampleUnit();
        for (const child of branch.branchNonSkipChildren.toArray()[0].branchNonSkipChildren.toArray()) {
            const def = this.transformNode(child);
            result.definitions.push(def);
        }
        return result;
    }

    // definition = classDefinition ;
    definition(branch: SPPTBranch): Definition {
        return this.transformNode(branch.branchNonSkipChildren.toArray()[0]);
    }

    /*
    classDefinition =
        'class' NAME '{'
            propertyDefinition*
            methodDefinition*
        '}'
    ;
     */
    classDefinition(branch:SPPTBranch): ClassDefinition {
        const name = branch.branchNonSkipChildren.toArray()[0].matchedText;
        const asm = new ClassDefinition(name);
        for(const pCh of branch.branchNonSkipChildren.toArray()[1].branchNonSkipChildren.toArray()) {
            const pDef = this.transformBranch(pCh);
            asm.properties.push(pDef);
        }
        for(const mCh of branch.branchNonSkipChildren.toArray()[2].branchNonSkipChildren.toArray()) {
            const mDef = this.transformBranch(mCh);
            asm.methods.push(mDef);
        }
        return asm;
    }

    //propertyDefinition = NAME ':' NAME ;
    propertyDefinition(branch: SPPTBranch): PropertyDefinition {
        let name = branch.branchNonSkipChildren.toArray()[0].nonSkipMatchedText;
        let typeName = branch.branchNonSkipChildren.toArray()[1].nonSkipMatchedText;
        return new PropertyDefinition(name, typeName);
    }

    //methodDefinition = NAME '(' parameterList ')' body ;
    //parameterList = [ parameterDefinition / ',']* ;
    methodDefinition(branch: SPPTBranch): MethodDefinition {
        let name = branch.branchNonSkipChildren.toArray()[0].nonSkipMatchedText
        let paramList = branch.branchNonSkipChildren.toArray()[1].branchNonSkipChildren.toArray()[0].branchNonSkipChildren.toArray().map (it =>
            this.transformNode(it)
        )
        let method = new MethodDefinition(name, paramList);
        // body!
        return method;
    }


    //parameterDefinition = NAME ':' NAME ;
    parameterDefinition(branch: SPPTBranch): ParameterDefinition {
        let name = branch.branchNonSkipChildren.toArray()[0].nonSkipMatchedText;
        let typeName = branch.branchNonSkipChildren.toArray()[1].nonSkipMatchedText;
        return new ParameterDefinition(name, typeName);
    }

    //body = '{' statement* '}' ;
}
