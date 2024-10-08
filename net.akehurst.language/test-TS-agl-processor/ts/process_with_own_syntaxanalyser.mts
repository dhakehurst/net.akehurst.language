//import {Agl, KtList} from 'net.akehurst.language-agl-processor';
//import {SpptWalker, SpptDataNodeInfo, SpptDataNode} from 'net.akehurst.language-agl-processor';
import {
    Agl,
    InputLocation,
    KtList,
    KtMap,
    LanguageProcessor,
    Nullable,
    SemanticAnalyser,
    SemanticAnalysisOptions,
    SemanticAnalysisResult, SemanticAnalysisResultDefault,
    Sentence,
    SentenceContext,
    SpptDataNodeInfo,
    SyntaxAnalyserByMethodRegistrationAbstract
} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-processor.mjs';

import {
    IssueHolder
} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-parser.mjs';


const grammarStr = `
namespace test
grammar Test {
    skip leaf WHITESPACE = "\\s+" ;
    skip leaf MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
    skip leaf SINGLE_LINE_COMMENT = "//[\\n\\r]*?" ;

    // literal must be last choice to be highest priority
    // otherwise 'true' | 'false' get matched as predefined.
    value = object | predefined | literal ;

    predefined = IDENTIFIER ;
    object = '{' property* '}' ;
    property = IDENTIFIER ':' value ;

    literal = BOOLEAN | INTEGER | REAL | STRING ;
    
    leaf BOOLEAN = "true|false";
    leaf REAL = "[0-9]+[.][0-9]+";
    leaf STRING = "'([^'\\\\]|\\\\'|\\\\\\\\)*'";
    leaf INTEGER = "[0-9]+";
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
}
`;

interface Value {
}

class LiteralValue implements Value {
    constructor(readonly value: any, readonly typeName: string) {
    }

    toString(): string {
        return `${this.value}:${this.typeName}`;
    }
}

class PredefinedValue implements Value {
    constructor(readonly name: string) {
    }

    public value: any;

    toString(): string {
        return `${this.name}(${this.value})`;
    }
}

class ObjectValue implements Value {
    constructor(readonly properties: Map<string, any>) {
    }

    toString(): string {
        let propStr = "";
        this.properties.forEach((v, k) => {
            propStr += ` ${k}=${'' + v}`;
        });
        return `{${propStr} }`
    }
}

class MySyntaxAnalyser extends SyntaxAnalyserByMethodRegistrationAbstract<Value> {

    registerHandlers() {
        super.registerFor('value', (n, c, s) => this.value(n, c, s))
        super.registerFor('predefined', (n, c, s) => this.predefined(n, c, s))
        super.registerFor('object', (n, c, s) => this.object_(n, c, s))
        super.registerFor('property', (n, c, s) => this.property(n, c, s))
        super.registerFor('literal', (n, c, s) => this.literal(n, c, s))
    }

    // value = object | predefined | literal ;
    private value(nodeInfo: SpptDataNodeInfo, children: KtList<object>, sentence: Sentence): Value {
        return children.asJsReadonlyArrayView()[0] as Value;
    }

    //  predefined = IDENTIFIER ;
    private predefined(nodeInfo: SpptDataNodeInfo, children: KtList<object>, sentence: Sentence): PredefinedValue {
        const name = children.asJsReadonlyArrayView()[0];
        return new PredefinedValue(name);
    }

    // object = '{' property* '}' ;
    private object_(nodeInfo: SpptDataNodeInfo, children: KtList<object>, sentence: Sentence): ObjectValue {
        const loc = super.locationForNode(sentence, nodeInfo.node);
        console.log(loc)
        const props = children.asJsReadonlyArrayView()[1] as KtList<[string, any]>;
        const props2 = props.asJsReadonlyArrayView().values();
        const properties = new Map<string, any>(props2);
        return new ObjectValue(properties);
    }

    // property = IDENTIFIER ':' value
    private property(nodeInfo: SpptDataNodeInfo, children: KtList<object>, sentence: Sentence): [string, any] {
        const name = children.asJsReadonlyArrayView()[0];
        const value = children.asJsReadonlyArrayView()[2];
        return [name, value];
    }

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private literal(nodeInfo: SpptDataNodeInfo, children: KtList<object>, sentence: Sentence): LiteralValue {
        let res: LiteralValue;
        const valueStr = children.asJsReadonlyArrayView()[0];
        switch (nodeInfo.alt.option) {
            case 0 :
                res = new LiteralValue('true' == valueStr, 'Boolean')
                break;
            case 1 :
                res = new LiteralValue(parseInt(valueStr), 'Integer')
                break;
            case  2:
                res = new LiteralValue(parseFloat(valueStr), 'Real')
                break;
            case   3:
                res = new LiteralValue(valueStr, 'String')
                break;
            default:
                throw "Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'";
        }
        return res;
    }

    // if there is no explicit handler leaf nodes return a String (the text matched by the leaf)
    // you can add an explicit handler for leaf rules if wanted.

}

class MyContext implements SentenceContext<Value> {
    constructor(readonly predefined: Map<string, Value>) {
    }
}

class MySemanticAnalyser implements SemanticAnalyser<Value, MyContext> {
    public issues = new IssueHolder();

    clear() {
    }

    analyse(asm: Value, locationMap: Nullable<KtMap<any, InputLocation>> | undefined, context: Nullable<MyContext> | undefined, options: SemanticAnalysisOptions<Value, MyContext>): SemanticAnalysisResult {
        this.analyseValue(asm, context)
        return new SemanticAnalysisResultDefault(this.issues)
    }

    private analyseValue(asm: Value, context: MyContext) {
        switch (true) {
            case asm instanceof LiteralValue:
                break;
            case asm instanceof ObjectValue:
                asm.properties.forEach((v, k) => this.analyseValue(v, context))
                break;
            case asm instanceof PredefinedValue:
                asm.value = context.predefined.get(asm.name)
                if(null==asm.value) {
                    this.issues.error(null, `Predefined value '${asm.name}' not found in context`);
                }
                break;
        }
    }
}


export function process_with_own_syntaxanalyser() {

    console.log("Create Processor")
    const res = Agl.getInstance().processorFromString(
        grammarStr,
        Agl.getInstance().configuration(undefined, (b) => {
            b.syntaxAnalyserResolverResult(() => new MySyntaxAnalyser());
            b.semanticAnalyserResolverResult(() => new MySemanticAnalyser());
        })
    );
    console.log(res.issues.toString());
    const proc: LanguageProcessor<Value, MyContext> = res.processor;

    const sentences = [
        "true", //BOOLEAN
        "1", //INTEGER
        "3.14", //REAL
        "'Hello World!'", // STRING
        "var1", // predefined
        "{}", // empty object
        "{ a:false b:1 c:3.141 d:'bob' e:var2 }", // object
        "{ f:{x:1 y:{a:3 b:7}} }" //nested objects
    ];

    const context = new MyContext(new Map<string, Value>([
        ["var1", new LiteralValue(true, "Boolean")],
        ["var2", new LiteralValue(55, "Integer")]
    ]));

    for (const sentence of sentences) {
        console.log(`Processing: '${sentence}'`)
        const pres = proc.process(sentence, Agl.getInstance().options(undefined, (b) => {
            b.semanticAnalysis((b) => b.context(context));
        }));
        console.log(pres.issues.toString());
        console.log(`  ${pres.asm}`)
    }

}