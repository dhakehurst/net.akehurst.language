import 'ace-builds/src-noconflict/ace';
//import 'ace-builds/webpack-resolver';
import 'ace-builds/src-noconflict/ext-language_tools';
import * as agl_js from 'net.akehurst.language-agl-processor';
import agl = agl_js.net.akehurst.language;

const Editor = ace.require("./editor").Editor;
const VirtualRenderer = ace.require("./virtual_renderer").VirtualRenderer;
const BackgroundTokenizer = ace.require('ace/background_tokenizer').BackgroundTokenizer;
//const TextLayer = ace.require("./layer/text").Text;
const autocomplete = ace.require('ace/autocomplete');
const Range = ace.require('ace/range').Range;

//const oop = ace.require("./lib/oop");
//const dom = ace.require("./lib/dom");
//const config = ace.require("./config");

declare var ace: any;

function trimIndent(text: string): string {
    let lines = text.split('\n');
    let firstLine = lines.find(el => el.length > 0);
    if (firstLine) {
        var matches;
        var indentation = (matches = /^\s+/.exec(firstLine)) != null ? matches[0] : null;
        if (!!indentation) {
            lines = lines.map(function (line) {
                return line.replace(indentation, '');
            });
            return lines.join('\n').trim();
        }
    }
    return text;
}

class AglComponents {
    nextCssClassNum = 1;
    cssClassPrefix = 'tok';
    tokenToClassMap = new Map<string, string>();

    processor: agl.api.processor.LanguageProcessor = null;
    goalRule: string = null;
    sppt: agl.api.sppt.SharedPackedParseTree = null;
    asm: agl.api.analyser.AsmElementSimple = null;
}

export class AglEditorAce {

    static initialise(): Map<string, AglEditorAce> {
        let map = new Map<string, AglEditorAce>();
        document.querySelectorAll('agl-ace').forEach((element: Element) => {
            let langId = element.getAttribute('id');
            let edId = langId; //TODO: make this different
            let initContent = trimIndent(element.textContent);
            element.textContent = '';
            let editor = new AglEditorAce(element as HTMLElement, edId, langId, null, initContent, {});
            map.set(edId, editor);
        });
        return map;
    }

    private mode: any = null; //: ace.SyntaxMode = null;
    public editor: any; // ace.Editor;
    errorParseMarkerIds = [];
    errorProcessMarkerIds = [];

    agl = new AglComponents();

    public get processor(): agl.api.processor.LanguageProcessor {
        return this.agl.processor;
    }

    public set processor(value: agl.api.processor.LanguageProcessor) {
        this.agl.processor = value;
        this.updateSyntax();
    }

    constructor(
        public element: HTMLElement,
        public editorId: string,
        public languageId: string, //TODO: add an editorId also!
        public goalRule: string,
        initialText: string,
        options: any
    ) {
        //TODO: allow passing options
        let doc = ace.createEditSession(initialText);
        this.editor = new Editor(new VirtualRenderer(this.element, null), doc, options);

        //this.editor = new ace.edit(this.element, {
        //    enableBasicAutocompletion: true
        //});
        //this.editor.getSession().renderer = new AglRenderer(this.element);
        this.editor.getSession().bgTokenizer = new AglBackgroundTokenizer(new AglTokenizer(this.agl), this.editor);
        this.editor.getSession().bgTokenizer.setDocument(this.editor.getSession().getDocument());
        this.mode = new AglMode(this.languageId, this.agl);

        this.setupCommands();

        // set default style
        this.setStyle(`
            $keyword {
              color: purple;
              font-weight: bold;
            }
        `);

        if (initialText) {
            this.editor.setValue(initialText, -1);
            this.editor.commands.addCommand(autocomplete.Autocomplete.startCommand);
            this.editor.completers = [new AglCompleter(this.languageId,this.agl)];
        }

        // delete the sppt if text is changed.
        // Any change will make the parsed tree invalid, w.r.t. locations in the leaves.
        this.editor.on('change', (e: Event) => {
            console.info("text changed");
            this.agl.sppt = null;
        });
        this.editor.on('input', (e: Event) => {
            console.info("text input");
            this.doBackgroundTryParse()
        });
    }

