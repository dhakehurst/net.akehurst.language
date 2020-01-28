import {TabView} from "./TabView.js"
import {AglEditorAce} from "./AglEditorAce"
import * as agl_js from 'net.akehurst.language-agl-processor';
const agl = agl_js.net.akehurst.language;
const Agl = agl.processor.Agl;

TabView.initialise();
const editors = AglEditorAce.initialise();

const expressionEditor = editors.get('expression-text');
const grammarEditor = editors.get('language-grammar');
const styleEditor = editors.get('language-style');
const formatEditor = editors.get('language-format');

grammarEditor.processor = Agl.grammarProcessor;
styleEditor.processor = Agl.styleProcessor;
formatEditor.processor = Agl.formatProcessor;

grammarEditor.setStyle(`
'namespace' {
  font-size: 12pt;
  font-family: "Courier New";
  color: darkgreen;
  font-weight: bold;
}
'grammar' {
  font-size: 12pt;
  font-family: "Courier New";
  color: darkgreen;
  font-weight: bold;
}
'skip' {
  font-size: 12pt;
  font-family: "Courier New";
  color: darkgreen;
  font-weight: bold;
}
"[a-zA-Z_][a-zA-Z_0-9-]*" {
  font-size: 12pt;
  font-family: "Courier New";
  color: darkred;
  font-style: italic;
}
`);

try {
    expressionEditor.processor = Agl.processorFromString(grammarEditor.editor.getValue());
    expressionEditor.setStyle(styleEditor.editor.getValue());
} catch (e) {
    console.error(e);
}

grammarEditor.editor.on("blur", e =>{
    console.info("grammar changed");
    try {
        expressionEditor.processor = Agl.processorFromString(grammarEditor.editor.getValue());
    } catch (e) {
        console.error(e);
    }
});

styleEditor.editor.on("blur", e =>{
   console.info("style changed");
   expressionEditor.setStyle(styleEditor.editor.getValue());
});
