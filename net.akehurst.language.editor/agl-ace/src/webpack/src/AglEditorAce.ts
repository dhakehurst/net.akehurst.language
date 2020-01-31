import * as ace from 'ace-builds';
import 'ace-builds/src-noconflict/ext-language_tools';
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;

const autocomplete = ace['require']('ace/autocomplete');

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

export class AglEditorAce {

    static initialise(): Map<string, AglEditorAce> {
        let map = new Map<string, AglEditorAce>();
        document.querySelectorAll('agl-ace').forEach((element: HTMLElement) => {
            let id = element.getAttribute('id');
            let initContent = trimIndent(element.textContent);
            element.textContent = '';
            let editor = new AglEditorAce(element, id, initContent);
            map.set(id, editor);
        });
        return map;
    }

    private nextCssClassNum = 1;
    private cssClassPrefix = 'tok';
    private tokenToClassMap = new Map<string, string>();

    private mode: ace.Ace.SyntaxMode = null;
    public editor: ace.Ace.Editor;

    private sppt: agl.api.sppt.SharedPackedParseTree = null;

    private _processor: agl.api.processor.LanguageProcessor = null;
    public get processor(): agl.api.processor.LanguageProcessor {
        return this._processor;
    }

    public set processor(value: agl.api.processor.LanguageProcessor) {
        this._processor = value;
        this.updateSyntaxMode();
    }

    constructor(
        public element: HTMLElement,
        public languageId: string,
        initialText: string
    ) {
        //TODO: allow passing options
        this.editor = ace.edit(this.element, {
            enableBasicAutocompletion: true
        });
        if (initialText) {
            this.editor.setValue(initialText, -1);
            this.editor.commands.addCommand(autocomplete.Autocomplete.startCommand);
            this.editor.completers = [new AglCompleter()];
        }

        this.editor.on('input',(e:Event) =>{
            console.info("text changed");
            this.doBackgroundTryParse()
        });
    }

    doBackgroundTryParse() {
        if (typeof(Worker) !== "undefined") {
            setTimeout(()=>this.tryParse(), 500)
            //TODO: use web worker stuff
        } else {
            setTimeout(()=>this.tryParse(), 500)
        }
    }

    tryParse() {
        try {
            this.sppt = this.processor.parse(this.editor.getValue());
        } catch (e) {
            this.sppt = null;
            console.error("Error parsing text in "+this.languageId,e)
        }
    }

    private updateSyntaxMode() {
        this.mode = new AglMode(this.languageId, this.processor, this.tokenToClassMap);
        this.editor.getSession().setMode(this.mode);
    }

    mapTokenTypeToClass(tokenType: string): string {
        let cssClass = this.tokenToClassMap.get(tokenType);
        if (!cssClass) {
            cssClass = this.cssClassPrefix + this.nextCssClassNum++;
            this.tokenToClassMap.set(tokenType, cssClass);
        }
        return cssClass;
    }

    setStyle(css: string) {
        const rules = agl.processor.Agl.styleProcessor.process(css);
        let mappedCss = '';
        rules.toArray().forEach((it: agl.api.style.AglStyleRule) => {
            const cssClass = '.' + this.languageId + ' ' + '.ace_' + this.mapTokenTypeToClass(it.selector);
            const mappedRule = new agl.api.style.AglStyleRule(cssClass);
            mappedRule.styles = it.styles;
            mappedCss = mappedCss + '\n' + mappedRule.toCss();
        });
        const module = {cssClass: this.languageId, cssText: mappedCss};
        // remove the current style element for 'languageId' (which is used as the theme name) from the container
        // else the theme css is not reapplied
        const curStyle = document.querySelector('style#' + this.languageId);
        if (curStyle) {
            curStyle.parentElement.removeChild(curStyle);
        }
        // the use of an object instead of a string is undocumented but seems to work
        this.editor.setOption('theme', module); //not sure but maybe this is better than settin gon renderer direct
//        this.editor.renderer.setTheme(module);
    }

}

class AglMode implements ace.Ace.SyntaxMode {

    constructor(
        public languageId: string,
        private processor: agl.api.processor.LanguageProcessor,
        private tokenToClassMap: Map<string, string>
    ) {
    }

    autoOutdent(state: any, doc: ace.Ace.Document, row: number): void {
    }

    checkOutdent(state: any, line: string, input: string): boolean {
        return false;
    }

    createModeDelegates(mapping: { [p: string]: string }): void {
    }

    createWorker(session: ace.Ace.EditSession): any {
    }

    getCompletions(state: string, session: ace.Ace.EditSession, pos: ace.Ace.Point, prefix: string): ace.Ace.Completion[] {
        return [
            {
                value: 'value',
                score: 0,
                meta: 'meta',
                name: 'name',
                caption: 'caption'
            }
        ];
    }

    getKeywords(append?: boolean): Array<string | RegExp> {
        return undefined;
    }

    getNextLineIndent(state: any, line: string, tab: string): string {
        return line.match(/^\s*/)[0];
    }

    getTokenizer(): ace.Ace.Tokenizer {
        return new AglTokenizer(this.processor, this.tokenToClassMap);
    }

    toggleBlockComment(state: any, session: ace.Ace.EditSession, range: ace.Ace.Range, cursor: ace.Ace.Point): void {
    }

    toggleCommentLines(state: any, session: ace.Ace.EditSession, startRow: number, endRow: number): void {
    }

    transformAction(state: string, action: string, editor: ace.Ace.Editor, session: ace.Ace.EditSession, text: string): any {
    }

}

class AglTokenizer implements ace.Ace.Tokenizer {

    constructor(
        private processor: agl.api.processor.LanguageProcessor,
        private tokenToClassMap: Map<string, string>
    ) {
    }

    tryParse() : List<List<agl.api.sppt.SPPTLeaf>> {
        try {

        }
    }

    mapTokenTypeToClass(tokenType: string): string {
        let cssClass = this.tokenToClassMap.get(tokenType);
        if (!cssClass) {
            cssClass = 'nostyle';
        }
        return cssClass;
    }

    createSplitterRegexp(src: string, flag?: string): RegExp {
        return undefined;
    }

    getLineTokens(line: string, startState: string | string[]): any {
        let text = (startState) ? startState + line : line;
        let leafs = this.processor.scan(text);
        let leafArray = leafs.toArray();
        const tokens = leafArray.map((it: agl.api.sppt.SPPTLeaf) => {
            const tokenType = (it.isPattern) ? '"' + it.name + '"' : "'" + it.name + "'";
            const cssClass = this.mapTokenTypeToClass(tokenType);
            return {
                type: cssClass,
                value: it.matchedText,
                //index?: number;
                start: it.startPosition
            }
        });
        return {state: '', tokens};
    }

    removeCapturingGroups(src: string): string {
        return "";
    }

}

class AglCompleter {

    getCompletions(editor, session, pos, prefix, callback) {
        const wordList = ["foo", "bar", "baz"];
        callback(null, wordList.map(function (word) {
            return {
                caption: word,
                value: word,
                meta: "static"
            };
        }));
    }
}