    setupCommands() {
        this.editor.commands.addCommand({
            name: 'format',
            bindKey: {win: 'Ctrl-F', mac: 'Command-F'},
            exec: (editor) => this.format(),
            readOnly: false
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

    doBackgroundTryProcess() {
        if (typeof (Worker) !== "undefined") {
            setTimeout(() => this.tryProcess(), 500)
            //TODO: use web worker stuff
        } else {
            setTimeout(() => this.tryProcess(), 500)
        }
    }

    tryParse() {
        if (this.processor) {
            try {
                this.editor.getSession().clearAnnotations();
                this.errorParseMarkerIds.forEach(id => this.editor.getSession().removeMarker(id));

                if (this.goalRule) {
                    this.agl.sppt = (this.processor as any).parseForGoal(this.goalRule, this.editor.getValue()); // cast to any because .d.ts file has wrong name for 'parseForGoal'
                } else {
                    this.agl.sppt = this.processor.parse(this.editor.getValue());
                }
                const event = document.createEvent("HTMLEvents");
                event.initEvent('parseSuccess', true, true);
                this.element.dispatchEvent(event);
                //start getTokens from parse result
                this.updateSyntax();
            } catch (e) {
                if (e.name == 'ParseFailedException') {
                    this.agl.sppt = null;
                    const event = document.createEvent("HTMLEvents");
                    event.initEvent('parseFailed', true, true);
                    event['exception'] = e;
                    this.element.dispatchEvent(event);
                    // parse failed so re-tokenize from scan
                    this.updateSyntax();
                    console.error("Error parsing text in " + this.editorId + ' for language ' + this.languageId, e.message);
                    const errors = [];
                    errors.push(new AglErrorAnnotation(
                        e.location.line,
                        e.location.column - 1,
                        "Syntax Error",
                        "error",
                        e.message
                    ));
                    this.editor.getSession().setAnnotations(errors);
                    errors.forEach(e => {
                        const range = new Range(e.row, e.column, e.row, e.column + 1);
                        const cls = 'ace_marker_text_error';
                        const errMrkId = this.editor.getSession().addMarker(range, cls, 'text');
                        this.errorParseMarkerIds.push(errMrkId);
                    });
                } else {
                    console.error("Error parsing text in " + this.editorId + ' for language ' + this.languageId, e.message);
                }
            }
        }
    }

    tryProcess() {
        if (this.processor && this.agl.sppt) {
            try {
                this.editor.getSession().clearAnnotations(); //assume there are no parse errors or there would be no sppt!
                this.errorProcessMarkerIds.forEach(id => this.editor.getSession().removeMarker(id));

                this.agl.asm = (this.processor as any).processFromSPPT(this.agl.sppt); // cast to any because .d.ts file has wrong name for 'processFromSPPT'

                const event = document.createEvent("HTMLEvents");
                event.initEvent('processSuccess', true, true);
                this.element.dispatchEvent(event);
            } catch (e) {
                if (e.name == 'SyntaxAnalyserException') {
                    this.agl.sppt = null;
                    const event = document.createEvent("HTMLEvents");
                    event.initEvent('processFailed', true, true);
                    event['exception'] = e;
                    this.element.dispatchEvent(event);

                    /*
                    const errors = [];
                    errors.push(new AglErrorAnnotation(
                        e.location.line,
                        e.location.column - 1,
                        "Syntax Error",
                        "error",
                        e.message
                    ));
                    this.editor.getSession().setAnnotations(errors);
                    errors.forEach(e => {
                        const range = new Range(e.row, e.column, e.row, e.column + 1);
                        const cls = 'ace_marker_text_error';
                        const errMrkId = this.editor.getSession().addMarker(range, cls, 'text');
                        this.errorProcessMarkerIds.push(errMrkId);
                    });
                     */
                } else {
                    console.error("Error processing parse result in " + this.editorId + ' for language ' + this.languageId, e.message);
                }
            }
        }

    }

    updateSyntax() {
        this.editor.renderer.updateText();
        this.editor.session.bgTokenizer.start(0);
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
        if (css) {
            const rules = agl.processor.Agl.styleProcessor.process(css);
            let mappedCss = '';
            rules.toArray().forEach((it: agl.api.style.AglStyleRule) => {
                const cssClass = '.' + this.languageId + ' ' + '.ace_' + this.mapTokenTypeToClass(it.selector);
                const mappedRule = new agl.api.style.AglStyleRule(cssClass);
                mappedRule.styles = it.styles;
                mappedCss = mappedCss + '\n' + mappedRule.toCss();
            });
            const module = {cssClass: this.languageId, cssText: mappedCss, _v: Date.now()}; // _v:Date added in order to force use of new module definition
            // remove the current style element for 'languageId' (which is used as the theme name) from the container
            // else the theme css is not reapplied
            const curStyle = document.querySelector('style#' + this.languageId);
            if (curStyle) {
                curStyle.parentElement.removeChild(curStyle);
            }
            // the use of an object instead of a string is undocumented but seems to work
            this.editor.setOption('theme', module); //not sure but maybe this is better than setting on renderer direct

            // force refresh of renderer
            this.updateSyntax();
        }
    }

    format() {
        if (this.agl.processor) {
            const pos = this.editor.selection.getCursor();
            const text = this.editor.getValue();
            const formattedText = this.agl.processor.format(text);
            this.editor.setValue(formattedText, -1);
        }
    }

}

class AglErrorAnnotation {

    public row = -1;

    constructor(
        public line: number,
        public column: number,
        public text: string,
        public type: string,
        public raw: string,
    ) {
        this.row = line - 1;
    }
}

/*
class AglRenderer extends VirtualRenderer {

    constructor(container, theme) {
        super(container, theme);
        // have to redo much of the constructor, because we replace the $textLayer
        const _self = this;
        var textLayer = this.$textLayer = new AglTextLayer(this.content);
        this.canvas = textLayer.element;
        this.$textLayer.$setFontMetrics(this.$fontMetrics);
        this.$textLayer.addEventListener("changeCharacterSize", function(e) {
            _self.updateCharacterSize();
            _self.onResize(true, _self.gutterWidth, _self.$size.width, _self.$size.height);
            _self._signal("changeCharacterSize", e);
        });

        this.updateCharacterSize();
        this.setPadding(4);
        config.resetOptions(this);
        config._signal("renderer", this);
    }

}

class AglTextLayer extends TextLayer {
    $$$$ = '*****';

    constructor(parentEl:any) {
        super(parentEl);
        oop.inherits(this,TextLayer);

        this.$renderToken = function(parent, screenColumn, token, value) {
            var self = this;
            var re = /(\t)|( +)|([\x00-\x1f\x80-\xa0\xad\u1680\u180E\u2000-\u200f\u2028\u2029\u202F\u205F\uFEFF\uFFF9-\uFFFC]+)|(\u3000)|([\u1100-\u115F\u11A3-\u11A7\u11FA-\u11FF\u2329-\u232A\u2E80-\u2E99\u2E9B-\u2EF3\u2F00-\u2FD5\u2FF0-\u2FFB\u3001-\u303E\u3041-\u3096\u3099-\u30FF\u3105-\u312D\u3131-\u318E\u3190-\u31BA\u31C0-\u31E3\u31F0-\u321E\u3220-\u3247\u3250-\u32FE\u3300-\u4DBF\u4E00-\uA48C\uA490-\uA4C6\uA960-\uA97C\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uF900-\uFAFF\uFE10-\uFE19\uFE30-\uFE52\uFE54-\uFE66\uFE68-\uFE6B\uFF01-\uFF60\uFFE0-\uFFE6]|[\uD800-\uDBFF][\uDC00-\uDFFF])/g;

            var valueFragment = this.dom.createFragment(this.element);

            var m;
            var i = 0;
            while (m = re.exec(value)) {
                var tab = m[1];
                var simpleSpace = m[2];
                var controlCharacter = m[3];
                var cjkSpace = m[4];
                var cjk = m[5];

                if (!self.showInvisibles && simpleSpace)
                    continue;

                var before = i != m.index ? value.slice(i, m.index) : "";

                i = m.index + m[0].length;

                if (before) {
                    valueFragment.appendChild(this.dom.createTextNode(before, this.element));
                }

                if (tab) {
                    var tabSize = self.session.getScreenTabSize(screenColumn + m.index);
                    valueFragment.appendChild(self.$tabStrings[tabSize].cloneNode(true));
                    screenColumn += tabSize - 1;
                } else if (simpleSpace) {
                    if (self.showInvisibles) {
                        var span = this.dom.createElement("span");
                        span.className = "ace_invisible ace_invisible_space";
                        span.textContent = lang.stringRepeat(self.SPACE_CHAR, simpleSpace.length);
                        valueFragment.appendChild(span);
                    } else {
                        valueFragment.appendChild(this.com.createTextNode(simpleSpace, this.element));
                    }
                } else if (controlCharacter) {
                    var span = this.dom.createElement("span");
                    span.className = "ace_invisible ace_invisible_space ace_invalid";
                    span.textContent = lang.stringRepeat(self.SPACE_CHAR, controlCharacter.length);
                    valueFragment.appendChild(span);
                } else if (cjkSpace) {
                    // U+3000 is both invisible AND full-width, so must be handled uniquely
                    screenColumn += 1;

                    var span = this.dom.createElement("span");
                    span.style.width = (self.config.characterWidth * 2) + "px";
                    span.className = self.showInvisibles ? "ace_cjk ace_invisible ace_invisible_space" : "ace_cjk";
                    span.textContent = self.showInvisibles ? self.SPACE_CHAR : cjkSpace;
                    valueFragment.appendChild(span);
                } else if (cjk) {
                    screenColumn += 1;
                    var span = this.dom.createElement("span");
                    span.style.width = (self.config.characterWidth * 2) + "px";
                    span.className = "ace_cjk";
                    span.textContent = cjk;
                    valueFragment.appendChild(span);
                }
            }

            valueFragment.appendChild(this.dom.createTextNode(i ? value.slice(i) : value, this.element));

            if (!this.$textToken[token.type]) {
                var classes = token.styles.map( (it:string) => "ace_"+it).join(' ');
                var span = this.dom.createElement("span");
                if (token.type == "fold")
                    span.style.width = (token.value.length * this.config.characterWidth) + "px";

                span.className = classes;
                span.appendChild(valueFragment);

                parent.appendChild(span);
            }
            else {
                parent.appendChild(valueFragment);
            }

            return screenColumn + value.length;
        };
    }
}
*/
class AglBackgroundTokenizer extends BackgroundTokenizer {
    constructor(tok: AglTokenizer, ed: ace.Editor) {
        super(tok, ed);
    }
}

class AglMode implements ace.SyntaxMode {

    private _tokenizer: AglTokenizer;

    constructor(
        public languageId: string,
        private agl: AglComponents
    ) {
    }

    autoOutdent(state: any, doc: ace.Document, row: number): void {
        const x = false;
    }

    checkOutdent(state: any, line: string, input: string): boolean {
        return false;
    }

    createModeDelegates(mapping: { [p: string]: string }): void {
    }

    createWorker(session: ace.EditSession): any {
    }

    getCompletions(state: string, session: ace.EditSession, pos: ace.Point, prefix: string): ace.Completion[] {
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
        return ''; //line.match(/^\s*/)[0];
    }

    getTokenizer(): ace.Tokenizer {
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

class AglLineToken {

    type: string;

    constructor(
        public styles: string[],
        public value: string,
        public start: number,
        public line: number
    ) {
        if (this.styles.length > 0) {
            this.type = styles.join(".");
        } else {
            this.type = 'nostyle';
        }
    }
}

class AglTokenizer implements ace.Tokenizer {

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

    mapTagListToCssClasses(tagArray: string[]): string[] {
        let cssClasses = tagArray.map((it: string) => {
            let cssClass = this.agl.tokenToClassMap.get(it);
            if (!cssClass) {
                cssClass = null;
            }
            return cssClass;
        }).filter((it: string) => null !== it); //TODO: find faster way than filter
        return cssClasses;
    }

    mapToCssClasses(leaf: agl.api.sppt.SPPTLeaf): string[] {
        let metaTagClasses = this.mapTagListToCssClasses(leaf.metaTags.toArray());
        let otherClasses = [];
        if (!leaf.tagList.isEmpty()) {
            otherClasses = this.mapTagListToCssClasses(leaf.tagList.toArray());
        } else {
            otherClasses = [this.mapTokenTypeToClass(leaf.name)];
        }
        let classes = [...metaTagClasses, ...otherClasses];
        return classes;
    }

    transformToAceTokens(leafArray: agl.api.sppt.SPPTLeaf[]): AglLineToken[] {
        return leafArray.map((leaf: agl.api.sppt.SPPTLeaf) => {
            const tokenType = leaf.name; //(it.isPattern) ? '"' + it.name + '"' : "'" + it.name + "'";
            let cssClasses = this.mapToCssClasses(leaf);
            let beforeEOL = leaf.matchedText;
            const eolIndex = leaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                beforeEOL = leaf.matchedText.substr(0, eolIndex);
            }
            return new AglLineToken(
                cssClasses,
                beforeEOL,
                leaf.location.column,
                leaf.location.line
            );
        });
    }

    getLineTokensByScan(line: string, startState: AglLineToken, row: number): any {
        //console.info("tokens by scan for line: "+row);
        if (this.agl.processor) {
            const text = (startState) ? startState.value + line : line;
            const leafs = this.agl.processor.scan(text);
            const leafArray = leafs.toArray();
            const tokens = this.transformToAceTokens(leafArray);
            return {state: '', tokens};
        } else {
            return {
                state: '',
                tokens: [new AglLineToken(['nostyle'], line, 1, row)]
            }
        }
    }

    getLineTokensByParse(line: string, startState: AglLineToken, row: number): any {
        //console.info("tokens by parse for line: "+row);
        const leafs = this.agl.sppt.tokensByLine.toArray()[row];
        if (leafs) {
            const leafArray = leafs.toArray();
            const tokens = this.transformToAceTokens(leafArray);
            let state = null;
            //TODO: if last leaf span multiple lines, then next state (for getLineTokensByScan should contain the text)
            const lastLeaf = leafArray[leafArray.length - 1];
            if (lastLeaf) {
                if (!lastLeaf.eolPositions.isEmpty()) {
                    const eolIndex = lastLeaf.eolPositions.toArray()[0];
                    const afterEOL = lastLeaf.matchedText.substring(eolIndex + 1);
                    const cssClasses = this.mapToCssClasses(lastLeaf);
                    state = new AglLineToken(
                        cssClasses,
                        afterEOL,
                        1,
                        lastLeaf.location.line + 1
                    );
                }
            }
            return {state, tokens};
        } else {
            return {state: null, tokens: []}
        }
    }

    // --- ace.Ace.Tokenizer ---
    createSplitterRegexp(src: string, flag?: string): RegExp {
        return undefined;
    }

    getLineTokens(line: string, startState: AglLineToken, row: number): any {
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

    constructor(
        public languageId: string,
        private agl: AglComponents
    ) {
    }

    getCompletions(editor, session, pos, prefix, callback) {
        const posn = session.doc.positionToIndex(pos, 0);
        const wordList = this.getCompletionItems(editor, posn);
        callback(null, wordList.map(function (ci) {
            return {
                caption: ci.text,
                value: ci.text,
                meta: '(' +ci.rule.name +')'
            };
        }));
    }

    getCompletionItems(editor, pos) : agl.api.processor.CompletionItem[] {
        if (this.agl.goalRule) {
            const set = this.agl.processor.expectedAt(this.agl.goalRule, editor.getValue(), pos, 1);
            return set.toArray()
        } else {
            const set = this.agl.processor.expectedAt(editor.getValue(), pos, 1);
            return set.toArray()
        }
    }
}
