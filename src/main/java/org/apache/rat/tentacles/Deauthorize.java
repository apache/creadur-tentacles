/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rat.tentacles;

import org.codehaus.swizzle.stream.DelimitedTokenReplacementInputStream;
import org.codehaus.swizzle.stream.ExcludeFilterInputStream;
import org.codehaus.swizzle.stream.StringTokenHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Little utility that will yank the author comments from java files.
 *
 * If the resulting comment block is effectively empty, it will be yanked too.
 */
public class Deauthorize {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) throw new IllegalArgumentException("At least one directory must be specified");

        final List<File> dirs = new ArrayList<File>();

        // Check the input args upfront
        for (String arg : args) {
            final File dir = new File(arg);

            if (not(dir.exists(), "Does not exist: %s", arg)) continue;
            if (not(dir.isDirectory(), "Not a directory: %s", arg)) continue;

            dirs.add(dir);
        }

        // Exit if we got bad input
        if (dirs.size() != args.length) System.exit(1);

        // Go!
        for (File dir : dirs) {
            deauthorize(dir);
        }
    }

    private static void deauthorize(File dir) throws IOException {
        for (File file : Files.collect(dir, ".*\\.java")) {

            if (not(file.canRead(), "File not readable: %s", file.getAbsolutePath())) continue;

            final String text = IO.slurp(file);

            // You really can't trust text to be in the native line ending
            final String eol = (text.contains("\r\n")) ? "\r\n" : "\n";

            InputStream in = new ByteArrayInputStream(text.getBytes());

            // Yank author tags
            in = new ExcludeFilterInputStream(in, " * @author", eol);

            // Clean "empty" comments
            final String begin = eol + "/*";
            final String end = "*/" + eol;
            in = new DelimitedTokenReplacementInputStream(in, begin, end, new StringTokenHandler() {
                @Override
                public String handleToken(String s) throws IOException {

                    if (s.replaceAll("[\\s*]", "").length() == 0) return eol;

                    return begin + s + end;
                }
            });

            byte[] content = IO.read(in);

            if (content.length != file.length()) {

                if (not(file.canWrite(), "File not writable: %s", file.getAbsolutePath())) continue;

                IO.copy(content, file);
            }
        }
    }

    private static boolean not(boolean b, String message, Object... details) {
        b=!b;
        if (b) {
            System.err.printf(message, details);
            System.err.println();
        }
        return b;
    }
}
