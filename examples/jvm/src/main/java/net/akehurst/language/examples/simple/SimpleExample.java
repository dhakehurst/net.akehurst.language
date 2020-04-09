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

import net.akehurst.language.agl.processor.Agl;
import net.akehurst.language.api.processor.LanguageProcessor;

import java.io.InputStream;
import java.util.Scanner;

public class SimpleExample {
    public static SimpleExample INSTANCE = new SimpleExample();

    public String grammarStr;
    public LanguageProcessor processor;

    private SimpleExample() {
        InputStream inputStream = SimpleExample.class.getResourceAsStream("/SimpleExample.agl");
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        this.grammarStr = s.hasNext() ? s.next() : "";
        this.processor = Agl.INSTANCE.processor(this.grammarStr, null,null);
    }
}
