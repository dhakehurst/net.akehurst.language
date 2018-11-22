/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.common;

import java.time.Duration;
import java.time.Instant;

public class TimeLogger implements AutoCloseable {

    private final String col;
    private final String item;
    private final Instant start;
    private boolean success;

    public TimeLogger(String col, String item) {
        this.col = col;
        this.item = item;
        this.start = Instant.now();
        this.success = false;
    }

    public void success() {
        this.success = true;
    }

    public void close() {
        Instant end = Instant.now();
        Duration d = Duration.between(this.start, end);
        Results.log(this.success, this.col, this.item, d);
    }

}
