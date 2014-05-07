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

import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.test.impl.Activator;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.junit.rules.ExternalResource;

/**
 * The <tt>ToolingSupportBundle</tt> rules ensures that the tooling support bundle is installed
 *
 */
public class ToolingSupportBundle extends ExternalResource {

    @Override
    protected void before() throws Throwable {

        EmbeddedArtifactLocator locator = Activator.getDefault().getArtifactLocator();
        EmbeddedArtifact toolingBundle = locator.loadToolingSupportBundle();

        OsgiClientFactory clientFactory = Activator.getDefault().getOsgiClientFactory();
        OsgiClient osgiClient = clientFactory.createOsgiClient(new RepositoryInfo("admin", "admin", "http://localhost:"
                + LaunchpadUtils.getLaunchpadPort() + "/"));
        osgiClient.installBundle(toolingBundle.openInputStream(), toolingBundle.getName());
    }
}
