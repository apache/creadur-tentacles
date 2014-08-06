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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.swizzle.stream.StreamLexer;

public class NexusClient {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(NexusClient.class);

    private final HttpClient client;
    private final FileSystem fileSystem;
    private final IOSystem ioSystem;

    public NexusClient(final Platform platform) {
        this.client = HttpClientBuilder.create().build();
        this.fileSystem = platform.getFileSystem();
        this.ioSystem = platform.getIoSystem();
    }

    public File download(final URI uri, final File file) throws IOException {
        if (file.exists()) {

            final long length = getConentLength(uri);

            if (file.length() == length) {
                log.info("Exists " + uri);
                return file;
            } else {
                log.info("Incomplete " + uri);
            }
        }

        log.info("Download " + uri);

        final HttpResponse response = get(uri);

        final InputStream content = response.getEntity().getContent();

        this.fileSystem.mkparent(file);

        this.ioSystem.copy(content, file);

        return file;
    }

    private long getConentLength(final URI uri) throws IOException {
        final HttpResponse head = head(uri);
        final Header[] headers = head.getHeaders("Content-Length");

        for (final Header header : headers) {
            return new Long(header.getValue());
        }

        return -1;
    }

    private HttpResponse get(final URI uri) throws IOException {
        final HttpGet request = new HttpGet(uri);
        request.setHeader(
                "User-Agent",
                "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
        return this.client.execute(request);
    }

    private HttpResponse head(final URI uri) throws IOException {
        final HttpHead request = new HttpHead(uri);
        request.setHeader(
                "User-Agent",
                "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
        return this.client.execute(request);
    }

    public Set<URI> crawl(final URI index) throws IOException {
        log.info("Crawl " + index);
        final Set<URI> resources = new LinkedHashSet<URI>();

        final HttpResponse response = get(index);

        final InputStream content = response.getEntity().getContent();
        final StreamLexer lexer = new StreamLexer(content);

        final Set<URI> crawl = new LinkedHashSet<URI>();

        // <a
        // href="https://repository.apache.org/content/repositories/orgapacheopenejb-094/archetype-catalog.xml">archetype-catalog.xml</a>
        while (lexer.readAndMark("<a ", "/a>")) {

            try {
                final String link = lexer.peek("href=\"", "\"");
                final String name = lexer.peek(">", "<");

                final URI uri = index.resolve(link);

                if (name.equals("../")) {
                    continue;
                }
                if (link.equals("../")) {
                    continue;
                }

                if (name.endsWith("/")) {
                    crawl.add(uri);
                    continue;
                }

                resources.add(uri);

            } finally {
                lexer.unmark();
            }
        }

        content.close();

        for (final URI uri : crawl) {
            resources.addAll(crawl(uri));
        }

        return resources;
    }
}