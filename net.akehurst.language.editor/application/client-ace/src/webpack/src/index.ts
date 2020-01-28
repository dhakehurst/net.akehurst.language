import {TabView} from "./TabView.js"
import {AglEditorAce} from "./AglEditorAce"
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;
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

grammarEditor.editor.setOptions({
    fontFamily: 'Courier New',
    fontSize: '12pt',
});
styleEditor.editor.setOptions({
    fontFamily: 'Courier New',
    fontSize: '12pt',
});
formatEditor.editor.setOptions({
    fontFamily: 'Courier New',
    fontSize: '12pt',
});
expressionEditor.editor.setOptions({
    fontFamily: 'Courier New',
    fontSize: '12pt',
});

grammarEditor.setStyle(`
'namespace' {
  color: darkgreen;
  font-weight: bold;
}
'grammar' {
  color: darkgreen;
  font-weight: bold;
}
'skip' {
  color: darkgreen;
  font-weight: bold;
}
"'(?:\\\\\\\\?.)*?'" {
  color: blue;
}
"\\\"(?:\\\\\\\\?.)*?\\\"" {
  color: darkblue;
}
"[a-zA-Z_][a-zA-Z_0-9-]*" {
  color: darkred;
  font-style: italic;
}
`);

styleEditor.setStyle(`
"'(?:\\\\\\\\?.)*?'" {
  color: blue;
}
"\\\"(?:[^\\\"\\\\\\\\]|\\\\\\\\.)*?\\\"" {
  font-family: "Courier New";
  color: darkblue;
}
"[a-zA-Z_][a-zA-Z_0-9-]*" {
  color: red;
}
"[-a-zA-Z_][-a-zA-Z_0-9-]*(?=\\\\s*[:])" {
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

grammarEditor.editor.on("blur", (e:Event) =>{
    console.info("grammar changed");
    try {
        expressionEditor.processor = Agl.processorFromString(grammarEditor.editor.getValue());
    } catch (e) {
        console.error(e);
    }
});

styleEditor.editor.on("blur", (e:Event) =>{
   console.info("style changed");
   expressionEditor.setStyle(styleEditor.editor.getValue());
});
