/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl.helpers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;

public class OsgiBundleManifest {

    public static OsgiBundleManifest symbolicName(String symbolicName) {

        return new OsgiBundleManifest(symbolicName);
    }

    private final Map<String, String> attributes = new LinkedHashMap<>();

    public OsgiBundleManifest(String symbolicName) {
        attributes.put(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        attributes.put("Bundle-ManifestVersion", "2");
        attributes.put("Bundle-SymbolicName", symbolicName);
    }

    public OsgiBundleManifest version(String version) {
        attributes.put("Bundle-Version", version);

        return this;
    }

    public OsgiBundleManifest name(String name) {
        attributes.put("Bundle-Name", name);

        return this;
    }

    public OsgiBundleManifest importPackage(String importPackage) {
        attributes.put("Import-Package", importPackage);

        return this;
    }

    public OsgiBundleManifest serviceComponent(String serviceComponent) {
        attributes.put("Service-Component", serviceComponent);

        return this;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
