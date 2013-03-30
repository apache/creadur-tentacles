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

import java.util.Properties;

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

    public static TemplateBuilder template(final String name, final IOSystem ioSystem) {
        return INSTANCE.builder(name, ioSystem);
    }

    private TemplateBuilder builder(final String name, final IOSystem ioSystem) {
        return new TemplateBuilder(name, ioSystem, this.engine);
    }
}
