const agl_module = require('net.akehurst.language-agl-processor');
const Agl = agl_module.net.akehurst.language.agl.processor.Agl;


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
console.info(sppt);

const asm = proc.process(sentence).toArray();
console.info(asm);
