function tabSelect(element, tabId) {
    let tabview = element.parentElement.parentElement;
    tabview.querySelectorAll(':scope > tab').forEach(e=>e.style.display='none');
    tabview.querySelector(':scope > tab[id="'+tabId+'"]').style.display='grid';
}

document.getElementById('language.grammar').textContent =
`namespace test

grammar Test {
  declaration = 'class' ID '{' '}' ;

  ID = "[A-Za-z_][A-Za-z0-9_]*" ;

}
`;

document.getElementById('language.style').textContent =
`'class' {
    -fx-font-size: 14pt;
    -fx-font-family: "Courier New";
    -fx-fill: purple;
    -fx-font-weight: bold;
}
"[A-Za-z_][A-Za-z0-9_]*" {
    -fx-font-size: 14pt;
    -fx-font-family: "Courier New";
    -fx-fill: red;
    -fx-font-style: italic;
}
'{' {
    -fx-fill: darkgreen;
}
'}' {
    -fx-fill: darkgreen;
}
`;

document.getElementById('expression.text').textContent =
`class XXX {

}
`;

let Agl = window['net.akehurst.language-agl-processor'].net.akehurst.language.processor.Agl;

let expressionText = ace.edit("expression.text");
let languageGrammar = ace.edit("language.grammar");
let languageStyle = ace.edit("language.style");
let languageFormat = ace.edit("language.format");

let expressionProcessor = Agl.processorFromString(expressionText.text);

languageGrammar.on("blur", e =>{
    console.info("grammar changed");
    try {
        expressionProcessor = Agl.processorFromString(expressionText.text);
    } catch (e) {
        console.error(e);
    }
});

languageStyle.on("blur", e =>{
   console.info("style changed");
});

function updateHighlighting() {

}