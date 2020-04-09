/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.examples.simple;

import net.akehurst.language.api.analyser.AsmElementSimple;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class test_SimpleExample {
    static final String EOL = "\n";
    static final String text1 = "" +
            "class class {" + EOL +
            "  property : String" + EOL +
            "  method(p1: Integer, p2: String) {" + EOL +
            "  }" + EOL +
            "}";

    @Test
    public void scan() {
        List<SPPTLeaf> result = SimpleExample.INSTANCE.processor.scan(text1);
        System.out.println(result);

        assertEquals(31, result.size());

    }

    @Test
    public void parse() {
        SharedPackedParseTree result = SimpleExample.INSTANCE.processor.parse("unit", text1);

        System.out.println("--- original text, from the parse tree ---");
        System.out.println(result.getAsString());

        System.out.println("--- the parse tree ---");
        System.out.println(result.toStringIndented("  "));

    }

    @Test
    public void process() {
        List<AsmElementSimple> result = SimpleExample.INSTANCE.processor.process("unit", text1);
        System.out.println(result);
    }

}