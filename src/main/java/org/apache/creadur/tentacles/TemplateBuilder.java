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
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class TemplateBuilder {
    private static final String LOG_TAG_NAME = TemplateBuilder.class.getName();

    private final TentaclesResources tentaclesResources;
    private final VelocityEngine engine;
    private final IOSystem ioSystem;
    private final String templateName;
    private final Map<String, Object> templateContextMap =
            new ConcurrentHashMap<String, Object>();

    public TemplateBuilder(final String template, final IOSystem ioSystem,
            final VelocityEngine engine,
            final TentaclesResources tentaclesResources) {
        this.templateName = template;
        this.ioSystem = ioSystem;
        this.engine = engine;
        this.tentaclesResources = tentaclesResources;
    }

    public TemplateBuilder add(final String key, final Object value) {
        this.templateContextMap.put(key, value);
        return this;
    }

    public TemplateBuilder addAll(final Map<String, Object> map) {
        this.templateContextMap.putAll(map);
        return this;
    }

    public String apply() {
        final StringWriter writer = new StringWriter();

        evaluate(writer);

        return writer.toString();
    }

    public File write(final File file) throws IOException {
        this.ioSystem.writeString(file, apply());
        return file;
    }

    private void evaluate(final Writer writer) {
        try {
            final Reader templateReader =
                    this.tentaclesResources.read(this.templateName);

            final VelocityContext context =
                    new VelocityContext(this.templateContextMap);
            this.engine.evaluate(context, writer, LOG_TAG_NAME, templateReader);

        } catch (final IOException ioe) {
            throw new RuntimeException("can't apply template "
                    + this.templateName, ioe);
        }
    }

}