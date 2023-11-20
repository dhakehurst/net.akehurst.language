/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package test;

import kotlin.Unit;
import net.akehurst.language.agl.processor.Agl;
import net.akehurst.language.agl.semanticAnalyser.ContextSimple;
import net.akehurst.language.api.asm.Asm;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.processor.LanguageProcessorConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class test_Agl {

    private static final String EOL = System.lineSeparator();
    private static final String grammarStr = ""
            + "namespace test" + EOL
            + "grammar Test {" + EOL
            + "  skip WS=\"\\s+\";" + EOL
            + "  S='hello' 'world' '!';" + EOL
            + "}";


    @Test
    public void getBuildStamp() {
        String actual = Agl.INSTANCE.getBuildStamp().substring(0, 4);

        String expected = "2023";
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void processorFromString_noConfig() {
        LanguageProcessor<Object, Object> proc = Agl.INSTANCE.processorFromString(grammarStr, null, null).getProcessor();

        Assert.assertNotNull(proc);
    }

    @Test
    public void processorFromString_withConfigDefault() {
        LanguageProcessorConfiguration<Asm, ContextSimple> config = Agl.INSTANCE.configurationDefault();

        LanguageProcessor<Asm, ContextSimple> proc = Agl.INSTANCE.processorFromString(grammarStr, config, null).getProcessor();

        Assert.assertNotNull(proc);
    }

    @Test
    public void processorFromString_withConfigSet() {
        LanguageProcessorConfiguration<Object, Object> config = Agl.INSTANCE.configuration(null, b -> {
            b.targetGrammarName("Test");
            return Unit.INSTANCE;
        });

        LanguageProcessor<Object, Object> proc = Agl.INSTANCE.processorFromString(grammarStr, config, null).getProcessor();

        Assert.assertNotNull(proc);
    }

}