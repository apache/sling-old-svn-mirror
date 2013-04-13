/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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
package org.apache.sling.launchpad.karaf;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.ops4j.pax.exam.util.Filter;

public abstract class KarafTestSupport {

    public static final String KARAF_GROUP_ID = "org.apache.karaf";

    public static final String KARAF_ARTIFACT_ID = "apache-karaf";

    public static final String KARAF_VERSION = "3.0.0.RC1";

    public static final String KARAF_NAME = "Apache Karaf";

    @Inject
    @Filter(timeout = 120000)
    BootFinished bootFinished;

    protected KarafTestSupport() {
    }

    public String karafGroupId() {
        return KARAF_GROUP_ID;
    }

    public String karafArtifactId() {
        return KARAF_ARTIFACT_ID;
    }

    public String karafVersion() {
        return KARAF_VERSION;
    }

    public String karafName() {
        return KARAF_NAME;
    }

}
