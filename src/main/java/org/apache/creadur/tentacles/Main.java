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
package org.apache.creadur.tentacles;

import static org.apache.creadur.tentacles.LicenseType.loadLicensesFrom;
import static org.apache.creadur.tentacles.RepositoryType.HTTP;
import static org.apache.creadur.tentacles.RepositoryType.LOCAL_FILE_SYSTEM;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @version $Rev$ $Date$
 */
public class Main {

    static {
        final Logger root = Logger.getRootLogger();

        root.addAppender(new ConsoleAppender(new PatternLayout(
                PatternLayout.TTCC_CONVERSION_PATTERN)));
        root.setLevel(Level.INFO);
    }

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(Main.class);

    private final File localRootDirectory;
    private final File output;
    private final File repository;
    private final File contentRootDirectory;
    private final Reports reports;
    private final Map<String, String> licenses;

    private final Platform platform;
    private final Configuration configuration;
    private final FileSystem fileSystem;
    private final IOSystem ioSystem;
    private final TentaclesResources tentaclesResources;
    private final Templates templates;

    public Main(final String... args) throws Exception {
        this(new Configuration(args), Platform.aPlatform());
    }

    public Main(final Configuration configuration, final Platform platform)
            throws Exception {
        this(configuration, platform, new Templates(platform));
    }

    public Main(final Configuration configuration, final Platform platform,
            final Templates templates) throws Exception {
        this.platform = platform;
        this.configuration = configuration;
        this.fileSystem = platform.getFileSystem();
        this.ioSystem = platform.getIoSystem();
        this.tentaclesResources = platform.getTentaclesResources();
        this.templates = templates;

        this.localRootDirectory =
                new File(this.configuration.getRootDirectoryForLocalOutput());

        this.fileSystem.mkdirs(this.localRootDirectory);

        this.repository = new File(this.localRootDirectory, "repo");
        this.contentRootDirectory = new File(this.localRootDirectory, "content");
        this.output = this.localRootDirectory;

        this.fileSystem.mkdirs(this.repository);
        this.fileSystem.mkdirs(this.contentRootDirectory);

        log.info("Remote repository: "
                + this.configuration.getStagingRepositoryURI());
        log.info("Local root directory: " + this.localRootDirectory);

        this.reports = new Reports();

        this.tentaclesResources.copyTo("legal/style.css", new File(this.output,
                "style.css"));

        this.licenses = loadLicensesFrom(this.tentaclesResources);
    }

    public static void main(final String[] args) throws Exception {
        new Main(args).main();
    }

    private void main() throws Exception {

        unpackContents(mirrorRepositoryFrom(this.configuration));

        reportOn(archivesIn(this.repository));
    }

    private List<Archive> archivesIn(final File repository) {
        final List<File> jars = this.fileSystem.documentsFrom(repository);

        final List<Archive> archives = new ArrayList<Archive>();
        for (final File file : jars) {
            final Archive archive =
                    new Archive(file, this.fileSystem, this.localRootDirectory,
                            repository);
            archives.add(archive);
        }
        return archives;
    }

    private void reportOn(final List<Archive> archives) throws IOException {
        this.templates.template("legal/archives.vm").add("archives", archives)
                .add("reports", this.reports)
                .write(new File(this.output, "archives.html"));

        reportLicenses(archives);
        reportNotices(archives);
        reportDeclaredLicenses(archives);
        reportDeclaredNotices(archives);
    }

    private void reportLicenses(final List<Archive> archives)
            throws IOException {
        initLicenses(archives);

        this.templates.template("legal/licenses.vm")
                .add("licenses", getLicenses(archives))
                .add("reports", this.reports)
                .write(new File(this.output, "licenses.html"));
    }

    private void initLicenses(final List<Archive> archives) throws IOException {
        final Map<License, License> licenses = new HashMap<License, License>();

        for (final Archive archive : archives) {
            final List<File> files =
                    this.fileSystem.licensesFrom(contents(archive));
            for (final File file : files) {
                final License license = new License(this.ioSystem.slurp(file));

                License existing = licenses.get(license);
                if (existing == null) {
                    licenses.put(license, license);
                    existing = license;
                }

                existing.locations.add(file);
                existing.getArchives().add(archive);
                archive.getLicenses().add(existing);
            }
        }
    }

