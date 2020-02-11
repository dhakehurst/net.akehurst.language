import './ace.css'
import './index.css'
import {TabView} from "./TabView.js"
import {AglEditorAce} from 'net.akehurst.language.editor-agl-ace'
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;
import {TreeView} from "./TreeView";
import {Examples} from "./examples";

const Agl = agl.processor.Agl;

TabView.initialise();
const editors = AglEditorAce.initialise();
const trees = TreeView.initialise();

const exampleSelect = document.querySelector('select#example') as HTMLElement;
Examples.initialise(exampleSelect);

const sentenceEditor = editors.get('expression-text');
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
sentenceEditor.editor.setOptions({
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
'leaf' {
  color: darkgreen;
  font-weight: bold;
}
LITERAL {
  color: blue;
}
PATTERN {
  color: darkblue;
}
IDENTIFIER {
  color: darkred;
  font-style: italic;
}
`);

styleEditor.setStyle(`
IDENTIFIER {
  color: blue;
  font-weight: bold;
}
LITERAL {
  color: blue;
  font-weight: bold;
}
PATTERN {
  color: darkblue;
  font-weight: bold;
}
STYLE_ID {
  color: red;
}
STYLE_ID {
  color: darkred;
  font-style: italic;
}
`);

try {
    sentenceEditor.processor = Agl.processorFromString(grammarEditor.editor.getValue());
    sentenceEditor.setStyle(styleEditor.editor.getValue());
} catch (e) {
    console.error(e);
}

grammarEditor.editor.on("input", (e: Event) => {
    console.info("grammar changed");
    try {
        sentenceEditor.processor = Agl.processorFromString(grammarEditor.editor.getValue());
    } catch (e) {
        sentenceEditor.processor = null;
        console.error(e.message);
    }
});

styleEditor.editor.on("input", (e: Event) => {
    console.info("style changed");
    sentenceEditor.setStyle(styleEditor.editor.getValue());
});

sentenceEditor.element.addEventListener('parseSuccess', e => {

    if (sentenceEditor.agl.sppt) {
        const sppt = sentenceEditor.agl.sppt;
        const tree = trees.get("parse");
        const root = tree.root;
        tree.setRoot(
            sppt.root,
            {
                label : n => n.name,
                hasChildren: n => n.isBranch,
                children : n => n.children.toArray()
            }
        );
    }
});

exampleSelect.addEventListener('change', (e:Event) => {
    let eg = Examples.map.get(e.target.value);
    grammarEditor.editor.setValue(eg.grammar,-1);
    styleEditor.editor.setValue(eg.style,-1);
    formatEditor.editor.setValue(eg.format,-1);
    sentenceEditor.editor.setValue(eg.sentence,-1);
});