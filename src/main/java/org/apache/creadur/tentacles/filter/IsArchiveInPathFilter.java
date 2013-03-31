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
package org.apache.creadur.tentacles.filter;

import java.io.File;
import java.io.FileFilter;

final class IsArchiveInPathFilter implements FileFilter {

    private final String pathNameFilter;

    IsArchiveInPathFilter(final String pathNameFilter) {
        super();
        this.pathNameFilter = pathNameFilter;
    }

    @Override
    public boolean accept(final File pathname) {
        final String path = pathname.getAbsolutePath();
        return path.matches(this.pathNameFilter) && isValidArchive(path);
    }

    private boolean isValidArchive(final String path) {
        return path.matches(".*\\.(jar|zip|war|ear|tar.gz)");
    }
}