    private Collection<License> getLicenses(final List<Archive> archives) {
        final Set<License> licenses = new LinkedHashSet<License>();
        for (final Archive archive : archives) {
            licenses.addAll(archive.getLicenses());
        }
        return licenses;
    }

    private void reportDeclaredLicenses(final List<Archive> archives)
            throws IOException {

        for (final Archive archive : archives) {

            classifyLicenses(archive);
        }
        for (final Archive archive : archives) {

            this.templates
                    .template("legal/archive-licenses.vm")
                    .add("archive", archive)
                    .add("reports", this.reports)
                    .write(new File(this.output, this.reports.licenses(archive)));
        }

    }

    private void classifyLicenses(final Archive archive) throws IOException {
        final Set<License> undeclared =
                new HashSet<License>(archive.getLicenses());

        final File contents = contents(archive);
        final List<File> files = this.fileSystem.licensesDeclaredIn(contents);

        for (final File file : files) {

            final License license = new License(this.ioSystem.slurp(file));

            undeclared.remove(license);

        }

        archive.getOtherLicenses().addAll(undeclared);

        final Set<License> declared =
                new HashSet<License>(archive.getLicenses());
        declared.removeAll(undeclared);
        archive.getDeclaredLicenses().addAll(declared);

        for (final License license : undeclared) {

            for (final License declare : declared) {
                if (license.implies(declare)) {
                    archive.getOtherLicenses().remove(license);
                }
            }
        }
    }

    private void reportDeclaredNotices(final List<Archive> archives)
            throws IOException {

        for (final Archive archive : archives) {

            final Set<Notice> undeclared =
                    new HashSet<Notice>(archive.getNotices());

            final File contents = contents(archive);
            final List<File> files =
                    this.fileSystem.noticesDeclaredIn(contents);

            for (final File file : files) {

                final Notice notice = new Notice(this.ioSystem.slurp(file));

                undeclared.remove(notice);
            }

            archive.getOtherNotices().addAll(undeclared);

            final Set<Notice> declared =
                    new HashSet<Notice>(archive.getNotices());
            declared.removeAll(undeclared);
            archive.getDeclaredNotices().addAll(declared);

            for (final Notice notice : undeclared) {

                for (final Notice declare : declared) {
                    if (notice.implies(declare)) {
                        archive.getOtherLicenses().remove(notice);
                    }
                }
            }

            this.templates
                    .template("legal/archive-notices.vm")
                    .add("archive", archive)
                    .add("reports", this.reports)
                    .write(new File(this.output, this.reports.notices(archive)));
        }
    }

    private void reportNotices(final List<Archive> archives) throws IOException {
        final Map<Notice, Notice> notices = new HashMap<Notice, Notice>();

        for (final Archive archive : archives) {
            final List<File> noticeDocuments =
                    this.fileSystem.noticesOnly(contents(archive));
            for (final File file : noticeDocuments) {
                final Notice notice = new Notice(this.ioSystem.slurp(file));

                Notice existing = notices.get(notice);
                if (existing == null) {
                    notices.put(notice, notice);
                    existing = notice;
                }

                existing.locations.add(file);
                existing.getArchives().add(archive);
                archive.getNotices().add(existing);
            }
        }

        this.templates.template("legal/notices.vm")
                .add("notices", notices.values()).add("reports", this.reports)
                .write(new File(this.output, "notices.html"));
    }

    private void unpackContents(final Set<File> files) throws IOException {
        for (final File file : files) {
            unpack(file);
        }
    }

    private Set<File> mirrorRepositoryFrom(final Configuration configuration)
            throws IOException {
        final Set<File> files = new HashSet<File>();
        if (HTTP.isRepositoryFor(configuration)) {
            final NexusClient client = new NexusClient(this.platform);
            final Set<URI> resources =
                    client.crawl(configuration.getStagingRepositoryURI());

            for (final URI uri : resources) {
                if (!uri.getPath().matches(".*(war|jar|zip)")) {
                    continue;
                }
                files.add(client.download(uri, mirroredFrom(uri)));
            }
        } else if (LOCAL_FILE_SYSTEM.isRepositoryFor(configuration)) {
            final File file = new File(configuration.getStagingRepositoryURI());
            final List<File> collect =
                    this.platform.getFileSystem().archivesInPath(file,
                            configuration.getFileRepositoryPathNameFilter());

            for (final File f : collect) {
                files.add(copyToMirror(f));
            }
        }
        return files;
    }

