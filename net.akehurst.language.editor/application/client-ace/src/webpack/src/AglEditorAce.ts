import * as ace from "ace-builds"
import 'ace-builds/src-noconflict/ext-language_tools'
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;
const bgTokenizer = ace['require']('ace/background_tokenizer');
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

class AglComponents {
    nextCssClassNum = 1;
    cssClassPrefix = 'tok';
    tokenToClassMap = new Map<string, string>();

    sppt: agl.api.sppt.SharedPackedParseTree = null;
    processor: agl.api.processor.LanguageProcessor = null;
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

    private mode: ace.Ace.SyntaxMode = null;
    public editor: ace.Ace.Editor;

    agl = new AglComponents();

    public get processor(): agl.api.processor.LanguageProcessor {
        return this.agl.processor;
    }

    public set processor(value: agl.api.processor.LanguageProcessor) {
        this.agl.processor = value;
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
        this.editor.getSession().bgTokenizer = new AglBackgroundTokenizer(new AglTokenizer(this.agl), this.editor);
        this.editor.getSession().bgTokenizer.setDocument(this.editor.getSession().getDocument());

        if (initialText) {
            this.editor.setValue(initialText, -1);
            this.editor.commands.addCommand(autocomplete.Autocomplete.startCommand);
            this.editor.completers = [new AglCompleter()];
        }

        this.editor.on('input', (e: Event) => {
            console.info("text changed");
            this.doBackgroundTryParse()
        });
    }

    doBackgroundTryParse() {
        if (typeof (Worker) !== "undefined") {
            setTimeout(() => this.tryParse(), 500)
            //TODO: use web worker stuff
        } else {
            setTimeout(() => this.tryParse(), 500)
        }
    }

    tryParse() {
        try {
            this.agl.sppt = this.processor.parse(this.editor.getValue());
            this.editor.session.bgTokenizer.start(0);
        } catch (e) {
            this.agl.sppt = null;
            console.error("Error parsing text in " + this.languageId, e)
        }
    }

    private updateSyntaxMode() {
        this.mode = new AglMode(this.languageId, this.agl);
        this.editor.getSession().setMode(this.mode);
    }

    mapTokenTypeToClass(tokenType: string): string {
        let cssClass = this.agl.tokenToClassMap.get(tokenType);
        if (!cssClass) {
            cssClass = this.agl.cssClassPrefix + this.agl.nextCssClassNum++;
            this.agl.tokenToClassMap.set(tokenType, cssClass);
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

class AglBackgroundTokenizer extends bgTokenizer.BackgroundTokenizer {
    constructor(tok:AglTokenizer, ed:ace.Ace.Editor) {
        super(tok, ed);
    }
}

class AglMode implements ace.Ace.SyntaxMode {

    private _tokenizer: AglTokenizer;

    constructor(
        public languageId: string,
        private agl: AglComponents
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
        if (!this._tokenizer) {
            this._tokenizer = new AglTokenizer(this.agl);
        }
        return this._tokenizer;
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
        private agl: AglComponents
    ) {
    }

    mapTokenTypeToClass(tokenType: string): string {
        let cssClass = this.agl.tokenToClassMap.get(tokenType);
        if (!cssClass) {
            cssClass = 'nostyle';
        }
        return cssClass;
    }

    transformToAceTokens(leafArray: agl.api.sppt.SPPTLeaf[]): any {
        return leafArray.map((it: agl.api.sppt.SPPTLeaf) => {
            const tokenType = it.name; //(it.isPattern) ? '"' + it.name + '"' : "'" + it.name + "'";
            const cssClass = this.mapTokenTypeToClass(tokenType);
            let beforeEOL = it.matchedText;
            const eolIndex = it.matchedText.indexOf('\n');
            if (-1 !==eolIndex) {
                beforeEOL = it.matchedText.substr(0,eolIndex);
            }
            return {
                type: cssClass,
                value: beforeEOL,
                //index?: number;
                start: it.startPosition
            }
        });
    }

    getLineTokensByScan(line: string, startState: string | string[],row:number): any {
        if (this.agl.processor) {
            const text = (startState) ? startState + line : line;
            const leafs = this.agl.processor.scan(text);
            const leafArray = leafs.toArray();
            const tokens = this.transformToAceTokens(leafArray);
            return {state: '', tokens};
        } else {
            return {
                state:'',
                tokens: [
                    {
                        type:'nostyle',
                        value:line,
                        start:1
                    }
                ]
            }
        }
    }

    getLineTokensByParse(line: string, startState: any, row:number): any {
        const leafs = this.agl.sppt.tokensByLine.toArray()[row];
        const leafArray = leafs.toArray();
        const tokens = this.transformToAceTokens(leafArray);
        if (startState) {
            tokens.unshift(startState);
        }
        let state = null;
        //TODO: if there are tokens that span multiple lines, then this rows leaf array maybe empty
        const lastLeaf = leafArray[leafArray.length-1];
        if (lastLeaf) {
            const eolIndex = lastLeaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                const afterEOL = lastLeaf.matchedText.substring(eolIndex+1);
                state = {
                    type: this.mapTokenTypeToClass(lastLeaf.name),
                    value: afterEOL,
                    //index?: number;
                    start: 1,
                    line: lastLeaf.location.line +1
                }
            }
        }
        return {state, tokens};
    }

    // --- ace.Ace.Tokenizer ---
    createSplitterRegexp(src: string, flag?: string): RegExp {
        return undefined;
    }

    getLineTokens(line: string, startState: string, row:number): any {
        if (this.agl.sppt) {
            return this.getLineTokensByParse(line, startState, row)
        } else {
            return this.getLineTokensByScan(line, startState, row)
        }
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
