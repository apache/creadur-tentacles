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
import java.net.URI;
import java.net.URISyntaxException;

public class Configuration {

    private static final String DEFAULT_FILE_REPOSITORY_PATH_NAME_FILTER =
            "org/apache/openejb";
    private static final String SYSTEM_PROPERTY_NAME_FOR_FILE_REPOSITORY_PATH_NAME_FILTER =
            "filter";
    private static final int ARGUMENT_INDEX_FOR_LOCAL_ROOT_DIRECTORY = 1;
    private static final int ARGUMENT_INDEX_FOR_URI_CONFIGURATION = 0;
    private static final int ARGUMENT_LENGTH_FOR_URI_CONFIGURATION_ONLY =
            ARGUMENT_INDEX_FOR_URI_CONFIGURATION + 1;

    private static URI toURI(final String arg) throws URISyntaxException {
        final URI uri = new URI(arg);
        if (arg.startsWith("file:")) {
            return new File(uri).getAbsoluteFile().toURI();
        }
        return uri;
    }

    private final URI staging;
    private final String rootDirectoryForLocalOutput;
    private final String fileRepositoryPathNameFilter;

    public Configuration(final String... args) throws URISyntaxException {
        this.staging = toURI(args[ARGUMENT_INDEX_FOR_URI_CONFIGURATION]);
        this.rootDirectoryForLocalOutput = rootDirectoryForLocalOutput(args);
        this.fileRepositoryPathNameFilter =
                System.getProperty(
                        SYSTEM_PROPERTY_NAME_FOR_FILE_REPOSITORY_PATH_NAME_FILTER,
                        DEFAULT_FILE_REPOSITORY_PATH_NAME_FILTER);
    }

    public String getFileRepositoryPathNameFilter() {
        return this.fileRepositoryPathNameFilter;
    }

    public URI getStaging() {
        return this.staging;
    }

    public String getRootDirectoryForLocalOutput() {
        return this.rootDirectoryForLocalOutput;
    }

    private String rootDirectoryForLocalOutput(final String... args) {
        final String rootDirectoryForLocal;
        if (args.length > ARGUMENT_LENGTH_FOR_URI_CONFIGURATION_ONLY) {
            rootDirectoryForLocal =
                    args[ARGUMENT_INDEX_FOR_LOCAL_ROOT_DIRECTORY];
        } else {
            rootDirectoryForLocal = new File(this.staging.getPath()).getName();
        }
        return rootDirectoryForLocal;
    }
}
