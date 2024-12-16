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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.*;

public class Main {

/* TENTACLES-12: disabled root logger configuration       
    static {
        final Logger root = LogManager.getRootLogger();
        root.addAppender(new ConsoleAppender(new PatternLayout(
                PatternLayout.TTCC_CONVERSION_PATTERN)));
        root.setLevel(Level.INFO);
    }
        */

    private static final Logger log = LogManager.getLogger(Main.class);
    private static final String CRAWL_PATTERN = ".*\\.(jar|zip|war|ear|rar|tar.gz)";

    private final Reports reports;
    private final Licenses licenses;

    private final Layout layout;
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
        this(configuration, platform, new Templates(platform), new Layout(
                platform, configuration));
    }

    public Main(final Configuration configuration, final Platform platform,
            final Templates templates, final Layout layout) throws Exception {
        this.platform = platform;
        this.configuration = configuration;
        this.layout = layout;
        this.fileSystem = platform.getFileSystem();
        this.ioSystem = platform.getIoSystem();
        this.tentaclesResources = platform.getTentaclesResources();
        this.templates = templates;

        this.reports = new Reports();

        log.info("Remote repository: {}", this.configuration.getStagingRepositoryURI());
        log.info("Local root directory: {}", this.layout.getLocalRootDirectory());

        this.tentaclesResources.copyTo("legal/style.css",
                new File(this.layout.getOutputDirectory(), "style.css"));

        this.licenses = loadLicensesFrom(platform);
    }

    public static void main(final String[] args) throws Exception {
    	
    	log.info("Launching Apache Tentacles ...");
    	
    	if(args == null || args.length < 1) {
    		log.error("Error: Input parameter missing - you did not specify any component to run Apache Tentacles on.");
    		log.error("Please launch Apache Tentacles with an URI to work on such as 'https://repository.apache.org/content/repositories/orgapachecreadur-1000/'.");
    	} else {
    		new Main(args).main();
    	}
    	
    }

    private void main() throws Exception {

        unpackContents(mirrorRepositoryFrom(this.configuration));

        reportOn(archivesIn(this.layout.getRepositoryDirectory()));
    }

    private List<Archive> archivesIn(final File repository) {
        final List<File> jars = this.fileSystem.documentsFrom(repository);

        final List<Archive> archives = new ArrayList<>();
        for (final File file : jars) {
            final Archive archive =
                    new Archive(file, this.fileSystem, this.layout);
            archives.add(archive);
        }
        return archives;
    }

    private void reportOn(final List<Archive> archives) throws IOException {
        this.templates
                .template("legal/archives.vm")
                .add("archives", archives)
                .add("reports", this.reports)
                .write(new File(this.layout.getOutputDirectory(),
                        "archives.html"));

        reportLicenses(archives);
        reportNotices(archives);
        reportDeclaredLicenses(archives);
        reportDeclaredNotices(archives);
    }

    private void reportLicenses(final List<Archive> archives)
            throws IOException {
        initLicenses(archives);

        this.templates
                .template("legal/licenses.vm")
                .add("licenses", getLicenses(archives))
                .add("reports", this.reports)
                .write(new File(this.layout.getOutputDirectory(),
                        "licenses.html"));
    }

    private void initLicenses(final List<Archive> archives) throws IOException {
        final Map<License, License> licenses = new HashMap<>();

        for (final Archive archive : archives) {
            final List<File> files =
                    this.fileSystem.licensesFrom(archive.contentsDirectory());
            for (final File file : files) {
                final License license = this.licenses.from(file);

                License existing = licenses.get(license);
                if (existing == null) {
                    licenses.put(license, license);
                    existing = license;
                }

                existing.getLocations().add(file);
                existing.getArchives().add(archive);
                archive.getLicenses().add(existing);
            }
        }
    }

    private Collection<License> getLicenses(final List<Archive> archives) {
        final Set<License> licenses = new LinkedHashSet<>();
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
                    .write(new File(this.layout.getOutputDirectory(),
                            this.reports.licenses(archive)));
        }

    }

    private void classifyLicenses(final Archive archive) throws IOException {
        final Set<License> undeclared =
                new HashSet<>(archive.getLicenses());

        final File contents = archive.contentsDirectory();
        final List<File> files = this.fileSystem.licensesDeclaredIn(contents);

        for (final File file : files) {

            undeclared.remove(this.licenses.from(file));

        }

        archive.getOtherLicenses().addAll(undeclared);

        final Set<License> declared =
                new HashSet<>(archive.getLicenses());
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
                    new HashSet<>(archive.getNotices());

            final File contents = archive.contentsDirectory();
            final List<File> files =
                    this.fileSystem.noticesDeclaredIn(contents);

            for (final File file : files) {

                final Notice notice = new Notice(this.ioSystem.slurp(file));

                undeclared.remove(notice);
            }

            archive.getOtherNotices().addAll(undeclared);

            final Set<Notice> declared =
                    new HashSet<>(archive.getNotices());
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
                    .write(new File(this.layout.getOutputDirectory(),
                            this.reports.notices(archive)));
        }
    }

    private void reportNotices(final List<Archive> archives) throws IOException {
        final Map<Notice, Notice> notices = new HashMap<>();

        for (final Archive archive : archives) {
            final List<File> noticeDocuments =
                    this.fileSystem.noticesOnly(archive.contentsDirectory());
            for (final File file : noticeDocuments) {
                final Notice notice = new Notice(this.ioSystem.slurp(file));

                Notice existing = notices.get(notice);
                if (existing == null) {
                    notices.put(notice, notice);
                    existing = notice;
                }

                existing.getLocations().add(file);
                existing.getArchives().add(archive);
                archive.getNotices().add(existing);
            }
        }

        this.templates
                .template("legal/notices.vm")
                .add("notices", notices.values())
                .add("reports", this.reports)
                .write(new File(this.layout.getOutputDirectory(),
                        "notices.html"));
    }

    private void unpackContents(final Set<File> files) throws IOException {
        for (final File file : files) {
            unpack(file);
        }
    }

    private Set<File> mirrorRepositoryFrom(final Configuration configuration)
            throws IOException {
        final Set<File> files = new HashSet<>();
        if (HTTP.isRepositoryFor(configuration)) {
            final NexusClient client = new NexusClient(this.platform);
            final Set<URI> resources =
                    client.crawl(configuration.getStagingRepositoryURI());

            for (final URI uri : resources) {
                if (!uri.getPath().matches(CRAWL_PATTERN)) {
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
        log.info("Unpack {}", archive);

        try {
            final ZipInputStream zip = this.ioSystem.unzip(archive);

            final File contents =
                    new Archive(archive, this.fileSystem, this.layout)
                            .contentsDirectory();

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
            log.error("Not a zip {}", archive);
        }
    }

    private File copyToMirror(final File src) throws IOException {
        final URI uri = src.toURI();

        final File file = mirroredFrom(uri);

        log.info("Copy {}", uri);

        this.fileSystem.mkparent(file);

        this.ioSystem.copy(this.ioSystem.read(src), file);

        return file;
    }

    private File mirroredFrom(final URI uri) {
        final String name =
                uri.toString()
                        .replace(
                                this.configuration.getStagingRepositoryURI()
                                        .toString(), "").replaceFirst("^/", "");
        return new File(this.layout.getRepositoryDirectory(), name);
    }

}
