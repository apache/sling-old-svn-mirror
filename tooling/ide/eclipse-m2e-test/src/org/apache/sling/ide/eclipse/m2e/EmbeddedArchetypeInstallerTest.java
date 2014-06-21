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
package org.apache.sling.ide.eclipse.m2e;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmbeddedArchetypeInstallerTest {

    private final String archetypeGroupId = "org.apache.sling";
    private final String archetypeArtifactId = "sling-bundle-archetype";
    private final String archetypeVersion = "0.1.0-test-only";

    @Before
    @After
    public void deleteInstalledArtifact() throws CoreException, IOException {

        File artifactPathLocation = getInstalledArchetypeLocation();

        File artifactDirLocation = artifactPathLocation.getParentFile();

        FileUtils.deleteDirectory(artifactDirLocation);
    }

    @Test
    public void testInstallArchetype() throws IOException, CoreException {
        
        EmbeddedArchetypeInstaller archetypeInstaller = new EmbeddedArchetypeInstaller(archetypeGroupId,
                archetypeArtifactId, archetypeVersion);

        EmbeddedArtifact[] archetypeArtifacts = new EmbeddedArtifact[] {
                new EmbeddedArtifact("jar", archetypeVersion, getClass().getClassLoader().getResource(
                        "META-INF/MANIFEST.MF")),
                new EmbeddedArtifact("pom", archetypeVersion, getClass().getClassLoader().getResource(
                        "META-INF/MANIFEST.MF"))
        };

        archetypeInstaller.addResource("pom", archetypeArtifacts[0].openInputStream());
        archetypeInstaller.addResource("jar", archetypeArtifacts[1].openInputStream());

        archetypeInstaller.installArchetype();

        File artifactPathLocation = getInstalledArchetypeLocation();

        assertTrue("Archetype was not found at " + artifactPathLocation, artifactPathLocation.exists());
    }

    private File getInstalledArchetypeLocation() throws CoreException {

        IMaven maven = MavenPlugin.getMaven();

        String artifactPath = maven.getArtifactPath(maven.getLocalRepository(), archetypeGroupId, archetypeArtifactId,
                archetypeVersion, "jar", null);

        String localRepositoryPath = maven.getLocalRepositoryPath();

        return new File(localRepositoryPath + File.separatorChar + artifactPath);
    }
}
