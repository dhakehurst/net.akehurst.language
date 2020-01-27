import {Ace} from "ace-builds"

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

class AglEditorAce {

    static initialise(): Map<string, AglEditorAce> {
        let map = new Map<string, AglEditorAce>();
        document.querySelectorAll('agl-monaco').forEach((element: HTMLElement) => {
            let id = element.getAttribute('id');
            let initContent = trimIndent(element.textContent);
            element.textContent = '';
            let editor = new AglEditorAce(element, id, initContent, {automaticLayout: true});
            map.set(id, editor);
        });
        return map;
    }

}

class AglMode  implements Ace.SyntaxMode {
    autoOutdent(state: any, doc: Ace.Document, row: number): void {
    }

    checkOutdent(state: any, line: string, input: string): boolean {
        return false;
    }

    createModeDelegates(mapping: { [p: string]: string }): void {
    }

    createWorker(session: Ace.EditSession): any {
    }

    getCompletions(state: string, session: Ace.EditSession, pos: Ace.Point, prefix: string): Ace.Completion[] {
        return [];
    }

    getKeywords(append?: boolean): Array<string | RegExp> {
        return undefined;
    }

    getNextLineIndent(state: any, line: string, tab: string): string {
        return line.match(/^\s*/)[0];
    }

    getTokenizer(): Ace.Tokenizer {
        return undefined;
    }

    toggleBlockComment(state: any, session: Ace.EditSession, range: Ace.Range, cursor: Ace.Point): void {
    }

    toggleCommentLines(state: any, session: Ace.EditSession, startRow: number, endRow: number): void {
    }

    transformAction(state: string, action: string, editor: Ace.Editor, session: Ace.EditSession, text: string): any {
    }

}

class AglTokenizer implements Ace.Tokenizer {
    createSplitterRegexp(src: string, flag?: string): RegExp {
        return undefined;
    }

    getLineTokens(line: string, startState: string | string[]): Ace.Token[] {
        return [];
    }

    removeCapturingGroups(src: string): string {
        return "";
    }

}