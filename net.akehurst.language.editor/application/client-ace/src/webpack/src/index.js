import {TabView} from "./TabView.js"
import ace from "ace-builds"
import EditSession from "ace-builds"
import * as agl_js from 'net.akehurst.language-agl-processor';
const agl = agl_js.net.akehurst.language;

TabView.initialise();

let Agl = agl.processor.Agl;

let expressionText = ace.edit("expression-text");
let languageGrammar = ace.edit("language-grammar");
let languageStyle = ace.edit("language-style");
let languageFormat = ace.edit("language-format");

let expressionProcessor = Agl.processorFromString(languageGrammar.getValue());

languageGrammar.on("blur", e =>{
    console.info("grammar changed");
    try {
        expressionProcessor = Agl.processorFromString(languageGrammar.getValue());
    } catch (e) {
        console.error(e);
    }
});

languageStyle.on("blur", e =>{
   console.info("style changed");
});

function updateHighlighting() {

}

languageGrammar.setSession()