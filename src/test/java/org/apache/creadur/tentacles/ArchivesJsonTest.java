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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.creadur.tentacles.model.Archives;
import org.junit.Assert;
import org.junit.Test;

public class ArchivesJsonTest extends Assert {

    @Test
    public void test() throws Exception {

        final Archives archives = new Archives();
        archives.addItem()
                .name("hello.jar")
                .path("one/two/hello.jar")
        ;

        final ObjectMapper mapper = new ObjectMapper();
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        // configure as necessary
        mapper.registerModule(module);

        mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, archives);
    }
}
