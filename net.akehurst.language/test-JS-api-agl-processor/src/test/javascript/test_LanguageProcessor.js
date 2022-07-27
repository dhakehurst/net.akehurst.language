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

    it('parse_noOptions', function () {
        let result = proc.parse('hello world !');

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('parse_defaultOptions', function () {
        let options = proc.parserOptionsDefault();
        options.goalRuleName="H";
        let result = proc.parse('hello world !');

        assert.notEqual(null, result.sppt);
        console.log( result.sppt.toStringAll );
    });

    it('parse_buildOptions', function () {
        let result = proc.parse("world !", proc.parserOptions(b => {
            b.goalRuleName("H");
        }));

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

    it('process_noOptions', function () {
        let result = proc.process('hello world !');

        assert.notEqual(null, result.asm);
        assert.notEqual(null, result.issues);
    });

    it('process_defaultOptions', function () {
        let options = proc.optionsDefault();
        options.parser.goalRuleName = "H"

        let result = proc.process('world !', options);

        assert.notEqual(null, result.asm);
        assert.notEqual(null, result.issues);
    });
});