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
package org.apache.sling.testing.paxexam;

import org.junit.Test;
import org.ops4j.pax.exam.options.MavenUrlReference.VersionResolver;

import static org.apache.sling.testing.paxexam.ProvisioningModelVersionResolver.fromSlingfeature;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.maven;

public class ProvisioningModelVersionResolverTest {

    @Test
    public void getVersionFromClasspathResource() throws Exception {
        final VersionResolver versionResolver =
                new ProvisioningModelVersionResolver(getClass().getResource("/test-dependencies.txt"));
        assertVersion("2.6.4", "org.apache.sling", "org.apache.sling.engine", versionResolver);
        assertVersion("2.4.10", "org.apache.sling", "org.apache.sling.servlets.resolver", versionResolver);
        assertVersion("2.1.18", "org.apache.sling", "org.apache.sling.servlets.get", versionResolver);
        assertVersion("2.3.14", "org.apache.sling", "org.apache.sling.servlets.post", versionResolver);
    }

    @Test
    public void getVersionFromMavenDependency() throws Exception {
        final VersionResolver versionResolver =
                fromSlingfeature(maven("org.apache.sling", "org.apache.sling.launchpad", "8"));
        assertVersion("2.4.4", "org.apache.sling", "org.apache.sling.engine", versionResolver);
        assertVersion("2.3.8", "org.apache.sling", "org.apache.sling.servlets.resolver", versionResolver);
        assertVersion("2.1.12", "org.apache.sling", "org.apache.sling.servlets.get", versionResolver);
        assertVersion("2.3.8", "org.apache.sling", "org.apache.sling.servlets.post", versionResolver);
    }

    private void assertVersion(final String expectedVersion,
                               final String groupId, final String artifactId, final VersionResolver versionResolver) {
        final String actualVersion = versionResolver.getVersion(groupId, artifactId);
        assertEquals("Version mismatch for " + groupId + ":" + artifactId, expectedVersion, actualVersion);
    }
}