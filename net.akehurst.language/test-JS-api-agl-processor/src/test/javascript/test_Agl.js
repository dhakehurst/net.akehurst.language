let assert = require('assert');
let Agl = require('./net.akehurst.language-agl-processor').net.akehurst.language.agl.processor.Agl;
describe('test_Agl', function () {

    it('getBuildStamp', function () {
        let actual = Agl.buildStamp.substring(0,4);
        let expected = '2023';
        assert.equal(actual, expected);
    });

    it('processorFromString', function () {
        let grammarStr=`
            namespace test
            grammar Test {
              skip WS="\\s+";
              S='hello' 'world' '!';
            }
        `;
        let proc = Agl.processorFromString(grammarStr);
        assert.notEqual(null, proc);
    });

});
