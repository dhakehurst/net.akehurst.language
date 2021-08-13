const agl_processor=window['net.akehurst.language-agl-processor'];
const Agl = agl_processor.net.akehurst.language.agl.processor.Agl;
const AutomatonKind = agl_processor.net.akehurst.language.api.processor.AutomatonKind_api;

function parse() {
    try {
        
        const grammarStr = document.getElementById('grammar').value;
        const sentenceStr = document.getElementById('sentence').value;

        const p = Agl.processorFromString(grammarStr);
        const tree = p.parse(sentenceStr, AutomatonKind.LOOKAHEAD_1);

        document.getElementById('result').value = tree.toStringAllWithIndent("  ");
        
    } catch (ex) {
        document.getElementById('result').value = 'ERROR: '+ex
    }
}