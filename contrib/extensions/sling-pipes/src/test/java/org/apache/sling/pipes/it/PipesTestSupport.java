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
package org.apache.sling.pipes.it;

import javax.inject.Inject;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.pipes.Plumber;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;

import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionDistribution;
import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionEvent;
import static org.apache.sling.testing.paxexam.SlingOptions.slingExtensionQuery;
import static org.apache.sling.testing.paxexam.SlingOptions.slingLaunchpadOakTar;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public abstract class PipesTestSupport extends TestSupport {

    protected static final String NN_TEST = "test";

    @Inject
    @Filter(timeout = 3000000)
    protected Plumber plumber;

    @Inject
    private ResourceResolverFactory resourceResolverFactory;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            launchpad(),
            // Sling Pipes
            testBundle("bundle.filename"),
            // testing
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                .asOption(),
            junitBundles()
        };
    }

    protected void mkdir(ResourceResolver resolver, String path) throws Exception {
        plumber.newPipe(resolver).mkdir(path).run();
    }

    protected Option launchpad() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingLaunchpadOakTar(workingDirectory, httpPort),
            slingExtensionEvent(),
            slingExtensionDistribution(),
            slingExtensionQuery(),
            // TODO remove johnzon bundle (should be part of sling in upcoming release of org.apache.sling.testing.paxexam)
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.johnzon").version("1.1.0"),
            // TODO required by Jackrabbit Vault (Sling Distribution)
            systemPackages(
                "org.w3c.dom.css",
                "org.w3c.dom.html",
                "org.w3c.dom.ranges",
                "org.w3c.dom.traversal"
            )
        );
    }

    ResourceResolver resolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

}
