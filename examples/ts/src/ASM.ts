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

export interface SimpleExampleType {
    treeToString(): string;
}

export class SimpleExampleUnit implements SimpleExampleType {
    definitions: Definition[] = [];

    treeToString() : string {
        return this.definitions.map(def => {
            console.log("Def [" + def + "]");
            return def.treeToString()
        }).join("\n\n");
    }
}

export abstract class Definition implements SimpleExampleType {
    treeToString() : string {
        return "Should be implemented by subclass";
    }
}

export class ClassDefinition extends Definition implements SimpleExampleType {
    name: String;
    properties: PropertyDefinition[] = [];
    // properties = mutableListOf<PropertyDefinition>();
    methods: MethodDefinition[] = [];
    // methods = mutableListOf<MethodDefinition>();
    // TODO members
    // members get() = properties + methods;
    constructor(name: string) {
        super();
        this.name = name;
    }
    treeToString() : string {
        return `
        ${this.name}
        ${this.properties.map(p => p.treeToString()).join("\n")}
        ${this.methods.map(p => p.treeToString()).join("\n")}
        `;
    }
}

export class PropertyDefinition implements SimpleExampleType {
    name: String;
    typeName: String;
    constructor(name: string, typeName: string) {
        this.name = name;
        this.typeName = typeName;
    }

    treeToString() : string {
        return `${this.name} : ${this.typeName}`;
    }
}

export class ParameterDefinition implements SimpleExampleType {
    name: String;
    typeName: String;
    constructor(name: string, typeName: string) {
        this.name = name;
        this.typeName = typeName;
    }

    treeToString() : string {
        return `${this.name} : ${this.typeName}`;
    }
}

export class MethodDefinition implements SimpleExampleType {
    name: String;
    paramList:ParameterDefinition[] = [];
    body: Statement[] = []; // mutableListOf<Statement>();
    constructor(name: string, paramList:ParameterDefinition[]) {
        this.name = name;
        this.paramList.push(...paramList);
    }

    treeToString() : string {        
        return `${this.name} ( ${this.paramList.map(p => p.treeToString()).join(", ")} )
        ${this.body.map(p => p.treeToString()).join(";\n")}`;
    }
}

export abstract class Statement implements SimpleExampleType {
    treeToString() : string {
        throw new Error("Method not implemented.");
    }
}

export class StatementReturn extends Statement{
    expression: Expression;
    treeToString() : string {
        return this.expression.treeToString();
    }
}

export abstract class Expression implements SimpleExampleType {
    treeToString() : string {
        throw new Error("Method not implemented.");
    }
}

export class ExpressionLiteral extends Expression {
    value: any;
    treeToString() : string {
        return this.value;
    }
}

export class ExpressionVariableReference implements SimpleExampleType {
    value: string;
    treeToString() : string {
        return this.value;
    }
}

export class ExpressionInfixOperator extends Expression {
    lhs: Expression;
    operator: string;
    rhs: Expression;
    treeToString() : string {
        return `${this.lhs.treeToString()} ${this.operator} ${this.rhs.treeToString()}`;
    }
}

// for now, as test
export class Leaf implements SimpleExampleType {
    content: string;
    constructor(content: string) {
        this.content = content;
    }
    treeToString() : string {
        return this.content;
    }
}
