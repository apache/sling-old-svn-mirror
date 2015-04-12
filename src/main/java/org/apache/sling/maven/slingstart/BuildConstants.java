/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.slingstart;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.provisioning.model.ModelConstants;

public abstract class BuildConstants {

    // CONTEXTS
    public static final String CONTEXT_GLOBAL = "slingstart:global";
    public static final String CONTEXT_STANDALONE = "slingstart" + ModelConstants.RUN_MODE_STANDALONE;
    public static final String CONTEXT_WEBAPP = "slingstart" + ModelConstants.RUN_MODE_WEBAPP;

    // Model artifact name
    public static final String MODEL_ARTIFACT_NAME = "slingstart.txt";

    // Types

    public static final String TYPE_JAR = "jar";

    public static final String TYPE_WAR = "war";

    public static final String TYPE_POM = "pom";

    public static final String TYPE_TXT = "txt";

    public static final String PACKAGING_PARTIAL_SYSTEM = "slingfeature";

    public static final String PACKAGING_SLINGSTART = "slingstart";

    // Classifiers

    public static final String CLASSIFIER_PARTIAL_SYSTEM = "slingfeature";

    public static final String CLASSIFIER_BASE = "base";

    public static final String CLASSIFIER_APP = "app";

    public static final String CLASSIFIER_WEBAPP = "webapp";

    // Manifest attributes

    public static final String ATTR_BUILT_BY = "Built-By";

    public static final String ATTR_CREATED_BY = "Created-By";

    public static final String ATTR_IMPLEMENTATION_VERSION = "Implementation-Version";

    public static final String ATTR_IMPLEMENTATION_VENDOR = "Implementation-Vendor";

    public static final String ATTR_IMPLEMENTATION_BUILD = "Implementation-Build";

    public static final String ATTR_IMPLEMENTATION_VENDOR_ID = "Implementation-Vendor-Id";

    public static final String ATTR_IMPLEMENTATION_TITLE = "Implementation-Title";

    public static final String ATTR_SPECIFICATION_TITLE = "Specification-Title";

    public static final String ATTR_SPECIFICATION_VENDOR = "Specification-Vendor";

    public static final String ATTR_SPECIFICATION_VERSION = "Specification-Version";

    public static final String ATTR_MAIN_CLASS = "Main-Class";

    public static final String ATTR_VALUE_MAIN_CLASS = "org.apache.sling.launchpad.app.Main";

    public static final List<String> ATTRS_EXCLUDES = new ArrayList<String>();
    static {
        ATTRS_EXCLUDES.add(ATTR_BUILT_BY);
        ATTRS_EXCLUDES.add(ATTR_CREATED_BY);
        ATTRS_EXCLUDES.add(ATTR_IMPLEMENTATION_VERSION);
        ATTRS_EXCLUDES.add(ATTR_IMPLEMENTATION_VENDOR);
        ATTRS_EXCLUDES.add(ATTR_IMPLEMENTATION_BUILD);
        ATTRS_EXCLUDES.add(ATTR_IMPLEMENTATION_VENDOR_ID);
        ATTRS_EXCLUDES.add(ATTR_IMPLEMENTATION_TITLE);
        ATTRS_EXCLUDES.add(ATTR_SPECIFICATION_TITLE);
        ATTRS_EXCLUDES.add(ATTR_SPECIFICATION_VENDOR);
        ATTRS_EXCLUDES.add(ATTR_SPECIFICATION_VERSION);
    }

    // build constants
    public static final String WEBAPP_OUTDIR = "slingstart-webapp";
}
