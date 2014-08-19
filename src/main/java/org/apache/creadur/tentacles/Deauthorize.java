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
package org.apache.creadur.tentacles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.swizzle.stream.DelimitedTokenReplacementInputStream;
import org.codehaus.swizzle.stream.ExcludeFilterInputStream;
import org.codehaus.swizzle.stream.StringTokenHandler;

/**
 * Little utility that will yank the author comments from java files.
 * 
 * If the resulting comment block is effectively empty, it will be yanked too.
 */
public class Deauthorize {

    /**
     * All input must be valid directories.
     * 
     * Invalid input is logged to System.err and skipped
     * 
     * @param args
     *            a list of directories to scan and fix
     * @throws Exception in case of errors.
     */
    public static void main(final String[] args) throws Exception {

        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "At least one directory must be specified");
        }

        final List<File> dirs = new ArrayList<File>();

        // Check the input args upfront
        for (final String arg : args) {
            final File dir = new File(arg);

            if (not(dir.exists(), "Does not exist: %s", arg)) {
                continue;
            }
            if (not(dir.isDirectory(), "Not a directory: %s", arg)) {
                continue;
            }

            dirs.add(dir);
        }

        // Exit if we got bad input
        if (dirs.size() != args.length) {
            System.exit(1);
        }

        // Go!
        for (final File dir : dirs) {
            deauthorize(dir);
        }
    }

    /**
     * Iterate over all the java files in the given directory
     * 
     * Read in the file so we can guess the line ending -- if we didn't need to
     * do that we could just stream. Run the content through Swizzle Stream and
     * filter out any author tags as well as any comment blocks that wind up (or
     * already were) empty as a result.
     * 
     * If that had any effect on the contents of the file, write it back out.
     * 
     * Should skip any files that are not readable or writable.
     * 
     * Will log an error on System.err for any files that were updated and were
     * not writable. Files that are not writable and don't need updating are
     * simply ignored.
     * 
     * @param dir
     * @throws IOException
     */
    private static void deauthorize(final File dir) throws IOException {
        deauthorize(dir, new IOSystem());
    }

    private static void deauthorize(final File dir, final IOSystem io)
            throws IOException {
        for (final File file : new FileSystem().collect(dir, ".*\\.java")) {

            if (not(file.canRead(), "File not readable: %s",
                    file.getAbsolutePath())) {
                continue;
            }

            final String text = io.slurp(file);

            // You really can't trust text to be in the native line ending
            final String eol = (text.contains("\r\n")) ? "\r\n" : "\n";
            final String startComment = eol + "/*";
            final String endComment = "*/" + eol;

            InputStream in = new ByteArrayInputStream(text.getBytes());

            // Yank author tags
            in = new ExcludeFilterInputStream(in, " * @author", eol);

            // Clean "empty" comments
            in =
                    new DelimitedTokenReplacementInputStream(in, startComment,
                            endComment, new StringTokenHandler() {
                                @Override
                                public String handleToken(
                                        final String commentBlock)
                                        throws IOException {

                                    // Yank if empty
                                    if (commentBlock.replaceAll("[\\s*]", "")
                                            .length() == 0) {
                                        return eol;
                                    }

                                    // Keep otherwise
                                    return startComment + commentBlock
                                            + endComment;
                                }
                            });

            final byte[] content = io.read(in);

            if (content.length != file.length()) {

                if (not(file.canWrite(), "File not writable: %s",
                        file.getAbsolutePath())) {
                    continue;
                }

                io.copy(content, file);
            }
        }
    }

    private static boolean not(boolean b, final String message,
            final Object... details) {
        b = !b;
        if (b) {
            System.err.printf(message, details);
            System.err.println();
        }
        return b;
    }
}
