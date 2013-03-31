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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Archive {

    private final Layout layout;
    private final FileSystem fileSystem;
    private final URI uri;
    private final File file;
    private final Map<URI, URI> map;

    private final Set<License> licenses = new HashSet<License>();
    private final Set<Notice> notices = new HashSet<Notice>();

    private final Set<License> declaredLicenses = new HashSet<License>();
    private final Set<Notice> declaredNotices = new HashSet<Notice>();

    private final Set<License> otherLicenses = new HashSet<License>();
    private final Set<Notice> otherNotices = new HashSet<Notice>();
    private Map<URI, URI> others;

    public Archive(final File file, final FileSystem fileSystem,
            final Layout layout) {
        this.fileSystem = fileSystem;
        this.layout = layout;
        this.uri =
                layout.getRepositoryDirectory().toURI()
                        .relativize(file.toURI());
        this.file = file;
        this.map = map();
    }

    public Set<License> getDeclaredLicenses() {
        return this.declaredLicenses;
    }

    public Set<Notice> getDeclaredNotices() {
        return this.declaredNotices;
    }

    public Set<License> getOtherLicenses() {
        return this.otherLicenses;
    }

    public Set<Notice> getOtherNotices() {
        return this.otherNotices;
    }

    public Set<License> getLicenses() {
        return this.licenses;
    }

    public Set<Notice> getNotices() {
        return this.notices;
    }

    public URI getUri() {
        return this.uri;
    }

    public File getFile() {
        return this.file;
    }

    public Map<URI, URI> getLegal() {
        return this.map;
    }

    public Map<URI, URI> getOtherLegal() {
        if (this.others == null) {
            this.others = mapOther();
        }
        return this.others;
    }

    private Map<URI, URI> mapOther() {
        final File jarContents = contentsDirectory();
        final List<File> legal =
                this.fileSystem.legalDocumentsUndeclaredIn(jarContents);

        return buildMapFrom(jarContents, legal);
    }

    private Map<URI, URI> buildMapFrom(final File jarContents,
            final List<File> legal) {
        final Map<URI, URI> map = new LinkedHashMap<URI, URI>();
        for (final File file : legal) {
            final URI name = jarContents.toURI().relativize(file.toURI());
            final URI link =
                    this.layout.getLocalRootDirectory().toURI()
                            .relativize(file.toURI());

            map.put(name, link);
        }
        return map;
    }

    private Map<URI, URI> map() {
        final File jarContents = contentsDirectory();
        final List<File> legal =
                this.fileSystem.legalDocumentsDeclaredIn(jarContents);

        return buildMapFrom(jarContents, legal);
    }

    public File contentsDirectory() {
        final File archiveDocument = getFile();
        String path =
                archiveDocument.getAbsolutePath().substring(
                        this.layout.getLocalRootDirectory().getAbsolutePath()
                                .length() + 1);

        if (path.startsWith("repo/")) {
            path = path.substring("repo/".length());
        }
        if (path.startsWith("content/")) {
            path = path.substring("content/".length());
        }

        final File contents =
                new File(this.layout.getContentRootDirectory(), path
                        + ".contents");
        this.fileSystem.mkdirs(contents);
        return contents;
    }

    public URI contentsURI() {
        return contentsDirectory().toURI();
    }
}