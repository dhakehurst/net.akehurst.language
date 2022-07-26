let assert = require('assert');
let Agl = require('./net.akehurst.language-agl-processor').net.akehurst.language.agl.processor.Agl;
describe('test_LanguageProcessor', function () {

    let grammarStr=`
            namespace test
            grammar Test {
              skip WS="\\s+";
              S='hello' 'world' '!';
            }
        `;
    let proc = Agl.processorFromString(grammarStr);

    it('scan', function () {
        let result = proc.scan('hello world !');

        assert.notEqual(null, result);
        assert.equal(5, result.toArray().length);
        assert.notEqual('hello', result.toArray()[0]);
    });

    it('parse', function () {
        let result = proc.parse('hello world !');

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('syntaxAnalysis', function () {
        let parse = proc.parse('hello world !');
        assert.notEqual(null, parse.sppt);
        let result = proc.syntaxAnalysis(parse.sppt);

        assert.notEqual(null, result.asm);
        console.log( result.asm );
    });

    it('semanticAnalysis', function () {
        let parse = proc.parse('hello world !');
        assert.notEqual(null, parse.sppt);
        let synt = proc.syntaxAnalysis(parse.sppt);
        assert.notEqual(null, synt.asm);

        let result = proc.semanticAnalysis(synt.asm);

        assert.notEqual(0, result.issues.size);
    });

    it('process', function () {
        let result = proc.process('hello world !');
        let expected = '2022';
        assert.notEqual(null, result.asm);
        assert.notEqual(null, result.issues);
    });
});