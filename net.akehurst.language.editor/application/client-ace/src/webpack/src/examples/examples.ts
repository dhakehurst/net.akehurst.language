export class Example {

    constructor(
        public id: string,
        public label: string,
        public sentence: string,
        public grammar: string,
        public style: string,
        public format: string
    ) {
    }
}

export class Examples {

    static initialise(selectEl) {
        Examples.map.forEach((eg,id) => {
            let option = document.createElement('option');
            selectEl.appendChild(option);
            option.setAttribute('value', eg.id);
            option.textContent = eg.label;
        });
    }

    static map = new Map<string, Example>();

    static add( id: string,
                label: string,
                sentence: string,
                grammar: string,
                style: string,
                format: string) {
        const eg = new Example(id,label, sentence, grammar, style, format);
        Examples.map.set(id, eg);
    }

}