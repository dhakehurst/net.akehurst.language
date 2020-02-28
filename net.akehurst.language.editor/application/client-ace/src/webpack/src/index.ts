import './ace.css'
import './index.css'
import * as kotlin_jsm from 'kotlin';
import $kotlin = kotlin_jsm;
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;
import {AglEditorAce} from 'net.akehurst.language.editor-agl-ace';
import {TreeView} from "./TreeView";
import {TabView} from './TabView.js';

import './examples/classes';
import './examples/agl-grammar';
import './examples/embedded-dot';
import {Examples} from './examples/examples';

const Agl = agl.processor.Agl;
const KList = ($kotlin as any).kotlin.collections.List;
const Kotlin = ($kotlin as any);


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
    sentenceEditor.processor = (Agl as any).processorFromString(grammarEditor.editor.getValue());
    sentenceEditor.setStyle(styleEditor.editor.getValue());
} catch (e) {
    console.error(e);
}

grammarEditor.editor.on("input", (e: Event) => {
    console.info("grammar changed");
    try {
        sentenceEditor.processor = (Agl as any).processorFromString(grammarEditor.editor.getValue());
    } catch (e) {
        sentenceEditor.processor = null;
        console.error(e.message);
    }
});

styleEditor.editor.on("input", (e: Event) => {
    console.info("style changed");
    sentenceEditor.setStyle(styleEditor.editor.getValue());
});

sentenceEditor.element.addEventListener('parseFailed', e => {

    if (sentenceEditor.agl.sppt) {
        const sppt = sentenceEditor.agl.sppt;
        const tree = trees.get("parse");
        const root = tree.root;
        if (e['exception'] && e['exception'].longestMatch) {
            tree.setRoot(
                e['exception'].longestMatch.root,
                {
                    label: n => n.name,
                    hasChildren: n => n.isBranch,
                    children: n => n.children.toArray()
                }
            );
        } else {
            tree.setRoot(
                null,
                {
                    label:n=>'Parse Exception',
                    hasChildren: n=> false
                }
            )
        }
    }
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
        sentenceEditor.doBackgroundTryProcess()
    }
});

sentenceEditor.element.addEventListener('processSuccess', e => {
    if (sentenceEditor.agl.asm) {
        const asm = sentenceEditor.agl.asm;
        const tree = trees.get("asm");
        const root = tree.root;
        tree.setRoot(
            asm,
            {
                label : n => {
                    if (n instanceof agl.api.analyser.AsmElementSimple) {
                        return ':' + n.typeName;
                    } else if (n instanceof agl.api.analyser.AsmElementProperty) {
                        if (n.value instanceof agl.api.analyser.AsmElementSimple) {
                            return n.name + " : " + n.value.typeName;
                        } else if (Kotlin.isType(n.value, KList)) {
                            return n.name +' : List';
                        } else {
                            return n.name + ' = ' + n.value;
                        }
                    } else {
                        return n.toString();
                    }
                },
                hasChildren: n => {
                    if (n instanceof agl.api.analyser.AsmElementSimple) {
                        return !n.properties.isEmpty();
                    } else if (n instanceof agl.api.analyser.AsmElementProperty) {
                        if (n.value instanceof agl.api.analyser.AsmElementSimple) {
                            return true;
                        } else if (Kotlin.isType(n.value, KList)) {
                            return true
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                },
                children : n => {
                    if (n instanceof agl.api.analyser.AsmElementSimple) {
                        return n.properties.toArray();
                    } else if (n instanceof agl.api.analyser.AsmElementProperty) {
                        if (n.value instanceof agl.api.analyser.AsmElementSimple) {
                            return n.value.properties.toArray();
                        } else if (Kotlin.isType(n.value, KList)) {
                            return n.value.toArray()
                        } else {
                            return [];
                        }
                    } else {
                        return [];
                    }
                },
            }
        );
    }
});

exampleSelect.addEventListener('change', (e:Event) => {
    let eg = Examples.map.get((e.target as any).value);
    grammarEditor.editor.setValue(eg.grammar,-1);
    styleEditor.editor.setValue(eg.style,-1);
    formatEditor.editor.setValue(eg.format,-1);
    sentenceEditor.editor.setValue(eg.sentence,-1);
});