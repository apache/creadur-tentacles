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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
        Logger root = Logger.getRootLogger();

        root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
        root.setLevel(Level.INFO);
    }

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Main.class);


    private final File local;
    private final URI staging;
    private final File repository;
    private final File content;
    private Reports reports;
    private Map<String, String> licenses = new HashMap<String, String>();
    private String filter;
    private final NexusClient client = new NexusClient();

    private final Configuration configuration;

    public Main(final String... args) throws Exception {

        configuration = new Configuration(args);

        this.staging = configuration.getStaging();

        this.local = new File(rootDirectoryForLocalOutput(args));

        Files.mkdirs(local);

        this.repository = new File(local, "repo");
        this.content = new File(local, "content");

        Files.mkdirs(repository);
        Files.mkdirs(content);

        log.info("Repo: " + staging);
        log.info("Local: " + local);

        this.reports = new Reports();

        this.filter = System.getProperty("filter", "org/apache/openejb");
        final URL style = this.getClass().getClassLoader().getResource("legal/style.css");
        IO.copy(style.openStream(), new File(local, "style.css"));

        licenses("asl-2.0");
        licenses("cpl-1.0");
        licenses("cddl-1.0");
    }

    private String rootDirectoryForLocalOutput(String... args) {
        final String rootDirectoryForLocal;
        if (args.length > 1) {
            rootDirectoryForLocal = args[1];
        } else {
            rootDirectoryForLocal = new File(this.staging.getPath()).getName();
        }
        return rootDirectoryForLocal;
    }


    private void licenses(String s) throws IOException {
        URL aslURL = this.getClass().getClassLoader().getResource("licenses/" + s + ".txt");
        licenses.put(s, IO.slurp(aslURL).trim());
    }

    public static void main(String[] args) throws Exception {
        new Main(args).main();
    }

    private void main() throws Exception {

        prepare();

        final List<File> jars = Files.collect(repository, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        final List<Archive> archives = new ArrayList<Archive>();
        for (File file : jars) {
            final Archive archive = new Archive(file);
            archives.add(archive);
        }

        Templates.template("legal/archives.vm")
                .add("archives", archives)
                .add("reports", reports)
                .write(new File(local, "archives.html"));

        reportLicenses(archives);
        reportNotices(archives);
        reportDeclaredLicenses(archives);
        reportDeclaredNotices(archives);
    }

    private void reportLicenses(List<Archive> archives) throws IOException {
        initLicenses(archives);

        Templates.template("legal/licenses.vm")
                .add("licenses", getLicenses(archives))
                .add("reports", reports)
                .write(new File(local, "licenses.html"));
    }

    private void initLicenses(List<Archive> archives) throws IOException {
        Map<License, License> licenses = new HashMap<License, License>();

        for (Archive archive : archives) {
            List<File> files = Files.collect(contents(archive.getFile()), new LicenseFilter());
            for (File file : files) {
                final License license = new License(IO.slurp(file));

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

    private Collection<License> getLicenses(List<Archive> archives) {
        Set<License> licenses = new LinkedHashSet<License>();
        for (Archive archive : archives) {
            licenses.addAll(archive.getLicenses());
        }
        return licenses;
    }

    private void reportDeclaredLicenses(List<Archive> archives) throws IOException {

        for (Archive archive : archives) {

            classifyLicenses(archive);
        }
        for (Archive archive : archives) {

            Templates.template("legal/archive-licenses.vm")
                    .add("archive", archive)
                    .add("reports", reports)
                    .write(new File(local, reports.licenses(archive)));
        }

    }

    private void classifyLicenses(Archive archive) throws IOException {
        final Set<License> undeclared = new HashSet<License>(archive.getLicenses());

        final File contents = contents(archive.getFile());
        final List<File> files = Files.collect(contents, new Filters(new DeclaredFilter(contents), new LicenseFilter()));

        for (File file : files) {

            final License license = new License(IO.slurp(file));

            undeclared.remove(license);

        }

        archive.getOtherLicenses().addAll(undeclared);

        final Set<License> declared = new HashSet<License>(archive.getLicenses());
        declared.removeAll(undeclared);
        archive.getDeclaredLicenses().addAll(declared);


        for (License license : undeclared) {

            for (License declare : declared) {
                if (license.implies(declare)) {
                    archive.getOtherLicenses().remove(license);
                }
            }
        }
    }

    private void reportDeclaredNotices(List<Archive> archives) throws IOException {

        for (Archive archive : archives) {

            final Set<Notice> undeclared = new HashSet<Notice>(archive.getNotices());

            final File contents = contents(archive.getFile());
            final List<File> files = Files.collect(contents, new Filters(new DeclaredFilter(contents), new NoticeFilter()));

            for (File file : files) {

                final Notice notice = new Notice(IO.slurp(file));

                undeclared.remove(notice);
            }

            archive.getOtherNotices().addAll(undeclared);

            final Set<Notice> declared = new HashSet<Notice>(archive.getNotices());
            declared.removeAll(undeclared);
            archive.getDeclaredNotices().addAll(declared);

            for (Notice notice : undeclared) {

                for (Notice declare : declared) {
                    if (notice.implies(declare)) {
                        archive.getOtherLicenses().remove(notice);
                    }
                }
            }


            Templates.template("legal/archive-notices.vm")
                    .add("archive", archive)
                    .add("reports", reports)
                    .write(new File(local, reports.notices(archive)));
        }
    }

    private void reportNotices(List<Archive> archives) throws IOException {
        Map<Notice, Notice> notices = new HashMap<Notice, Notice>();

        for (Archive archive : archives) {
            List<File> files = Files.collect(contents(archive.getFile()), new NoticeFilter());
            for (File file : files) {
                final Notice notice = new Notice(IO.slurp(file));

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

        Templates.template("legal/notices.vm")
                .add("notices", notices.values())
                .add("reports", reports)
                .write(new File(local, "notices.html"));
    }

    public class Reports {
        public String licenses(Archive archive) {
            return archive.uri.toString().replace('/', '.') + ".licenses.html";
        }

        public String notices(Archive archive) {
            return archive.uri.toString().replace('/', '.') + ".notices.html";
        }

    }


    private List<URI> allNoticeFiles() {
        List<File> legal = Files.collect(content, new LegalFilter());
        for (File file : legal) {
            log.info("Legal " + file);
        }

        URI uri = local.toURI();
        List<URI> uris = new ArrayList<URI>();
        for (File file : legal) {
            URI full = file.toURI();
            URI relativize = uri.relativize(full);
            uris.add(relativize);
        }
        return uris;
    }

    private void prepare() throws URISyntaxException, IOException {
        final Set<File> files = new HashSet<File>();

        if (staging.toString().startsWith("http")) {
            final Set<URI> resources = client.crawl(staging);

            for (URI uri : resources) {
                if (!uri.getPath().matches(".*(war|jar|zip)")) continue;
                files.add(download(uri));
            }
        } else if (staging.toString().startsWith("file:")) {
            File file = new File(staging);
            List<File> collect = Files.collect(file, new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String path = pathname.getAbsolutePath();
                    return path.matches(filter) && isValidArchive(path);
                }
            });

            for (File f : collect) {
                files.add(copy(f));
            }
        }

        for (File file : files) {
            unpack(file);
        }
    }

    private void unpack(File archive) throws IOException {
        log.info("Unpack " + archive);

        try {
            final ZipInputStream zip = IO.unzip(archive);

            final File contents = contents(archive);

            try {
                ZipEntry entry = null;

                while ((entry = zip.getNextEntry()) != null) {

                    if (entry.isDirectory()) continue;

                    final String path = entry.getName();

                    final File fileEntry = new File(contents, path);

                    Files.mkparent(fileEntry);

                    // Open the output file

                    IO.copy(zip, fileEntry);

                    if (fileEntry.getName().endsWith(".jar")) {
                        unpack(fileEntry);
                    }
                }
            } finally {
                IO.close(zip);
            }
        } catch (IOException e) {
            log.error("Not a zip " + archive);
        }
    }

    public class License {
        private final String text;
        private String key;
        private Set<Archive> archives = new HashSet<Archive>();
        private List<File> locations = new ArrayList<File>();

        public License(String text) {
            key = text.replaceAll("[ \\n\\t\\r]+", "").toLowerCase().intern();

            for (Map.Entry<String, String> license : licenses.entrySet()) {
                text = text.replace(license.getValue(), String.format("---[%s - full text]---\n\n", license.getKey()));
            }
            this.text = text.intern();
        }

        public String getText() {
            return text;
        }

        public String getKey() {
            return key;
        }

        public Set<Archive> getArchives() {
            return archives;
        }

        public Set<URI> locations(Archive archive) {
            URI contents = contents(archive.getFile()).toURI();
            Set<URI> locations = new HashSet<URI>();
            for (File file : this.locations) {
                URI uri = file.toURI();
                URI relativize = contents.relativize(uri);
                if (!relativize.equals(uri)) {
                    locations.add(relativize);
                }
            }

            return locations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            License license = (License) o;

            if (!key.equals(license.key)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public boolean implies(License fullLicense) {
            return fullLicense.key.contains(this.key);
        }
    }

    public class Notice {
        private final String text;
        private String key;
        private Set<Archive> archives = new HashSet<Archive>();
        private List<File> locations = new ArrayList<File>();

        public Notice(String text) {
            this.text = text.intern();
            key = text.replaceAll("[ \\n\\t\\r]+", "").toLowerCase().intern();
        }

        public String getText() {
            return text;
        }

        public String getKey() {
            return key;
        }

        public Set<Archive> getArchives() {
            return archives;
        }

        public Set<URI> locations(Archive archive) {
            URI contents = contents(archive.getFile()).toURI();
            Set<URI> locations = new HashSet<URI>();
            for (File file : this.locations) {
                URI uri = file.toURI();
                URI relativize = contents.relativize(uri);
                if (!relativize.equals(uri)) {
                    locations.add(relativize);
                }
            }

            return locations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Notice notice = (Notice) o;

            if (!key.equals(notice.key)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public boolean implies(Notice fullLicense) {
            return fullLicense.key.contains(this.key);
        }

    }

    private File contents(File archive) {
        String path = archive.getAbsolutePath().substring(local.getAbsolutePath().length() + 1);

        if (path.startsWith("repo/")) path = path.substring("repo/".length());
        if (path.startsWith("content/")) path = path.substring("content/".length());

        final File contents = new File(content, path + ".contents");
        Files.mkdirs(contents);
        return contents;
    }

    private File download(URI uri) throws IOException {

        final File file = getFile(uri);

        return client.download(uri, file);
    }

    private File copy(File src) throws IOException {
        final URI uri = src.toURI();

        final File file = getFile(uri);

        log.info("Copy " + uri);

        Files.mkparent(file);

        IO.copy(IO.read(src), file);

        return file;
    }

    private static class LegalFilter implements FileFilter {

        private static final NoticeFilter notice = new NoticeFilter();
        private static final LicenseFilter license = new LicenseFilter();

        @Override
        public boolean accept(File pathname) {
            return notice.accept(pathname) || license.accept(pathname);
        }
    }

    private static class NoticeFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            final String name = pathname.getName().toLowerCase();

            if (name.equals("notice")) return true;
            if (name.equals("notice.txt")) return true;

            return false;
        }
    }

    private static class LicenseFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            final String name = pathname.getName().toLowerCase();

            if (name.equals("license")) return true;
            if (name.equals("license.txt")) return true;

            return false;
        }
    }

    private static class N implements FileFilter {
        private final FileFilter filter;

        private N(FileFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(File pathname) {
            return !filter.accept(pathname);
        }
    }

    private static class DeclaredFilter implements FileFilter {
        private final File file;

        private DeclaredFilter(File file) {
            this.file = file;
        }

        @Override
        public boolean accept(File file) {
            while (file != null) {
                if (file.equals(this.file)) break;

                if (file.isDirectory() && file.getName().endsWith(".contents")) return false;
                file = file.getParentFile();
            }

            return true;
        }
    }

    private static class Filters implements FileFilter {

        List<FileFilter> filters = new ArrayList<FileFilter>();

        private Filters(FileFilter... filters) {
            for (FileFilter filter : filters) {
                this.filters.add(filter);
            }
        }

        @Override
        public boolean accept(File file) {
            for (FileFilter filter : filters) {
                if (!filter.accept(file)) return false;
            }

            return true;
        }
    }

    public class Archive {

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

        public Archive(File file) {
            this.uri = repository.toURI().relativize(file.toURI());
            this.file = file;
            this.map = map();
        }

        public Set<License> getDeclaredLicenses() {
            return declaredLicenses;
        }

        public Set<Notice> getDeclaredNotices() {
            return declaredNotices;
        }

        public Set<License> getOtherLicenses() {
            return otherLicenses;
        }

        public Set<Notice> getOtherNotices() {
            return otherNotices;
        }

        public Set<License> getLicenses() {
            return licenses;
        }

        public Set<Notice> getNotices() {
            return notices;
        }

        public URI getUri() {
            return uri;
        }

        public File getFile() {
            return file;
        }

        public Map<URI, URI> getLegal() {
            return map;
        }

        public Map<URI, URI> getOtherLegal() {
            if (others == null) {
                others = mapOther();
            }
            return others;
        }

        private Map<URI, URI> mapOther() {
            final File jarContents = contents(file);
            final List<File> legal = Files.collect(jarContents, new Filters(new N(new DeclaredFilter(jarContents)), new LegalFilter()));

            Map<URI, URI> map = new LinkedHashMap<URI, URI>();
            for (File file : legal) {
                URI name = jarContents.toURI().relativize(file.toURI());
                URI link = local.toURI().relativize(file.toURI());

                map.put(name, link);
            }
            return map;
        }

        private Map<URI, URI> map() {
            final File jarContents = contents(file);
            final List<File> legal = Files.collect(jarContents, new Filters(new DeclaredFilter(jarContents), new LegalFilter()));

            Map<URI, URI> map = new LinkedHashMap<URI, URI>();
            for (File file : legal) {
                URI name = jarContents.toURI().relativize(file.toURI());
                URI link = local.toURI().relativize(file.toURI());

                map.put(name, link);
            }
            return map;
        }
    }

    private File getFile(URI uri) {
        final String name = uri.toString().replace(staging.toString(), "").replaceFirst("^/", "");
        return new File(repository, name);
    }

    private boolean isValidArchive(String path) {
        return path.matches(".*\\.(jar|zip|war|ear|tar.gz)");
    }
}
