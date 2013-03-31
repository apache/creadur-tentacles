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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum LicenseType {

    ASL_2_0("asl-2.0"), CPL_1_0("cpl-1.0"), CDDL_1_0("cddl-1.0");

    public static Map<String, String> loadLicensesFrom(
            final TentaclesResources tentaclesResources) throws IOException {
        final Map<String, String> licenses =
                new ConcurrentHashMap<String, String>();
        for (final LicenseType type : LicenseType.values()) {
            type.putTextInto(licenses, tentaclesResources);
        }
        return licenses;
    }

    private final String resourceName;
    private final String resourcePath;

    private LicenseType(final String resourceName) {
        this.resourceName = resourceName;
        this.resourcePath = "licenses/" + getResourceName() + ".txt";
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String readText(final TentaclesResources tentaclesResources)
            throws IOException {
        return tentaclesResources.readText(getResourcePath()).trim();
    }

    public void putTextInto(final Map<String, String> licenseTextByName,
            final TentaclesResources tentaclesResources) throws IOException {
        licenseTextByName.put(getResourceName(), readText(tentaclesResources));
    }
}
