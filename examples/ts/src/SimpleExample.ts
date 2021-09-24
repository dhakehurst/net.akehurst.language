export const grammarStr: string = `
namespace net.akehurst.language.examples.simple
        grammar SimpleExample {
        
            skip WHITE_SPACE = "\\s+" ;
	        skip MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
	        skip SINGLE_LINE_COMMENT = "//.*?${'$'}" ;
        
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
