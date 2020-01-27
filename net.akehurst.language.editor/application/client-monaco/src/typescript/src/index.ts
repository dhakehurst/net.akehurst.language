import {AglEditorMonaco} from './AglEditorMonaco'
import {TabView} from './TabView'
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

TabView.initialise();
let editors = AglEditorMonaco.initialise();

editors.get('language-grammar').processor = agl.processor.Agl.grammarProcessor;
editors.get('language-grammar').setStyle(`
'grammar' {
    foreground: 00aa00;
    fontStyle: bold;
}
`);

editors.get('language-style').processor = agl.processor.Agl.styleProcessor;
editors.get('language-style').setStyle(`

`);

editors.get('language-format').processor =agl.processor.Agl.formatProcessor;
editors.get('language-format').setStyle(`

`);

editors.get('language-grammar').getMonaco().getModel().onDidChangeContent((e:monaco.editor.IModelContentChangedEvent) => {
    const grammarText = editors.get('language-grammar').getText();
    editors.get('expression-text').processor = agl.processor.Agl.processor(grammarText);
});

editors.get('language-style').getMonaco().getModel().onDidChangeContent((e:monaco.editor.IModelContentChangedEvent) => {
    const styleText = editors.get('language-style').getText();
    editors.get('expression-text').setStyle(styleText);
});