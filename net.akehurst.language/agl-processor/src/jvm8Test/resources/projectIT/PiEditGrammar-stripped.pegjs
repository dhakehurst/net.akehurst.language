Editor_Definition = projectionGroup

projectionGroup = ws "editor" ws var ws ("precedence" ws numberliteral ws )?
        standardBooleanProjection? ws
        standardReferenceSeparator? ws
        classifierProjection* ws

propProjectionStart     = "${"
propProjectionEnd       = "}"
projection_begin        = "["
projection_end          = "]"
projection_separator    = "|"

standardBooleanProjection = "boolean" ws projection_begin textBut projection_separator textBut projection_end ws

standardReferenceSeparator = "referenceSeparator" ws projection_begin textBut projection_end ws

classifierProjection =
            classifierReference curly_begin ws
                projectionChoice?
                extraClassifierInfo?
            curly_end

/* rule that makes order of projections flexible */
projectionChoice = projection tableProjection?
    / tableProjection projection?

/* rules that make the order of extra info flexible */
extraClassifierInfo = trigger
              extraChoiceSub1?
    / symbol
      extraChoiceSub2?
    / referenceShortcut
      extraChoiceSub3?

extraChoiceSub1 = referenceShortcut
                  symbol?
    / symbol
      referenceShortcut?

extraChoiceSub2 = referenceShortcut
                  trigger?
    / trigger
      referenceShortcut?

extraChoiceSub3 = symbol
                  trigger?
    / trigger
      symbol?
/* END of rules that make order of extra info flexible */

projection = ws projection_begin lineWithOptional* projection_end ws

tableProjection = "table" ws projection_begin ws
                       ( textBut
                                (ws projection_separator ws textBut )* ws
                               )?
                       ( property_projection
                                (ws projection_separator ws property_projection )*
                             ) ws
                   projection_end ws

lineWithOptional = (templateSpace / textItem / optionalProjection / property_projection / superProjection / newline )+

lineWithOutOptional = (templateSpace / textItem / property_projection / superProjection / newline )+

templateSpace = [ \t]+

textItem = anythingBut+

property_projection = singleProperty 
    / listProperty
    / tableProperty
    / booleanProperty

singleProperty = propProjectionStart ws
                         "self."? var (colon_separator var )? ws
                      propProjectionEnd

listProperty = propProjectionStart ws
                         "self."? var (colon_separator var )? ws listInfo? ws
                      propProjectionEnd

tableProperty = propProjectionStart ws
                         "self."? var (colon_separator var )? ws tableInfo? ws
                      propProjectionEnd

booleanProperty = propProjectionStart ws
                         "self."? var (colon_separator var )? ws keywordDecl? ws
                      propProjectionEnd

optionalProjection = projection_begin "?" lineWithOutOptional* projection_end

superProjection = projection_begin "=>" ws classifierReference (colon_separator var )? ws projection_end

tableInfo = "table" ws ("rows" / "columns")? ws

keywordDecl = projection_begin textBut (projection_separator textBut )? projection_end

listInfo =  listDirection? (listInfoType "[" textBut "]" )? ws

listDirection = ("horizontal" / "vertical") ws

listInfoType = ("separator" / "terminator" / "initiator") ws

trigger = "trigger" ws equals_separator ws "\"" string "\"" ws

referenceShortcut = "referenceShortcut" ws equals_separator ws propProjectionStart ws "self."? var propProjectionEnd ws

symbol = "symbol" ws equals_separator ws "\"" string "\"" ws

priority = "priority" ws ":" ws "\"" string "\"" ws

// This rule parses text until one of the special starter chars or string is encountered.
textBut  = anythingBut+

// The 'anythingBut' rule parses text until one of the special starter chars or string is encountered.
// Note that these chars can still be escaped, through the 'char' rule in the basic grammar
// The following are excluded:
// propProjectionStart     = "${"
// projection_begin        = "["
// projection_end          = "]"
// projection_separator    = "|"
anythingBut = !("${" / newline / "[" / "|" / "]") char

newline     = "\r"? "\n"

// These are the parsing rules for the expressions over the language structure,
// as defined in meta/src/languagedef/metalanguage/PiLangExpressions.ts
// They are not meant to be used separately, they should be used in the parser for 
// projectIt parts that use the language expressions.
// Because they are common they are developed and tested separately, together with the
// creator functions in LanguageExpressionCreators.ts.

// the following rules should be part of a parser that wants to use PiLangExpressions.ts

classifierReference = var

langExpression = functionExpression
               / instanceExpression
               / expression                
               / simpleExpression    

instanceExpression = var ':' var

expression = var dotExpression
            / var

dotExpression = '.' var dotExpression?  

functionExpression = var round_begin (
      langExpression
      (comma_separator langExpression )*
    )?
    round_end dotExpression?
    

simpleExpression = numberliteral

// This is a partial grammar file for re-use in other grammars

// the following is basic stuff

curly_begin    = ws "{" ws
curly_end      = ws "}" ws
round_begin    = ws "(" ws
round_end      = ws ")" ws
comma_separator = ws "," ws
semicolon_separator = ws ";" ws
colon_separator  = ws ":" ws
equals_separator  = ws "=" ws
plus_separator = ws "+" ws
ws "whitespace" = (([ \t\n\r]) / (SingleLineComment) / (MultiLineComment) )*
rws "required whitespace" = (([ \t\n\r]) / (SingleLineComment) / (MultiLineComment) )+

var "variable"
  = varLetter identifierChar*

string           = (char)*

varLetter           = [a-zA-Z]
identifierChar      = [a-zA-Z0-9_$] // any char but not /.,!?@~%^&*-=+(){}"':;<>?[]\/
anyChar             = [*a-zA-Z0-9' /\-[\]\|+<>=#$_.,!?@~%^&*-=+(){}:;<>?]
number              = [0-9]

numberliteral     = number+

// from javascript example
SingleLineComment
  = "//" (!LineTerminator SourceCharacter)*

LineTerminator
  = [\n\r\u2028\u2029]

SourceCharacter
  = .

Comment "comment"
  = MultiLineComment
  / SingleLineComment

MultiLineComment
  = "/*" (!"*/" SourceCharacter)* "*/"

// from JSON example
// see also ParserGenUtil.escapeRelevantChars()
char
  = unescaped
  / escape
    (
        '"'
      / "\\"
      / "/"
      / "|"
      / "["
      / "]"
      / "{"
      / "}"
      / "$"
      / "b" 
      / "f" 
      / "n" 
      / "r" 
      / "t" 
      / "u" $(HEXDIG HEXDIG HEXDIG HEXDIG)
    )

escape
  = "\\"

unescaped
  = [^\0-\x1F\x22\x5C]

// ----- Core ABNF Rules -----

// See RFC 4234, Appendix B (http://tools.ietf.org/html/rfc4234).
DIGIT  = [0-9]
HEXDIG = [0-9a-f]
