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

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference.VersionResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * VersionResolver that reads version information from a sling provisioning file.
 * One use-case is to reference Sling's launchpad, which normally references a
 * (recent) set of compatible bundles, in order to allow running test based on
 * the versioning information from the Sling launchpad's provisioning model.
 */
public class ProvisioningModelVersionResolver implements VersionResolver {

    private final Model model;

    /**
     * Adds classifier "slingfeature" and type "txt" to the provided MavenArtifactUrlReference
     * to simplify creation of a VersionResolver based on a slingfeature.
     *
     * @param reference Maven coordinates of a module that provides a slingfeature.
     * @return VersionResolver instance backed by the referenced slingfeature.
     */
    public static VersionResolver fromSlingfeature(MavenArtifactUrlReference reference) {
        final String url = reference.classifier("slingfeature").type("txt").getURL();
        try {
            return new ProvisioningModelVersionResolver(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor to create a VersionResolver instance backed by a provisioning model referenced
     * by the URL represented by the provided String.
     *
     * @param url The String representation of a URL.
     * @throws MalformedURLException If the String representation of the URL is not a valid URL.
     */
    public ProvisioningModelVersionResolver(final String url) throws MalformedURLException {
        this(toUrl(url));
    }

    /**
     * Constructor to create a VersionResolver instance backed by a provisioning model referenced
     * by the provided URL object.
     *
     * @param url The URL pointing the the provisioning model file.
     */
    public ProvisioningModelVersionResolver(final URL url) {
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            this.model = ModelReader.read(new InputStreamReader(inputStream), url.toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + url.toExternalForm(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // silent
                }
            }
        }
    }

    private static URL toUrl(final String url) throws MalformedURLException {
        final boolean hasProtocolHandler = System.getProperty("java.protocol.handler.pkgs") != null;
        if (!hasProtocolHandler) {
            // enable org.ops4j.pax.url handlers by default, unless the property is already set
            System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        }
        try {
            return new URL(url);
        } catch (final MalformedURLException e) {
            if ("unknown protocol: mvn".equals(e.getMessage())) {
                // best effort: present a helpful message in case the mvn protocol handler is missing
                final MalformedURLException exception = new MalformedURLException(e.getMessage()
                        + " -> Consider a dependency to org.ops4j.pax.url:pax-url-aether");
                exception.initCause(e);
                throw exception;
            }
            throw e;
        } finally {
            if (!hasProtocolHandler) {
                System.clearProperty("java.protocol.handler.pkgs");
            }
        }
    }

    @Override
    public String getVersion(final String groupId, final String artifactId) {
        for (final Feature feature : model.getFeatures()) {
            for (final RunMode runMode : feature.getRunModes()) {
                for (final ArtifactGroup artifacts : runMode.getArtifactGroups()) {
                    for (final Artifact artifact : artifacts) {
                        if (groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId())) {
                            return artifact.getVersion();
                        }
                    }
                }
            }
        }
        return null;
    }
}
