import {Examples} from "./examples";

const id = 'classes';
const label = 'Classes';

const sentence = `
class Person {
    name: String
    dob: Date
    friends: List<Person>
}
class class {
    prop: String
}
`;
const grammar = `
namespace test

grammar Test {
    skip WS = "\\s+" ;

    unit = declaration* ;
    declaration = 'class' ID '{' property* '}' ;
    property = ID ':' typeReference ;
    typeReference = ID typeArguments? ;
    typeArguments = '<' [typeReference / ',']+ '>' ;

    leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;

}
`;
const style = `
'class' {
  color: purple;
  font-weight: bold;
}
ID {
  color: red;
  font-style: italic;
}
'{' {
  color: darkgreen;
  font-weight: bold;
}
'}' {
  color: darkgreen;
  font-weight: bold;
}
property {
  background-color: lightgray;
}
typeReference {
  background-color: lightblue;
}
`;
const format = `
`;

Examples.add(id, label, sentence, grammar, style, format);