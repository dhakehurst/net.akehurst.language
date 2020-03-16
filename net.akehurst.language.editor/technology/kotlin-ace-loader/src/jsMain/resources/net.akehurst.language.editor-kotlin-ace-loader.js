/**
 * Based on [https://github.com/daemontus/kotlin-ace-wrapper]
 *
 * The loader receives an Ace id and a class name, loads the corresponding module and unwraps the class.
 * Optionally provide a 'root' module name that defaults to 'ace-builds'
 * If no name is provided, it simply assumes the module is the class.
 */
module.exports = function() {
    var loaderUtils = require('loader-utils');
    var options = loaderUtils.parseQuery(this.resourceQuery);
    var id = options["id"];
    var name = options["name"];
    if (!id) {
        throw new Error("Missing Ace module id.");
    }
    var code;
    if (this.target === 'webworker') {
        // Web worker does not have the full Ace as a dependency. The necessary classes
        // are loaded from global context.
        code = "require('script-loader!kotlin-ace-worker/class-loader.js'); module.exports = ace.require('"+id+"')";
    } else if (this.target === 'web') {
        // On web, load extensions, load Ace, then load class.
        code = "var ace = require('ace-builds/src-noconflict/ace'); require('ace-builds/src-noconflict/ext-language_tools'); module.exports = ace.require('"+id+"')";
    } else {
        throw new Error("Ace cannot be executed on target "+target);
    }
    if (name) code = code + "." + name;
    return code;
};