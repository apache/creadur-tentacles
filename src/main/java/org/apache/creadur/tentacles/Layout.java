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

public class Layout {

    private final File localRootDirectory;
    private final File output;
    private final File repository;
    private final File contentRootDirectory;

    public Layout(final Platform platform, final Configuration configuration) {
        super();
        this.localRootDirectory =
                new File(configuration.getRootDirectoryForLocalOutput());

        final FileSystem fileSystem = platform.getFileSystem();
        fileSystem.mkdirs(this.localRootDirectory);

        this.repository = new File(this.localRootDirectory, "repo");
        this.contentRootDirectory =
                new File(this.localRootDirectory, "content");
        this.output = this.localRootDirectory;

        fileSystem.mkdirs(this.repository);
        fileSystem.mkdirs(this.contentRootDirectory);
    }

    public File getLocalRootDirectory() {
        return this.localRootDirectory;
    }

    public File getOutputDirectory() {
        return this.output;
    }

    public File getRepositoryDirectory() {
        return this.repository;
    }

    public File getContentRootDirectory() {
        return this.contentRootDirectory;
    }

}
