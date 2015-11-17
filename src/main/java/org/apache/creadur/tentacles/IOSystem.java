/**
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

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipInputStream;

/**
 * @version $Rev$ $Date$
 */
public class IOSystem {
    private static final Logger LOG = Logger.getLogger(IOSystem.class);

    public String slurp(final File file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(file, out);
        return new String(out.toByteArray());
    }

    public String slurp(final URL url) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(url.openStream(), out);
        return new String(out.toByteArray());
    }

    public void writeString(final File file, final String string)
            throws IOException {
        final FileWriter out = new FileWriter(file);
        try {
            final BufferedWriter bufferedWriter = new BufferedWriter(out);
            try {
                bufferedWriter.write(string);
                bufferedWriter.newLine();
            } finally {
                close(bufferedWriter);
            }
        } finally {
            close(out);
        }
    }

    private void copy(final File from, final OutputStream to)
            throws IOException {
        final InputStream read = read(from);
        try {
            copy(read, to);
        } finally {
            close(read);
        }
    }

    public void copy(final InputStream from, final File to) throws IOException {
        final OutputStream write = write(to);
        try {
            copy(from, write);
        } finally {
            close(write);
        }
    }

    private void copy(final InputStream from, final OutputStream to)
            throws IOException {
        final byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = from.read(buffer)) != -1) {
            to.write(buffer, 0, length);
        }
        to.flush();
    }

    public void copy(final byte[] from, final File to) throws IOException {
        copy(new ByteArrayInputStream(from), to);
    }

    public ZipInputStream unzip(final File file) throws IOException {
        final InputStream read = read(file);
        return new ZipInputStream(read);
    }

    public void close(final Closeable closeable) throws IOException {
        if (closeable == null) {
            return;
        }
        try {
            if (closeable instanceof Flushable) {
                ((Flushable) closeable).flush();
            }
        } catch (final IOException e) {
        	LOG.trace("Error when trying to flush before closing " + closeable, e);
        }
        try {
            closeable.close();
        } catch (final IOException e) {
        	LOG.trace("Error when trying to close " + closeable, e);
        }
    }

    public OutputStream write(final File destination)
            throws FileNotFoundException {
        final OutputStream out = new FileOutputStream(destination);
        return new BufferedOutputStream(out, 32768);
    }

    public InputStream read(final File source) throws FileNotFoundException {
        final InputStream in = new FileInputStream(source);
        return new BufferedInputStream(in, 32768);
    }

    public byte[] read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        out.close();
        return out.toByteArray();
    }
}
