let Agl = require('net.akehurst.language-agl-processor').net.akehurst.language.agl.processor.Agl;

let grammarStr=`
        namespace test
        grammar Test {
          skip WS="\\s+" ;
          S= H W E ;
          H = 'hello' ;
          W = 'world' ;
          E = '!' ;
        }
    `;
console.log('Starting, grammar is:\n'+grammarStr)


let proc = Agl.processorFromStringDefault(grammarStr);
console.log('Processed grammar')
console.log('Errors:\n'+proc.issues.errors)

let result = proc.processor.parse('hello world !');
console.log('Parsed sentence: "hello world !"')
console.log('Errors:\n'+result.issues.errors)
console.log('Parse Tree:\n'+result.sppt.toStringAll );

let result2 = proc.processor.process('hello world !');
console.log('Processed sentence: "hello world !"')
console.log('Errors:\n'+result2.issues.errors)
console.log('Abstract Syntax Model:\n'+result2.asm.asString("  "));