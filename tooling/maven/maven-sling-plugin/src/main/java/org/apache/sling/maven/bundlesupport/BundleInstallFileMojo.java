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

package org.apache.sling.maven.bundlesupport;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Install an OSGi bundle to a running Sling instance.
 */
@Mojo(name = "install-file", requiresProject = false)
public class BundleInstallFileMojo extends AbstractBundleInstallMojo {

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property="sling.file")
    private String bundleFileName;

    /**
     * The groupId of the artifact to install
     */
    @Parameter(property="sling.groupId")
    private String groupId;

    /**
     * The artifactId of the artifact to install
     */
    @Parameter(property="sling.artifactId")
    private String artifactId;

    /**
     * The version of the artifact to install
     */
    @Parameter(property="sling.version")
    private String version;

    /**
     * The packaging of the artifact to install
     */
    @Parameter(property="sling.packaging", defaultValue="jar")
    private String packaging = "jar";

    /**
     * The classifier of the artifact to install
     */
    @Parameter(property="sling.classifier")
    private String classifier;

    /**
     * A string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter(property="sling.artifact")
    private String artifact;

    @Parameter(property="project.remoteArtifactRepositories", required = true, readonly = true)
    private List pomRemoteRepositories;

    /**
     * The id of the repository from which we'll download the artifact
     */
    @Parameter(property = "sling.repoId", defaultValue = "temp")
    private String repositoryId = "temp";

    /**
     * The url of the repository from which we'll download the artifact
     */
    @Parameter(property = "sling.repoUrl")
    private String repositoryUrl;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    @Component(hint="default")
    private ArtifactRepositoryLayout repositoryLayout;

    @Parameter(property="localRepository", readonly = true)
    private ArtifactRepository localRepository;

    @Override
    protected String getBundleFileName() throws MojoExecutionException {
        String fileName = bundleFileName;
        if (fileName == null) {
            fileName = resolveBundleFileFromArtifact();

            if (fileName == null) {
                throw new MojoExecutionException("Must provide either sling.file or sling.artifact parameters");
            }
        }

        return fileName;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String resolveBundleFileFromArtifact() throws MojoExecutionException {
        if (artifactId == null && artifact == null) {
            return null;
        }
        if (artifactId == null) {
            String[] tokens = StringUtils.split(artifact, ":");
            if (tokens.length != 3 && tokens.length != 4 && tokens.length != 5) {
                throw new MojoExecutionException("Invalid artifact, you must specify "
                        + "groupId:artifactId:version[:packaging[:classifier]] " + artifact);
            }
            groupId = tokens[0];
            artifactId = tokens[1];
            version = tokens[2];
            if (tokens.length >= 4)
                packaging = tokens[3];
            if (tokens.length == 5)
                classifier = tokens[4];
        }
        Artifact packageArtifact = artifactFactory.createArtifactWithClassifier(groupId, artifactId, version, packaging, classifier);

        if (pomRemoteRepositories == null) {
            pomRemoteRepositories = new ArrayList();
        }

        List repoList = new ArrayList(pomRemoteRepositories);

        if (repositoryUrl != null) {
            ArtifactRepositoryPolicy policy =
                new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                              ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
            ArtifactRepository remoteRepo = artifactRepositoryFactory.createArtifactRepository(repositoryId, repositoryUrl,
                    repositoryLayout, policy, policy);

            repoList.add(remoteRepo);
        }

        try {
            artifactResolver.resolve(packageArtifact, repoList, localRepository);
            getLog().info("Resolved artifact to " + packageArtifact.getFile().getAbsolutePath());
        } catch (AbstractArtifactResolutionException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }

        return packageArtifact.getFile().getAbsolutePath();
    }
}