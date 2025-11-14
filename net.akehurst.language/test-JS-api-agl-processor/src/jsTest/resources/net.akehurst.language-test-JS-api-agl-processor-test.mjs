import {
    createThis2j2avj17cvnv2 as createThis,
    println2shhhgwwt4c61 as println,
    initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
    VOID3gxj6tk5isa35 as VOID,
    toString1pkumu07cwy4m as toString,
    IllegalStateExceptionkoljg5n0nrlr as IllegalStateException,
    Unit_getInstanced3nlqte25ayu as Unit_getInstance,
    THROW_CCE2g6jy02ryeudk as THROW_CCE,
    isInterface3d6p8outrmvmk as isInterface,
    KtList3hktaavzmj137 as KtList,
    toMap1vec9topfei08 as toMap,
    emptyMapr06gerzljqtm as emptyMap,
    Paire9pteg33gng7 as Pair,
    initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
    hashCodeq5arwsb9dgti as hashCode,
    getStringHashCode26igk1bx568vk as getStringHashCode,
    equals2au1ep9vhcato as equals,
    toString30pk9tzaqopn as toString_0,
    ArrayList3it5z8td81qkl as ArrayList,
    protoOf180f3jzyo7rfj as protoOf,
    ensureNotNull1e947j3ixpazm as ensureNotNull,
    initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
    listOf1jh22dvmctj1r as listOf,
    to2cs3ny02qtbcb as to,
    mapOf1xd03cq9cnmy8 as mapOf,
} from './kotlin-kotlin-stdlib.mjs';
import {
    SpptWalkern8pvvurx4wdl as SpptWalker,
    IssueHolder39atakzjd4kvn as IssueHolder,
    LanguageProcessorPhase_SEMANTIC_ANALYSIS_getInstance1p2ji795kcz9h as LanguageProcessorPhase_SEMANTIC_ANALYSIS_getInstance,
} from './net.akehurst.language-agl-parser.mjs';
import {
    Agl as Agl_getInstance,
    GrammarString as GrammarString,
    SyntaxAnalyserByMethodRegistrationAbstract as SyntaxAnalyserByMethodRegistrationAbstract,
    SentenceContext as SentenceContext,
    SemanticAnalysisResultDefault as SemanticAnalysisResultDefault,
    analyse as analyse,
    SemanticAnalyser as SemanticAnalyser,
} from './net.akehurst.language-agl-processor.mjs';
import {
    assertTrue1upg56ex908m as assertTrue,
    suitenlt29rr40l1o as suite,
    test3806p0uwinskq as test,
} from './kotlin-kotlin-test.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
class test_ParserFromString$parse$walker$1 {
    static new_test_test_ParserFromString__no_name_provided__rgj4z3_k$($box) {
        return createThis(this, $box);
    }
    beginTree() {
        println('start of SPPT');
    }
    endTree() {
        println('end of SPPT');
    }
    skip(startPosition, nextInputPosition) {
        println('a skip node: ' + startPosition + '-' + nextInputPosition);
    }
    leaf(nodeInfo) {
        println('leaf node: ' + nodeInfo.node.rule.tag + ' ' + nodeInfo.node.startPosition + '-' + nodeInfo.node.nextInputPosition);
    }
    beginBranch(nodeInfo) {
        println('start branch: ' + nodeInfo.node.rule.tag);
    }
    endBranch(nodeInfo) {
        println('end branch: ' + nodeInfo.node.rule.tag);
    }
    beginEmbedded(nodeInfo) {
        println('start embedded');
    }
    endEmbedded(nodeInfo) {
        println('end embedded');
    }
    treeError(msg, path) {
        println('error ' + msg);
    }
}
class test_ParserFromString {
    static new_test_test_ParserFromString_m7ooic_k$($box) {
        return createThis(this, $box);
    }
    parse_ti0j6l_k$() {
        var grammarStr = "namespace CalculatorModelLanguage\n\n\ngrammar CalculatorModelGrammar {\n\n// rules for \"Calculator\"\nCalculator = 'Calculator' identifier\n\t InputField*\n\t OutputField* ;\n\nInputField = 'input' identifier ;\n\nOutputField = 'output' CalcExpression ;\n\nInputFieldReference = __fre_reference ;\n\nNumberLiteralExpression = stringLiteral ;\n\nCalcExpression = InputFieldReference \n    | LiteralExpression \n    | __fre_binary_CalcExpression ;\n\nLiteralExpression = NumberLiteralExpression  ;\n\n__fre_binary_CalcExpression = [CalcExpression / __fre_binary_operator]2+ ;\nleaf __fre_binary_operator = '*' | '+' | '-' | '/' ;\n\n// common rules\n\n__fre_reference = [ identifier / '.' ]+ ;\n\n// white space and comments\nskip WHITE_SPACE = \"\\s+\" ;\nskip SINGLE_LINE_COMMENT = \"//[\\n\\r]*?\" ;\nskip MULTI_LINE_COMMENT = \"/\\*[^*]*\\*+(?:[^*`/`][^*]*\\*+)*`/`\" ;\n\n// the predefined basic types\nleaf identifier          = \"[a-zA-Z_][a-zA-Z0-9_]*\" ;\n/* see https://stackoverflow.com/questions/37032620/regex-for-matching-a-string-literal-in-java */\nleaf stringLiteral       = '\"' \"([^'\\\\\\\\]|\\\\'|\\\\\\\\)*\" '\"' ;\nleaf numberLiteral       = \"[0-9]+\";\nleaf booleanLiteral      = 'false' | 'true';\n\n}";
        var res = Agl_getInstance().processorFromStringSimple(GrammarString.new_net_akehurst_language_api_processor_GrammarString_q5vrur_k$(grammarStr));
        println(toString(res.issues));
        assertTrue(res.issues.errors.isEmpty_y1axqb_k$());
        var tmp0_elvis_lhs = res.processor;
        var tmp;
        if (tmp0_elvis_lhs == null) {
            var message = 'processor not found';
            throw IllegalStateException.new_kotlin_IllegalStateException_8zpm09_k$(toString(message));
        } else {
            tmp = tmp0_elvis_lhs;
        }
        var proc = tmp;
        var sentence = 'Calculator a\n  input i\n  output o';
        var pres = proc.parse(sentence);
        println(toString(pres.issues));
        assertTrue(pres.issues.isEmpty_y1axqb_k$(), toString(res.issues));
        var tmp1_elvis_lhs = pres.sppt;
        var tmp_0;
        if (tmp1_elvis_lhs == null) {
            var message_0 = 'sppt not found';
            throw IllegalStateException.new_kotlin_IllegalStateException_8zpm09_k$(toString(message_0));
        } else {
            tmp_0 = tmp1_elvis_lhs;
        }
        var sppt = tmp_0;
        var walker = test_ParserFromString$parse$walker$1.new_test_test_ParserFromString__no_name_provided__rgj4z3_k$();
        sppt.traverseTreeDepthFirst(walker, false);
    }
}
class Value {}
class LiteralValue {
    static new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(value, typeName, $box) {
        var $this = createThis(this, $box);
        $this.value_1 = value;
        $this.typeName_1 = typeName;
        return $this;
    }
    get_value_j01efc_k$() {
        return this.value_1;
    }
    get_typeName_s1eeum_k$() {
        return this.typeName_1;
    }
    component1_7eebsc_k$() {
        return this.value_1;
    }
    component2_7eebsb_k$() {
        return this.typeName_1;
    }
    copy_owxkdt_k$(value, typeName) {
        return LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(value, typeName);
    }
    copy$default_jxepl_k$(value, typeName, $super) {
        value = value === VOID ? this.value_1 : value;
        typeName = typeName === VOID ? this.typeName_1 : typeName;
        return $super === VOID ? this.copy_owxkdt_k$(value, typeName) : $super.copy_owxkdt_k$.call(this, value, typeName);
    }
    toString() {
        return 'LiteralValue(value=' + toString(this.value_1) + ', typeName=' + this.typeName_1 + ')';
    }
    hashCode() {
        var result = hashCode(this.value_1);
        result = imul(result, 31) + getStringHashCode(this.typeName_1) | 0;
        return result;
    }
    equals(other) {
        if (this === other)
            return true;
        if (!(other instanceof LiteralValue))
            return false;
        if (!equals(this.value_1, other.value_1))
            return false;
        if (!(this.typeName_1 === other.typeName_1))
            return false;
        return true;
    }
}
class ObjectValue {
    static new_test_test_Processor_Companion_ObjectValue_d5fxgb_k$(properties, $box) {
        var $this = createThis(this, $box);
        $this.properties_1 = properties;
        return $this;
    }
    get_properties_zhllqc_k$() {
        return this.properties_1;
    }
    component1_7eebsc_k$() {
        return this.properties_1;
    }
    copy_d9f1b8_k$(properties) {
        return ObjectValue.new_test_test_Processor_Companion_ObjectValue_d5fxgb_k$(properties);
    }
    copy$default_9601mh_k$(properties, $super) {
        properties = properties === VOID ? this.properties_1 : properties;
        return $super === VOID ? this.copy_d9f1b8_k$(properties) : $super.copy_d9f1b8_k$.call(this, properties);
    }
    toString() {
        return 'ObjectValue(properties=' + toString(this.properties_1) + ')';
    }
    hashCode() {
        return hashCode(this.properties_1);
    }
    equals(other) {
        if (this === other)
            return true;
        if (!(other instanceof ObjectValue))
            return false;
        if (!equals(this.properties_1, other.properties_1))
            return false;
        return true;
    }
}
class PredefinedValue {
    static new_test_test_Processor_Companion_PredefinedValue_zhvbse_k$(name, $box) {
        var $this = createThis(this, $box);
        $this.name_1 = name;
        $this.value_1 = null;
        return $this;
    }
    get_name_woqyms_k$() {
        return this.name_1;
    }
    set_value_rjqr2d_k$(_set____db54di) {
        this.value_1 = _set____db54di;
    }
    get_value_j01efc_k$() {
        return this.value_1;
    }
    toString() {
        return 'PredefinedValue(' + this.name_1 + '=' + toString_0(this.value_1) + ')';
    }
}
class MySyntaxAnalyser extends SyntaxAnalyserByMethodRegistrationAbstract {
    static new_test_test_Processor_Companion_MySyntaxAnalyser_e3pr2i_k$($box) {
        return this.new_net_akehurst_language_agl_syntaxAnalyser_SyntaxAnalyserByMethodRegistrationAbstract_2jq9zq_k$($box);
    }
    registerHandlers() {
        super.register(test_Processor$Companion$MySyntaxAnalyser$value$ref(this));
        super.register(test_Processor$Companion$MySyntaxAnalyser$predefined$ref(this));
        super.registerFor('object', test_Processor$Companion$MySyntaxAnalyser$object_$ref(this));
        super.register(test_Processor$Companion$MySyntaxAnalyser$property$ref(this));
        super.register(test_Processor$Companion$MySyntaxAnalyser$literal$ref(this));
    }
}
class MyContext {
    static new_test_test_Processor_Companion_MyContext_sjc478_k$(predefined, $box) {
        var $this = createThis(this, $box);
        $this.predefined_1 = predefined;
        return $this;
    }
    get_predefined_2tltgx_k$() {
        return this.predefined_1;
    }
}
class MySemanticAnalyser {
    static new_test_test_Processor_Companion_MySemanticAnalyser_ymbnxo_k$($box) {
        var $this = createThis(this, $box);
        $this.issues_1 = IssueHolder.new_net_akehurst_language_issues_ram_IssueHolder_w7jz83_k$(LanguageProcessorPhase_SEMANTIC_ANALYSIS_getInstance());
        var tmp = $this;
        // Inline function 'kotlin.collections.mutableListOf' call
        tmp._resolvedReferences_1 = ArrayList.new_kotlin_collections_ArrayList_h94ppk_k$();
        return $this;
    }
    get_issues_ewqnyb_k$() {
        return this.issues_1;
    }
    clear() {
    }
    analyse_vhf53k_k$(sentenceIdentity, asm, locationMap, options) {
        analyseValue(this, asm, options.context);
        return SemanticAnalysisResultDefault.new_net_akehurst_language_agl_processor_SemanticAnalysisResultDefault_ql6wkk_k$(this._resolvedReferences_1, this.issues_1);
    }
    analyse_la9vp2_k$(sentenceIdentity, asm, locationMap, options) {
        return this.analyse_vhf53k_k$(sentenceIdentity, isInterface(asm, Value) ? asm : THROW_CCE(), locationMap, options);
    }
}
class Companion {
    static new_test_test_Processor_Companion_sohqhx_k$($box) {
        var $this = createThis(this, $box);
        Companion_instance = $this;
        $this.grammarStr_1 = 'namespace test\ngrammar Test {\n    skip leaf WHITESPACE = "\\s+" ;\n    skip leaf MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;\n    skip leaf SINGLE_LINE_COMMENT = "//[\\n\\r]*?" ;\n\n    value = predefined | object | literal ;\n\n    predefined = IDENTIFIER ;\n    object = \'{\' property* \'}\' ;\n    property = IDENTIFIER \':\' value ;\n\n    literal = BOOLEAN | INTEGER | REAL | STRING ;\n    \n    leaf BOOLEAN = "true|false";\n    leaf REAL = "[0-9]+[.][0-9]+";\n    leaf STRING = "\'([^\'\\\\\\\\]|\\\\\'|\\\\\\\\)*\'";\n    leaf INTEGER = "[0-9]+";\n    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;\n}';
        var tmp = $this;
        var tmp_0 = Agl_getInstance();
        var tmp_1 = Agl_getInstance();
        // Inline function 'kotlin.let' call
        var it = tmp_0.processorFromString($this.grammarStr_1, tmp_1.configuration(VOID, test_Processor$Companion$processor$lambda));
        assertTrue(it.issues.errors.isEmpty_y1axqb_k$(), toString(it.issues));
        tmp.processor_1 = ensureNotNull(it.processor);
        return $this;
    }
    get_grammarStr_o2wb59_k$() {
        return this.grammarStr_1;
    }
    get_processor_9l9ogn_k$() {
        return this.processor_1;
    }
}
class test_Processor {
    static new_test_test_Processor_fu9lve_k$($box) {
        Companion_getInstance();
        return createThis(this, $box);
    }
    process_myqcf5_k$() {
        var sentences = listOf(['true', '1', '3.14', "'Hello World!'", 'var1', '{}', "{ a:false b:1 c:3.141 d:'bob' e:var2 }", '{ f:{x:1 y:{a:3 b:7}} }']);
        var context = MyContext.new_test_test_Processor_Companion_MyContext_sjc478_k$(mapOf([to('var1', LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(true, 'Boolean')), to('var2', LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(55, 'Integer'))]));
        var _iterator__ex2g4s = sentences.iterator_jk1svi_k$();
        while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
            var sentence = _iterator__ex2g4s.next_20eer_k$();
            println(sentence);
            var tmp = Companion_getInstance().processor_1;
            var tmp_0 = Agl_getInstance();
            var pres = tmp.process(sentence, tmp_0.options(VOID, test_Processor$process$lambda(context)));
            assertTrue(pres.allIssues.errors.isEmpty_y1axqb_k$(), toString(pres.allIssues));
            println('  ' + toString_0(pres.asm));
        }
    }
}
//endregion
function test_fun_izoufj() {
    suite('test_ParserFromString', false, test_fun$test_ParserFromString_test_fun$ref_ns1z10());
}
function test_fun$test_ParserFromString_test_fun$parse_test_fun$ref_nk8klo() {
    return () => {
        var tmp = test_ParserFromString.new_test_test_ParserFromString_m7ooic_k$();
        tmp.parse_ti0j6l_k$();
        return Unit_getInstance();
    };
}
function test_fun$test_ParserFromString_test_fun$ref_ns1z10() {
    return () => {
        test('parse', false, test_fun$test_ParserFromString_test_fun$parse_test_fun$ref_nk8klo());
        return Unit_getInstance();
    };
}
function value($this, nodeInfo, children, sentence) {
    var tmp = children.get_c1px32_k$(0);
    return (!(tmp == null) ? isInterface(tmp, Value) : false) ? tmp : THROW_CCE();
}
function predefined($this, nodeInfo, children, sentence) {
    var tmp = children.get_c1px32_k$(0);
    var name = (!(tmp == null) ? typeof tmp === 'string' : false) ? tmp : THROW_CCE();
    return PredefinedValue.new_test_test_Processor_Companion_PredefinedValue_zhvbse_k$(name);
}
function object_($this, nodeInfo, children, sentence) {
    var tmp = children.get_c1px32_k$(1);
    var props = (tmp == null ? true : isInterface(tmp, KtList)) ? tmp : THROW_CCE();
    var tmp1_elvis_lhs = props == null ? null : toMap(props);
    var properties = tmp1_elvis_lhs == null ? emptyMap() : tmp1_elvis_lhs;
    return ObjectValue.new_test_test_Processor_Companion_ObjectValue_d5fxgb_k$(properties);
}
function property($this, nodeInfo, children, sentence) {
    var tmp = children.get_c1px32_k$(0);
    var name = (!(tmp == null) ? typeof tmp === 'string' : false) ? tmp : THROW_CCE();
    var tmp_0 = children.get_c1px32_k$(2);
    var value = (!(tmp_0 == null) ? isInterface(tmp_0, Value) : false) ? tmp_0 : THROW_CCE();
    return Pair.new_kotlin_Pair_curykh_k$(name, value);
}
function literal($this, nodeInfo, children, sentence) {
    var tmp = children.get_c1px32_k$(0);
    var valueStr = (!(tmp == null) ? typeof tmp === 'string' : false) ? tmp : THROW_CCE();
    var tmp_0;
    switch (nodeInfo.alt.option.value) {
        case 0:
            tmp_0 = LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(valueStr, 'Boolean');
            break;
        case 1:
            tmp_0 = LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(valueStr, 'Integer');
            break;
        case 2:
            tmp_0 = LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(valueStr, 'Real');
            break;
        case 3:
            tmp_0 = LiteralValue.new_test_test_Processor_Companion_LiteralValue_4umiwk_k$(valueStr, 'String');
            break;
        default:
            var message = 'Internal error: alternative ' + nodeInfo.alt.option.toString() + " not handled for 'literal'";
            throw IllegalStateException.new_kotlin_IllegalStateException_8zpm09_k$(toString(message));
    }
    return tmp_0;
}
function test_Processor$Companion$MySyntaxAnalyser$value$ref(p0) {
    var l = (_this__u8e3s4, p0_0, p1) => {
        var tmp0 = p0;
        return value(tmp0, _this__u8e3s4, p0_0, p1);
    };
    l.callableName = 'value';
    return l;
}
function test_Processor$Companion$MySyntaxAnalyser$predefined$ref(p0) {
    var l = (_this__u8e3s4, p0_0, p1) => {
        var tmp0 = p0;
        return predefined(tmp0, _this__u8e3s4, p0_0, p1);
    };
    l.callableName = 'predefined';
    return l;
}
function test_Processor$Companion$MySyntaxAnalyser$object_$ref(p0) {
    var l = (_this__u8e3s4, p0_0, p1) => {
        var tmp0 = p0;
        return object_(tmp0, _this__u8e3s4, p0_0, p1);
    };
    l.callableName = 'object_';
    return l;
}
function test_Processor$Companion$MySyntaxAnalyser$property$ref(p0) {
    var l = (_this__u8e3s4, p0_0, p1) => {
        var tmp0 = p0;
        return property(tmp0, _this__u8e3s4, p0_0, p1);
    };
    l.callableName = 'property';
    return l;
}
function test_Processor$Companion$MySyntaxAnalyser$literal$ref(p0) {
    var l = (_this__u8e3s4, p0_0, p1) => {
        var tmp0 = p0;
        return literal(tmp0, _this__u8e3s4, p0_0, p1);
    };
    l.callableName = 'literal';
    return l;
}
function _get__resolvedReferences__wucd2q($this) {
    return $this._resolvedReferences_1;
}
function analyseValue($this, asm, context) {
    if (!(asm instanceof LiteralValue)) {
        if (asm instanceof ObjectValue) {
            // Inline function 'kotlin.collections.forEach' call
            var _iterator__ex2g4s = asm.properties_1.get_values_ksazhn_k$().iterator_jk1svi_k$();
            while (_iterator__ex2g4s.hasNext_bitz1p_k$()) {
                var element = _iterator__ex2g4s.next_20eer_k$();
                analyseValue($this, element, context);
            }
        } else {
            if (asm instanceof PredefinedValue) {
                var tmp = asm;
                var tmp2_safe_receiver = context == null ? null : context.predefined_1;
                tmp.value_1 = tmp2_safe_receiver == null ? null : tmp2_safe_receiver.get_wei43m_k$(asm.name_1);
                if (null == asm.value_1) {
                    $this.issues_1.error(null, "Predefined value '" + asm.name_1 + "' not found in context");
                }
            }
        }
    }
}
function test_Processor$Companion$processor$lambda($this$configuration) {
    $this$configuration.syntaxAnalyserResolverResult(test_Processor$Companion$processor$lambda$lambda);
    $this$configuration.semanticAnalyserResolverResult(test_Processor$Companion$processor$lambda$lambda_0);
    return Unit_getInstance();
}
function test_Processor$Companion$processor$lambda$lambda() {
    return MySyntaxAnalyser.new_test_test_Processor_Companion_MySyntaxAnalyser_e3pr2i_k$();
}
function test_Processor$Companion$processor$lambda$lambda_0() {
    return MySemanticAnalyser.new_test_test_Processor_Companion_MySemanticAnalyser_ymbnxo_k$();
}
var Companion_instance;
function Companion_getInstance() {
    if (Companion_instance === VOID)
        Companion.new_test_test_Processor_Companion_sohqhx_k$();
    return Companion_instance;
}
function test_Processor$process$lambda$lambda($context) {
    return ($this$semanticAnalysis) => {
        $this$semanticAnalysis.context($context);
        return Unit_getInstance();
    };
}
function test_Processor$process$lambda($context) {
    return ($this$options) => {
        $this$options.semanticAnalysis(test_Processor$process$lambda$lambda($context));
        return Unit_getInstance();
    };
}
function test_fun_izoufj_0() {
    suite('test_Processor', false, test_fun$test_Processor_test_fun$ref_5aprx4());
}
function test_fun$test_Processor_test_fun$process_test_fun$ref_wcn7pg() {
    return () => {
        var tmp = test_Processor.new_test_test_Processor_fu9lve_k$();
        tmp.process_myqcf5_k$();
        return Unit_getInstance();
    };
}
function test_fun$test_Processor_test_fun$ref_5aprx4() {
    return () => {
        test('process', false, test_fun$test_Processor_test_fun$process_test_fun$ref_wcn7pg());
        return Unit_getInstance();
    };
}
//region block: post-declaration
initMetadataForClass(test_ParserFromString$parse$walker$1, VOID, VOID, VOID, [SpptWalker]);
initMetadataForClass(test_ParserFromString, 'test_ParserFromString', test_ParserFromString.new_test_test_ParserFromString_m7ooic_k$);
initMetadataForInterface(Value, 'Value');
initMetadataForClass(LiteralValue, 'LiteralValue', VOID, VOID, [Value]);
initMetadataForClass(ObjectValue, 'ObjectValue', VOID, VOID, [Value]);
initMetadataForClass(PredefinedValue, 'PredefinedValue', VOID, VOID, [Value]);
initMetadataForClass(MySyntaxAnalyser, 'MySyntaxAnalyser', MySyntaxAnalyser.new_test_test_Processor_Companion_MySyntaxAnalyser_e3pr2i_k$);
initMetadataForClass(MyContext, 'MyContext', VOID, VOID, [SentenceContext]);
protoOf(MySemanticAnalyser).analyse = analyse;
initMetadataForClass(MySemanticAnalyser, 'MySemanticAnalyser', MySemanticAnalyser.new_test_test_Processor_Companion_MySemanticAnalyser_ymbnxo_k$, VOID, [SemanticAnalyser]);
initMetadataForCompanion(Companion);
initMetadataForClass(test_Processor, 'test_Processor', test_Processor.new_test_test_Processor_fu9lve_k$);
//endregion
//region block: tests
(function () {
    suite('test', false, function () {
        test_fun_izoufj();
        test_fun_izoufj_0();
    });
}());
//endregion

//# sourceMappingURL=net.akehurst.language-test-Kotlin-agl-processor-test.mjs.map
