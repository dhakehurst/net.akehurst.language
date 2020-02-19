import {Examples} from "./examples";

const id = 'embedded-dot';
const label = 'Graphviz DOT Language (XML Embedded in DOT) ';

const sentence = `

`;
const grammar = `
namespace net.akehurst.language.example.dot

grammar Dot  {

    skip SKIP = "\s+" ;

	graph =
	  STRICT?  (GRAPH | DIGRAPH) ID? '{' stmt_list '}'
	;
	
	stmt_list = [ stmt / ';' ]* ;
	
	stmt =
	    node_stmt
      | edge_stmt
      | attr_stmt
      | ID '=' ID
      | subgraph
      ;
      
    node_stmt = node_id attr_list? ;
    node_id = ID port? ;
    port =
        ':' ID (':' compass_pt)? 
      | ':' compass_pt
      ;
    compass_pt	=	('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_') ;
    
    edge_stmt =	(node_id | subgraph) edgeRHS attr_list? ;
    edgeRHS = ( EDGEOP (node_id | subgraph) )+ ;
    EDGEOP = '--' | '->' ;
    
    attr_stmt = (GRAPH | NODE | EDGE) attr_list ;
    attr_list = ( '[' a_list? ']' )+ ;
    a_list = ID '=' ID (';' | ',')? a_list? ;

    subgraph = (SUBGRAPH ID? )? '{' stmt_list '}' ;
	

	STRICT = "[Ss][Tt][Rr][Ii][Cc][Tt]";
	GRAPH = "[Gg][Rr][Aa][Pp][Hh]" ;
	DIGRAPH = "[Dd][Ii][Gg][Rr][Aa][Pp][Hh]" ;
	SUBGRAPH = "[Ss][Uu][Bb][Gg][Rr][Aa][Pp][Hh]" ;
	NODE = "[Nn][Oo][Dd][Ee]" ;
	EDGE = "[Ee][Dd][Gg][Ee]" ;
	
	ID =
	  ALPHABETIC_ID
	| NUMERAL
	| DOUBLE_QUOTE_STRING
	| HTML
	;
	
	ALPHABETIC_ID = "[a-zA-Z\u0080-\u00FF_][a-zA-Z\u0080-\u00FF_0-9]*" ;
	
	NUMERAL = "[-+]?\d+(\.\d+)?" ;
	DOUBLE_QUOTE_STRING = "\"(?:\\?.)*?\"" ;
	HTML = Xml.elementContent ;

}

grammar Xml {

	skip COMMENT = "<!-- [.]* -->" ;
	
	file = element? ;
	
	element = elementEmpty | elementContent ;
	elementEmpty = '<' WS? NAME WS? attribute* '/>' ;
    elementContent = startTag content endTag ;
	startTag = '<' WS? NAME WS? attribute* '>' ;
	endTag = '</' WS? NAME WS? '>' ;

	content = (CHARDATA | element)* ;

	attribute = NAME WS? '=' WS? string WS? ;
	string = DOUBLE_QUOTE_STRING | SINGLE_QUOTE_STRING ;
	WS = "\s" ;
	CHARDATA = "[^<]+" ;
	NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
	DOUBLE_QUOTE_STRING = "[\"][^\"]*[\"]" ;
	SINGLE_QUOTE_STRING = "['][^']*[']" ;
}
`;
const style = `

`;
const format = `
`;

Examples.add(id, label, sentence, grammar, style, format);