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
package org.apache.sling.ide.artifacts.impl;

import java.net.URL;

import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class ArtifactsLocatorImpl implements EmbeddedArtifactLocator {

    private static final String ARTIFACTS_LOCATION = "target/artifacts";

    private ComponentContext context;

    protected void activate(ComponentContext context) {

        this.context = context;

    }

    @Override
    public EmbeddedArtifact loadToolingSupportBundle() {

        BundleContext bundleContext = context.getBundleContext();

        String version = "1.0.0"; // TODO - remove version hardcoding
        String artifactId = "org.apache.sling.tooling.support.install";
        String extension = "jar";

        URL jarUrl = loadResource(bundleContext, ARTIFACTS_LOCATION + "/sling-tooling-support-install/" + artifactId
                + "." + extension);

        return new EmbeddedArtifact(artifactId + "-" + version + "." + extension, version, jarUrl);
    }

    private URL loadResource(BundleContext bundleContext, String resourceLocation) {

        URL resourceUrl = bundleContext.getBundle().getResource(resourceLocation);
        if (resourceUrl == null) {
            throw new RuntimeException("Unable to locate bundle resource " + resourceLocation);
        }
        return resourceUrl;
    }

}
