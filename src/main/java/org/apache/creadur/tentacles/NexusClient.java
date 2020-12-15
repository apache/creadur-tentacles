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

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.*;
import org.codehaus.swizzle.stream.StreamLexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

public class NexusClient {

    private static final Logger log = LogManager.getLogger(NexusClient.class);
    private static final String SLASH = "/";
    private static final String ONE_UP = "../";
    private static final String USER_AGENT_CONTENTS = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13";

    private final CloseableHttpClient client;
    private final FileSystem fileSystem;
    private final IOSystem ioSystem;
    private final int retries;

    public NexusClient(final Platform platform) {

        System.setProperty("http.keepAlive", "false");
        System.setProperty("http.maxConnections", "50");

        this.retries = Integer.parseInt(System.getProperty("NexusClient.retries", "5"));

        this.client = HttpClientBuilder.create().disableContentCompression()
                .build();
        this.fileSystem = platform.getFileSystem();
        this.ioSystem = platform.getIoSystem();
    }

    public File download(final URI uri, final File file) throws IOException {
        if (file.exists()) {

            final long length = getContentLength(uri);

            if (file.length() == length) {
                log.info("Exists " + uri);
                return file;
            } else {
                log.info("Incomplete " + uri);
            }
        }

        log.info("Download " + uri);

        final CloseableHttpResponse response = get(uri);

        InputStream content = null;
        try {
            content = response.getEntity().getContent();

            this.fileSystem.mkparent(file);

            this.ioSystem.copy(content, file);
        } finally {
            if (content != null) {
                content.close();
            }

            response.close();
        }

        return file;
    }

    private Long getContentLength(final URI uri) throws IOException {
        final CloseableHttpResponse head = head(uri);
        final Header[] headers = head.getHeaders(HttpHeaders.CONTENT_LENGTH);

        if (headers != null && headers.length >= 1) {
            return Long.valueOf(headers[0].getValue());
        }

        head.close();

        return (long) -1;
    }

    private CloseableHttpResponse get(final URI uri) throws IOException {
        return get(new HttpGet(uri), this.retries);
    }

    private CloseableHttpResponse head(final URI uri) throws IOException {
        return get(new HttpHead(uri), this.retries);
    }

    private CloseableHttpResponse get(final HttpUriRequest request, int tries) throws IOException {
        try {
            request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_CONTENTS);
            return this.client.execute(request);
        } catch (final IOException e) {
            if (tries > 0) {
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException ie) {
                    Thread.interrupted();
                    throw new IOException("Interrupted", ie);
                }
                return get(request, tries--);
            } else {
                throw e;
            }
        }
    }

    public Set<URI> crawl(final URI index) throws IOException {
        log.info("Crawl " + index);
        final Set<URI> resources = new LinkedHashSet<URI>();

        final CloseableHttpResponse response = get(index);

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

                if (name.equals(ONE_UP)) {
                    continue;
                }
                if (link.equals(ONE_UP)) {
                    continue;
                }

                if (name.endsWith(SLASH)) {
                    crawl.add(uri);
                    continue;
                }

                resources.add(uri);

            } finally {
                lexer.unmark();
            }
        }

        content.close();
        response.close();

        for (final URI uri : crawl) {
            resources.addAll(crawl(uri));
        }

        return resources;
    }
}