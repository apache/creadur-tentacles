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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.CommonsLogLogChute;

public final class Templates {

    private static final Templates INSTANCE = new Templates();

    private final VelocityEngine engine;

    private Templates() {
        final Properties properties = new Properties();
        properties.setProperty("file.resource.loader.cache", "true");
        properties.setProperty("resource.loader", "file, class");
        properties.setProperty("class.resource.loader.description",
                "Velocity Classpath Resource Loader");
        properties
                .setProperty("class.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        properties.setProperty("runtime.log.logsystem.class",
                CommonsLogLogChute.class.getName());
        properties.setProperty("runtime.log.logsystem.commons.logging.name",
                Templates.class.getName());

        this.engine = new VelocityEngine();
        this.engine.init(properties);
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

    public static Builder template(final String name, final IOSystem ioSystem) {
        return INSTANCE.new Builder(name, ioSystem);
    }

    public class Builder {
        private final IOSystem ioSystem;
        private final String template;
        private final Map<String, Object> map = new HashMap<String, Object>();

        public Builder(final String template, final IOSystem ioSystem) {
            this.template = template;
            this.ioSystem = ioSystem;
        }

        public Builder add(final String key, final Object value) {
            this.map.put(key, value);
            return this;
        }

        public Builder addAll(final Map<String, Object> map) {
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
    }
}
