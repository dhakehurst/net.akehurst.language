import * as api from './AglEditor'
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;

// for more fetures see https://github.com/Microsoft/monaco-editor-samples/blob/master/browser-esm-webpack-small/index.js
import 'monaco-editor/esm/vs/editor/browser/controller/coreCommands';
import 'monaco-editor/esm/vs/editor/contrib/find/findController';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import {StaticServices} from 'monaco-editor/esm/vs/editor/standalone/browser/standaloneServices';

//import {Observable, Subject} from 'rxjs';
var self = {MonacoEnvironment: {}};
self.MonacoEnvironment = {
    getWorkerUrl: function (moduleId: string, label: string) {
        return './main.js';
    }
}

function trimIndent(text: string): string {
    let lines = text.split('\n');
    let firstLine = lines.find(el => el.length > 0);
    var matches;
    var indentation = (matches = /^\s+/.exec(firstLine)) != null ? matches[0] : null;
    if (!!indentation) {
        lines = lines.map(function (line) {
            return line.replace(indentation, '');
        });
        return lines.join('\n').trim();
    }
}

class AglScanTokensProviderState implements monaco.languages.IState {
    constructor(
        public lineNumber: number,
        public lastLeafNextInputPosition: number,
        public leftOverText: string
    ) {
    }

    clone(): monaco.languages.IState {
        return new AglScanTokensProviderState(this.lineNumber, this.lastLeafNextInputPosition, this.leftOverText);
    }

    equals(other: monaco.languages.IState): boolean {
        const otherS = other as AglScanTokensProviderState;
        return this.lastLeafNextInputPosition === otherS.lastLeafNextInputPosition
            && this.leftOverText === otherS.leftOverText
            ;
    }
}

class AglScanTokenProvider implements monaco.languages.TokensProvider {
    constructor(
        private tokenPrefix:string,
        private processor: agl.api.processor.LanguageProcessor
    ) {

    }

    public lastTokensScanned: Map<number, monaco.languages.ILineTokens> = new Map();

    getInitialState(): monaco.languages.IState {
        return new AglScanTokensProviderState(0, 0, '');
    }

    tokenize(line: string, state: monaco.languages.IState): monaco.languages.ILineTokens {
        let lineNumber = state.lineNumber + 1;
        let text = state.leftOverText + line;
        let leafs = this.processor.scan(text);
        let leafArray = leafs.toArray();

        let tokens = leafArray.map((it: agl.api.sppt.SPPTLeaf) => {
            let tokenLabel = "'" + it.name + "'";
            if (it.isPattern) {
                tokenLabel = '"' + it.name + '"';
            }
            return {
                startIndex: it.startPosition,
                scopes: this.tokenPrefix+tokenLabel,
                leaf: it,
                lineNumber

            };
        });
        let result = null;
        if (leafArray.length === 0) {
            let endState = new AglScanTokensProviderState(lineNumber, state.nextInputPosition, state.leftOverText);
            result = {
                tokens,
                endState
            };
        } else {
            let lastLeaf = leafArray[leafArray.length - 1];
            let leftOverText = line.substring(lastLeaf.nextInputPosition, line.length);
            let endState = new AglScanTokensProviderState(lineNumber, lastLeaf.nextInputPosition, leftOverText);
            result = {
                tokens,
                endState
            };
        }
        this.lastTokensScanned.set(lineNumber, result);
        return result;
    }

}

export class AglEditorMonaco implements api.AglEditor {

    static initialise(): Map<string, AglEditorMonaco> {
        let map = new Map<string, AglEditorMonaco>();
        document.querySelectorAll('agl-monaco').forEach((element: HTMLElement) => {
            let id = element.getAttribute('id');
            let initContent = trimIndent(element.textContent);
            element.textContent = '';
            let editor = new AglEditorMonaco(element, id, initContent, {automaticLayout: true});
            map.set(id, editor);
        });
        return map;
    }

    // https://github.com/Microsoft/monaco-editor/issues/338
    // all editors on the same page must share the same theme!
    // hence we create a global theme and modify it as needed.
    private aglGlobalTheme = 'agl-theme';
    private allAglGlobalThemeRules: Map<String, monaco.editor.ITokenThemeRule> = new Map();

    private monacoEditor: monaco.editor.IStandaloneCodeEditor;
    private onChangeHandler: (text: string) => void;
    public languageThemePrefix: string;

