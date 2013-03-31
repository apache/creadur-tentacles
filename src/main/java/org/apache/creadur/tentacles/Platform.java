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

public class Platform {

    public static Platform aPlatform() {
        final FileSystem fileSystem = new FileSystem();
        final IOSystem ioSystem = new IOSystem();
        final TentaclesResources tentaclesResources =
                new TentaclesResources(ioSystem);
        return new Platform(tentaclesResources, fileSystem, ioSystem);
    }

    private final TentaclesResources tentaclesResources;
    private final FileSystem fileSystem;
    private final IOSystem ioSystem;

    public Platform(final TentaclesResources tentaclesResources,
            final FileSystem fileSystem, final IOSystem ioSystem) {
        super();
        this.tentaclesResources = tentaclesResources;
        this.fileSystem = fileSystem;
        this.ioSystem = ioSystem;
    }

    public TentaclesResources getTentaclesResources() {
        return this.tentaclesResources;
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public IOSystem getIoSystem() {
        return this.ioSystem;
    }

}