    private void unpack(final File archive) throws IOException {
        log.info("Unpack " + archive);

        try {
            final ZipInputStream zip = this.ioSystem.unzip(archive);

            final File contents =
                    contents(new Archive(archive, this.fileSystem,
                            this.localRootDirectory, this.repository));

            try {
                ZipEntry entry = null;

                while ((entry = zip.getNextEntry()) != null) {

                    if (entry.isDirectory()) {
                        continue;
                    }

                    final String path = entry.getName();

                    final File fileEntry = new File(contents, path);

                    this.fileSystem.mkparent(fileEntry);

                    // Open the output file

                    this.ioSystem.copy(zip, fileEntry);

                    if (fileEntry.getName().endsWith(".jar")) {
                        unpack(fileEntry);
                    }
                }
            } finally {
                this.ioSystem.close(zip);
            }
        } catch (final IOException e) {
            log.error("Not a zip " + archive);
        }
    }

    public class License {
        private final String text;
        private final String key;
        private final Set<Archive> archives = new HashSet<Archive>();
        private final List<File> locations = new ArrayList<File>();

        public License(String text) {
            this.key =
                    text.replaceAll("[ \\n\\t\\r]+", "").toLowerCase().intern();

            for (final Map.Entry<String, String> license : Main.this.licenses
                    .entrySet()) {
                text =
                        text.replace(license.getValue(), String.format(
                                "---[%s - full text]---\n\n", license.getKey()));
            }
            this.text = text.intern();
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

        public Set<URI> locations(final Archive archive) {
            final URI contents = contents(archive).toURI();
            final Set<URI> locations = new HashSet<URI>();
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

    public class Notice {
        private final String text;
        private final String key;
        private final Set<Archive> archives = new HashSet<Archive>();
        private final List<File> locations = new ArrayList<File>();

        public Notice(final String text) {
            this.text = text.intern();
            this.key =
                    text.replaceAll("[ \\n\\t\\r]+", "").toLowerCase().intern();
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

        public Set<URI> locations(final Archive archive) {
            final URI contents = contents(archive).toURI();
            final Set<URI> locations = new HashSet<URI>();
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

            final Notice notice = (Notice) o;

            if (!this.key.equals(notice.key)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        public boolean implies(final Notice fullLicense) {
            return fullLicense.key.contains(this.key);
        }

    }

    private File contents(final Archive archive) {
        final File archiveDocument = archive.getFile();
        String path =
                archiveDocument.getAbsolutePath().substring(
                        this.localRootDirectory.getAbsolutePath().length() + 1);

        if (path.startsWith("repo/")) {
            path = path.substring("repo/".length());
        }
        if (path.startsWith("content/")) {
            path = path.substring("content/".length());
        }

        final File contents = new File(this.contentRootDirectory, path + ".contents");
        this.fileSystem.mkdirs(contents);
        return contents;
    }

    private File copyToMirror(final File src) throws IOException {
        final URI uri = src.toURI();

        final File file = mirroredFrom(uri);

        log.info("Copy " + uri);

        this.fileSystem.mkparent(file);

        this.ioSystem.copy(this.ioSystem.read(src), file);

        return file;
    }

    public class Archive {

        private final File localRootDirectory;
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
                final File localRootDirectory, final File repository) {
            this.fileSystem = fileSystem;
            this.localRootDirectory = localRootDirectory;
            this.uri = repository.toURI().relativize(file.toURI());
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
            final File jarContents = contents();
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
                        this.localRootDirectory.toURI()
                                .relativize(file.toURI());

                map.put(name, link);
            }
            return map;
        }

        private Map<URI, URI> map() {
            final File jarContents = contents();
            final List<File> legal =
                    this.fileSystem.legalDocumentsDeclaredIn(jarContents);

            return buildMapFrom(jarContents, legal);
        }

        private File contents() {
            return Main.this.contents(this);
        }
    }

    private File mirroredFrom(final URI uri) {
        final String name =
                uri.toString()
                        .replace(
                                this.configuration.getStagingRepositoryURI()
                                        .toString(), "").replaceFirst("^/", "");
        return new File(this.repository, name);
    }

}