    private _tokenProvider: AglScanTokenProvider = null;

    private _processor: agl.api.processor.LanguageProcessor = null;
    public get processor(): agl.api.processor.LanguageProcessor {
        return this._processor;
    }

    public set processor(value: agl.api.processor.LanguageProcessor) {
        this._processor = value;
        this.updateTokensProvider();
    }

    constructor(
        private parentElement: HTMLElement,
        private languageId: string,
        private initialContent: string,
        private editorOptions: any
    ) {
        const self = this;
        this.languageThemePrefix = languageId + '-';
        // https://github.com/Microsoft/monaco-editor/issues/338
        // all editors on the same page must share the same theme!
        // hence we create a global theme and modify it as needed.
        monaco.editor.defineTheme(this.aglGlobalTheme, {
            base: 'vs',
            inherit: false,
            rules: []
        });

        monaco.languages.register({id: languageId});
        monaco.languages.registerHoverProvider(languageId, {
            provideHover: function (model: monaco.editor.ITextModel, position: monaco.Position): monaco.languages.ProviderResult<monaco.languages.Hover> {
                let lineTokens = self._tokenProvider.lastTokensScanned.get(position.lineNumber);
                let tokIndex = 0;
                lineTokens.tokens.find((it: monaco.languages.IToken, index: number) => {
                    tokIndex = index;
                    return it.startIndex > position.column
                });
                let token = lineTokens.tokens[tokIndex - 1];
                if (tokIndex === 0) {
                    token = lineTokens.tokens[0]
                }
                let result = {
                    range: new monaco.Range(1, 1, model.getLineCount(), model.getLineMaxColumn(model.getLineCount())),
                    contents: [{value: token.scopes}]
                };
                return result;
            }
        });

        const a = {language: languageId, value: initialContent, theme: this.aglGlobalTheme};
        const v = {...editorOptions, ...a};

        this.monacoEditor = monaco.editor.create(parentElement, v);

        this.monacoEditor.onDidChangeModelContent((evt: any) => {
            const text = this.getText();
            if (this.onChangeHandler) {
                this.onChangeHandler(text);
            }
        });

        this.monacoEditor.deltaDecorations()
        monaco.languages.registerCompletionItemProvider()
    }

    private getTokensAtLine(model: monaco.editor.ITextModel, lineNumber: number): monaco.languages.IToken[] {
        // Force line's state to be accurate
        let lineToks = model.getLineTokens(lineNumber);
        // Get the tokenization state at the beginning of this line
        var freshState = lineToks.tokens[lineNumber - 1].getState().clone();
        // Get the human readable tokens on this line
        return model._tokenizationSupport.tokenize(model.getLineContent(lineNumber), freshState, 0).tokens;
    }

    layout(): void {
        this.monacoEditor.layout();
    }

    getMonaco() {
        return this.monacoEditor;
    }

    // --- api.Editor ---

    getText(): string {
        try {
            return this.monacoEditor.getModel().getValue()
        } catch (err) {
            console.log("Error: " + err.message)
        }
    }

    setText(value: string): void {
        try {
            return this.monacoEditor.getModel().setValue(value)
        } catch (err) {
            console.log("Error: " + err.message)
        }
    }

    onChange(handler: (text: string) => void): void {
        this.onChangeHandler = handler;
    }

    private updateTokensProvider() {

        this._tokenProvider = new AglScanTokenProvider(this.languageThemePrefix, this.processor);
        monaco.languages.setTokensProvider(this.languageId, this._tokenProvider);

    }


    updateParseTree(tree: api.SharedPackedParseTree): void {
    }

    setStyle(css: string) {
        // https://github.com/Microsoft/monaco-editor/issues/338
        // all editors on the same page must share the same theme!
        // hence we create a global theme and modify it as needed.

        let rules = agl.processor.Agl.styleProcessor.process(css);
        rules.toArray().forEach( (it:agl.api.style.AglStyleRule) => {
            const key = this.languageThemePrefix + it.selector;
            const value = {
                token: key,
                foreground: it.getStyle('foreground'),
                background: it.getStyle('background'),
                fontStyle: it.getStyle('fontStyle')
            };
           this.allAglGlobalThemeRules.set(key, value);
        });
        // reset the theme with the new rules
        monaco.editor.defineTheme(this.aglGlobalTheme, {
            base: 'vs',
            inherit: false,
            rules: this.allAglGlobalThemeRules.values()
        });
    }


}
