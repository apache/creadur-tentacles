/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.creadur.tentacles;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class TentaclesResources {

    private final IOSystem ioSystem;

    public TentaclesResources(final IOSystem ioSystem) {
        super();
        this.ioSystem = ioSystem;
    }

    public Reader read(final String resourceName) throws IOException {
        final URL resourceUrl = toUrl(resourceName);
        final InputStreamReader templateReader =
                new InputStreamReader(resourceUrl.openStream());
        return templateReader;
    }

    public String readText(final String resourcePath) throws IOException {
        final String text = this.ioSystem.slurp(toUrl(resourcePath));
        return text;
    }

    public void copyTo(final String resourcePath, final File to)
            throws IOException {
        this.ioSystem.copy(toUrl(resourcePath).openStream(), to);
    }

    private URL toUrl(final String resourcePath) {
        final URL resourceUrl =
                this.getClass().getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalStateException(
                    "Tentacles expects the classpath to contain "
                            + resourcePath);
        }
        return resourceUrl;
    }
}
