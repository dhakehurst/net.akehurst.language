const agl_module = require('net.akehurst.language-agl-processor');
const Agl = agl_module.net.akehurst.language.agl.processor.Agl;
const AsmElementSimple = agl_module.net.akehurst.language.api.syntaxAnalyser.AsmElementSimple;

const grammarStr = `
namespace test
grammar SimpleExample {

    skip WHITE_SPACE = "\\s+" ;
    skip MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
    skip SINGLE_LINE_COMMENT = "//.*?$" ;

    unit = definition* ;
    definition = classDefinition ;
    classDefinition =
        'class' NAME '{'
            propertyDefinition*
            methodDefinition*
        '}'
    ;

    propertyDefinition = NAME ':' NAME ;
    methodDefinition = NAME '(' parameterList ')' body ;
    parameterList = [ parameterDefinition / ',']* ;
    parameterDefinition = NAME ':' NAME ;
    body = '{' '}' ;

    NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
    BOOLEAN = "true | false" ;
    NUMBER = "[0-9]+([.][0-9]+)?" ;
    STRING = "'(?:\\\\?.)*?'" ;
}
`;
const proc = Agl.processorFromString(grammarStr);

const sentence = `
class class {
  property : String
  method(p1: Integer, p2: String) {
  }
}
`;
const sppt = proc.parse(sentence);
console.info(sppt.toStringAllWithIndent('  '));

const asm = proc.process(null, sentence);

function asmToString(indent, asm) {
    const newIndent = '  '+indent;
    if (typeof(asm)==='string') {
        return asm;
    } else if (asm instanceof AsmElementSimple) {
        var str = ':'+asm.typeName + ' {\n';
        const props = asm.properties.toArray();
        for (const p of props) {
            str += indent + p.name+' = '+asmToString(newIndent, p.value) +'\n';
        }
        str += indent+'}';
        return str;
    } else { // assume a List
        var str = '[\n';
        const arr = asm.toArray();
        for (const el of arr) {
            str += indent + asmToString(newIndent, el) + '\n';
        }
        str += indent+']';
        return str;
    }
}


console.info(asmToString('',asm));
