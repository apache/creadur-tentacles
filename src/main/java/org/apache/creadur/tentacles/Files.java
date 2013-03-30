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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @version $Rev$ $Date$
 */
public class Files {

    public static List<File> collect(final File dir, final String regex) {
        return collect(dir, Pattern.compile(regex));
    }

    private static List<File> collect(final File dir, final Pattern pattern) {
        return collect(dir, new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return pattern.matcher(file.getAbsolutePath()).matches();
            }
        });
    }

    public static List<File> collect(final File dir, final FileFilter filter) {
        final List<File> accepted = new ArrayList<File>();
        if (filter.accept(dir)) {
            accepted.add(dir);
        }

        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                accepted.addAll(collect(file, filter));
            }
        }

        return accepted;
    }

    public static void mkparent(final File file) {
        mkdirs(file.getParentFile());
    }

    public static void mkdirs(final File file) {

        if (!file.exists()) {

            final boolean success = file.mkdirs();
            assert success : "mkdirs failed to create " + file;

            return;
        }

        final boolean isDirectory = file.isDirectory();
        assert isDirectory : "Not a directory: " + file;
    }
}
