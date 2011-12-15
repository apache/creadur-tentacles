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

    public static List<File> collect(final File dir, final Pattern pattern) {
        return collect(dir, new FileFilter() {
            @Override
            public boolean accept(File file) {
                return pattern.matcher(file.getAbsolutePath()).matches();
            }
        });
    }


    public static List<File> collect(File dir, FileFilter filter) {
        final List<File> accepted = new ArrayList<File>();
        if (filter.accept(dir)) accepted.add(dir);

        final File[] files = dir.listFiles();
        if (files != null) for (File file : files) {
            accepted.addAll(collect(file, filter));
        }

        return accepted;
    }

    public static void exists(File file, String s) {
        if (!file.exists()) throw new RuntimeException(s + " does not exist: " + file.getAbsolutePath());
    }

    public static void dir(File file) {
        if (!file.isDirectory()) throw new RuntimeException("Not a directory: " + file.getAbsolutePath());
    }

    public static void file(File file) {
        if (!file.isFile()) throw new RuntimeException("Not a file: " + file.getAbsolutePath());
    }

    public static void writable(File file) {
        if (!file.canWrite()) throw new RuntimeException("Not writable: " + file.getAbsolutePath());
    }

    public static void readable(File file) {
        if (!file.canRead()) throw new RuntimeException("Not readable: " + file.getAbsolutePath());
    }

    public static void mkdir(File file) {
        if (file.exists()) return;
        if (!file.mkdirs()) throw new RuntimeException("Cannot mkdir: " + file.getAbsolutePath());
    }
}
