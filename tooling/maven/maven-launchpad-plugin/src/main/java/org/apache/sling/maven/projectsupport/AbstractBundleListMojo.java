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
package org.apache.sling.maven.projectsupport;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractBundleListMojo extends AbstractMojo {

    /**
     * Partial Bundle List type
     */
    protected static final String PARTIAL = "partialbundlelist";

    @Parameter(defaultValue = "${basedir}/src/main/bundles/list.xml")
    protected File bundleListFile;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    @Parameter(property = "configDirectory", defaultValue = "src/main/config")
    private File configDirectory;

    @Parameter(property = "commonSlingProps", defaultValue = "src/main/sling/common.properties")
    protected File commonSlingProps;

    @Parameter(property = "commonSlingBootstrap", defaultValue = "src/main/sling/common.bootstrap.txt")
    protected File commonSlingBootstrap;

    @Parameter(property = "webappSlingProps", defaultValue = "src/main/sling/webapp.properties")
    protected File webappSlingProps;

    @Parameter(property = "webappSlingBootstrap", defaultValue = "src/main/sling/webapp.bootstrap.txt")
    protected File webappSlingBootstrap;

    @Parameter(property = "standaloneSlingProps", defaultValue = "src/main/sling/standalone.properties")    
    protected File standaloneSlingProps;

    @Parameter(property = "standaloneSlingBootstrap", defaultValue = "src/main/sling/standalone.bootstrap.txt")
    protected File standaloneSlingBootstrap;

    @Parameter(property="ignoreBundleListConfig", defaultValue = "false")
    protected boolean ignoreBundleListConfig;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession mavenSession;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    private ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component(hint = "maven")
    private ArtifactMetadataSource metadataSource;

    /**
     * Location of the local repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository local;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepos;

    @Component
    private ArtifactResolver resolver;

    protected File getConfigDirectory() {
        return this.configDirectory;
    }

    /**
     * Get a resolved Artifact from the coordinates found in the artifact
     * definition.
     *
     * @param def the artifact definition
     * @return the artifact, which has been resolved
     * @throws MojoExecutionException
     */
    protected Artifact getArtifact(ArtifactDefinition def) throws MojoExecutionException {
        return getArtifact(def.getGroupId(), def.getArtifactId(), def.getVersion(), def.getType(), def.getClassifier());
    }

    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    protected Artifact getArtifact(String groupId, String artifactId, String version, String type, String classifier)
            throws MojoExecutionException {
                Artifact artifact;
                VersionRange vr;

                try {
                    vr = VersionRange.createFromVersionSpec(version);
                } catch (InvalidVersionSpecificationException e) {
                    vr = VersionRange.createFromVersion(version);
                }

                if (StringUtils.isEmpty(classifier)) {
                    artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type, null, Artifact.SCOPE_COMPILE);
                } else {
                    artifact = factory.createDependencyArtifact(groupId, artifactId, vr, type, classifier,
                            Artifact.SCOPE_COMPILE);
                }

                // This code kicks in when the version specifier is a range.
                if (vr.getRecommendedVersion() == null) {
                    try {
                        List<ArtifactVersion> availVersions = metadataSource.retrieveAvailableVersions(artifact, local, remoteRepos);
                        ArtifactVersion resolvedVersion = vr.matchVersion(availVersions);
                        artifact.setVersion(resolvedVersion.toString());
                    } catch (ArtifactMetadataRetrievalException e) {
                        throw new MojoExecutionException("Unable to find version for artifact", e);
                    }

                }

                try {
                    resolver.resolve(artifact, remoteRepos, local);
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException("Unable to resolve artifact.", e);
                } catch (ArtifactNotFoundException e) {
                    throw new MojoExecutionException("Unable to find artifact.", e);
                }
                return artifact;
            }

    /**
     * Helper method to copy a whole directory
     */
    protected void copyDirectory(final File source, final File target, final String[] includes, final String[] excludes)
    throws IOException {
        final String prefix = source.getAbsolutePath() + File.separatorChar;
        final int prefixLength = prefix.length();
        org.apache.commons.io.FileUtils.copyDirectory(source, target, new FileFilter() {

            public boolean accept(final File file) {
                final String path = file.getAbsolutePath().substring(prefixLength).replace(File.separatorChar, '/');
                if ( includes != null ) {
                    boolean matched = false;
                    for(int i = 0; i<includes.length && !matched; i++) {
                        if ( SelectorUtils.matchPath(includes[i], path)) {
                            matched = true;
                        }
                    }
                    if ( !matched ) {
                        return false;
                    }
                }
                if ( excludes != null ) {
                    for(final String pattern:excludes) {
                        if ( SelectorUtils.matchPath(pattern, path)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });
    }

}
