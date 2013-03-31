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

public class Filters {

    private final FilesOnlyFilter filesOnly;
    private final LicenseFilter licensesOnly;
    private final NoticeFilter noticesOnly;
    private final LegalFilter legalOnly;

    public Filters() {
        this.filesOnly = new FilesOnlyFilter();
        this.licensesOnly = new LicenseFilter();
        this.noticesOnly = new NoticeFilter();
        this.legalOnly = new LegalFilter();
    }

    public FileFilter filesOnly() {
        return this.filesOnly;
    }

    public FileFilter licensesOnly() {
        return this.licensesOnly;
    }

    public FileFilter noticesOnly() {
        return this.noticesOnly;
    }

    public FileFilter legalOnly() {
        return this.legalOnly;
    }

    public FileFilter licensesIn(final File contents) {
        return new AndFilter(new DeclaredFilter(contents), new LicenseFilter());

    }

    public FileFilter noticesIn(final File contents) {
        return new AndFilter(new DeclaredFilter(contents), new NoticeFilter());
    }

    public FileFilter undeclaredLegalDocumentsIn(final File contents) {
        return new AndFilter(new NotFilter(new DeclaredFilter(contents)),
                new LegalFilter());
    }

    public FileFilter declaredLegalDocumentsIn(final File contents) {
        return new AndFilter(new DeclaredFilter(contents), new LegalFilter());
    }

    public FileFilter archiveInPath(final String repositoryPathNameFilter) {
        return new IsArchiveInPathFilter(repositoryPathNameFilter);
    }
}
