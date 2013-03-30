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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class TemplateBuilder {
    private final VelocityEngine engine;
    private final IOSystem ioSystem;
    private final String template;
    private final Map<String, Object> map = new HashMap<String, Object>();

    public TemplateBuilder(final String template, final IOSystem ioSystem,
            final VelocityEngine engine) {
        this.template = template;
        this.ioSystem = ioSystem;
        this.engine = engine;
    }

    public TemplateBuilder add(final String key, final Object value) {
        this.map.put(key, value);
        return this;
    }

    public TemplateBuilder addAll(final Map<String, Object> map) {
        this.map.putAll(map);
        return this;
    }

    public String apply() {
        final StringWriter writer = new StringWriter();

        try {
            evaluate(this.template, this.map, writer);
        } catch (final IOException ioe) {
            throw new RuntimeException("can't apply template "
                    + this.template, ioe);
        }

        return writer.toString();
    }

    public File write(final File file) throws IOException {
        this.ioSystem.writeString(file, apply());
        return file;
    }

    private void evaluate(final String template,
            final Map<String, Object> mapContext, final Writer writer)
            throws IOException {

        final URL resource =
                Thread.currentThread().getContextClassLoader()
                        .getResource(template);

        if (resource == null) {
            throw new IllegalStateException(template);
        }

        final VelocityContext context = new VelocityContext(mapContext);
        this.engine.evaluate(context, writer, Templates.class.getName(),
                new InputStreamReader(resource.openStream()));
    }

}