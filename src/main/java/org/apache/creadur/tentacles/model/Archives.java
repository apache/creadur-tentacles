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
package org.apache.creadur.tentacles.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * To be marshalled to json as an archives.json file
 * Will contain a summary list of the artifacts intended for tabular display
 */
@XmlRootElement
public class Archives {

    private final List<Item> archives = new ArrayList<>();

    /**
     * Required for JAXB
     */
    public Archives() {
    }

    public List<Item> getArchives() {
        return archives;
    }

    public void add(Item item) {
        archives.add(item);
    }

    public Item addItem() {
        final Item item = new Item();
        archives.add(item);
        return item;
    }

    public static class Item {

        /**
         * Required for JAXB
         */
        public Item() {
        }

        /**
         * The path in the repo, minus the repo portion itself (relative)
         */
        private String path;

        /**
         * Just the file name with no path
         */
        private String name;

        /**
         * Jar, war, zip
         */
        private String type;

        /**
         * Count of how many jars are in this archive
         */
        private int jars;

        /**
         * Unique ID of the license file
         */
        private String license;


        /**
         * Unique ID of the notice file
         */
        private String notice;


        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getJars() {
            return jars;
        }

        public void setJars(int jars) {
            this.jars = jars;
        }

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }

        public String getNotice() {
            return notice;
        }

        public void setNotice(String notice) {
            this.notice = notice;
        }

        public Item path(String path) {
            this.path = path;
            return this;
        }

        public Item name(String name) {
            this.name = name;
            return this;
        }

        public Item type(String type) {
            this.type = type;
            return this;
        }

        public Item jars(int jars) {
            this.jars = jars;
            return this;
        }

        public Item license(String license) {
            this.license = license;
            return this;
        }

        public Item notice(String notice) {
            this.notice = notice;
            return this;
        }
    }
}
