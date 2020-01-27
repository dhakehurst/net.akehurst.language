export interface AglEditor {

    getText(): string;

    setText(value: string): void;

    onChange(handler: (text: string) => void): void;

    updateParseTree(tree: SharedPackedParseTree): void;

    layout(): void;
}


export interface SharedPackedParseTree {

}