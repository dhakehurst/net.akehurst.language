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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

public class Java8TestFiles {

    static String javaTestFiles = "../javaTestFiles/javac";

    public static Collection<Object[]> getFiles() {
        final ArrayList<Object[]> params = new ArrayList<>();
        try {
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

            Files.walkFileTree(Paths.get(javaTestFiles), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() && matcher.matches(file)) {
                        params.add(new Object[] { file });
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new RuntimeException("Error getting files",e);
        }
        return params;
    }

}
