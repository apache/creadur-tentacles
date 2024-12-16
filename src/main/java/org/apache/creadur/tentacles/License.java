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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class License {
    private final String text;
    private final String key;
    private final Set<Archive> archives = new HashSet<>();
    private final List<File> locations = new ArrayList<>();

    public License(final String key, final String text) {
        this.text = text;
        this.key = key;
    }

    public String getText() {
        return this.text;
    }

    public String getKey() {
        return this.key;
    }

    public Set<Archive> getArchives() {
        return this.archives;
    }

    public List<File> getLocations() {
        return this.locations;
    }

    public Set<URI> locations(final Archive archive) {
        final URI contents = archive.contentsURI();
        final Set<URI> locations = new HashSet<>();
        for (final File file : this.locations) {
            final URI uri = file.toURI();
            final URI relativize = contents.relativize(uri);
            if (!relativize.equals(uri)) {
                locations.add(relativize);
            }
        }

        return locations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final License license = (License) o;

        if (!this.key.equals(license.key)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    public boolean implies(final License fullLicense) {
        return fullLicense.key.contains(this.key);
    }
}