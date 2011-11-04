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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class AbstractBundleListMojo extends AbstractMojo {

    /**
     * Partial Bundle List type
     */
    protected static final String PARTIAL = "partialbundlelist";

    /**
     * @parameter default-value="${basedir}/src/main/bundles/list.xml"
     */
    protected File bundleListFile;

    /**
     * @parameter
     */
    private ConfigurationStartLevel[] includeDependencies;

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${configDirectory}"
     *            default-value="src/main/config"
     */
    private File configDirectory;

    /**
     * @parameter expression="${commonSlingProps}"
     *            default-value="src/main/sling/common.properties"
     */
    protected File commonSlingProps;

    /**
     * @parameter expression="${commonSlingBootstrap}"
     *            default-value="src/main/sling/common.bootstrap.txt"
     */
    protected File commonSlingBootstrap;

    /**
     * @parameter expression="${webappSlingProps}"
     *            default-value="src/main/sling/webapp.properties"
     */
    protected File webappSlingProps;

    /**
     * @parameter expression="${webappSlingBootstrap}"
     *            default-value="src/main/sling/webapp.bootstrap.txt"
     */
    protected File webappSlingBootstrap;

    /**
     * @parameter expression="${standaloneSlingProps}"
     *            default-value="src/main/sling/standalone.properties"
     */
    protected File standaloneSlingProps;

    /**
     * @parameter expression="${standaloneSlingBootstrap}"
     *            default-value="src/main/sling/standalone.bootstrap.txt"
     */
    protected File standaloneSlingBootstrap;

    /**
     * @parameter expression="${ignoreBundleListConfig}"
     *            default-value="false"
     */
    protected boolean ignoreBundleListConfig;

    /**
     * @parameter expression="${session}
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * The start level to be used when generating the bundle list.
     * 
     * @parameter default-value="-1"
     */
    private int dependencyStartLevel;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component hint="maven"
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List<?> remoteRepos;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    private ArtifactResolver resolver;

    protected File getConfigDirectory() {
        return this.configDirectory;
    }

    protected BundleList readBundleList(File file) throws IOException, XmlPullParserException {
        BundleListXpp3Reader reader = new BundleListXpp3Reader();
        FileInputStream fis = new FileInputStream(file);
        try {
            return reader.read(fis);
        } finally {
            fis.close();
        }
    }

    @SuppressWarnings("unchecked")
    protected void addDependencies(final BundleList bundleList) throws MojoExecutionException {
        if (includeDependencies != null) {
            for (ConfigurationStartLevel startLevel : includeDependencies) {
                Set<Artifact> artifacts = getArtifacts(startLevel);
                for (Artifact artifact : artifacts) {
                    bundleList.add(ArtifactDefinition.toBundle(artifact, startLevel.getLevel()));
                }
            }
        }

        if (dependencyStartLevel >= 0) {
            final List<Dependency> dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
                if (!PARTIAL.equals(dependency.getType())) {
                    bundleList.add(ArtifactDefinition.toBundle(dependency, dependencyStartLevel));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Artifact> getArtifacts(ConfigurationStartLevel startLevel) throws MojoExecutionException {
        // start with all artifacts.
        Set<Artifact> artifacts = project.getArtifacts();

        // perform filtering
        try {
            artifacts = startLevel.buildFilter(project).filter(artifacts);
        } catch (ArtifactFilterException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        
        return artifacts;
    }

    protected void interpolateProperties(BundleList bundleList) throws MojoExecutionException {
        Interpolator interpolator = createInterpolator();
        for (final StartLevel sl : bundleList.getStartLevels()) {
            for (final Bundle bndl : sl.getBundles()) {
                try {
                    bndl.setArtifactId(interpolator.interpolate(bndl.getArtifactId()));
                    bndl.setGroupId(interpolator.interpolate(bndl.getGroupId()));
                    bndl.setVersion(interpolator.interpolate(bndl.getVersion()));
                    bndl.setClassifier(interpolator.interpolate(bndl.getClassifier()));
                    bndl.setType(interpolator.interpolate(bndl.getType()));
                } catch (InterpolationException e) {
                    throw new MojoExecutionException("Unable to interpolate properties for bundle " + bndl.toString(), e);
                }
            }
        }

    }

    private Interpolator createInterpolator() {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();

        final Properties props = new Properties();
        props.putAll(project.getProperties());
        props.putAll(mavenSession.getExecutionProperties());

        interpolator.addValueSource(new PropertiesBasedValueSource(props));

        // add ${project.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource(Arrays.asList("project", "pom"), project, true));

        // add ${session.foo}
        interpolator.addValueSource(new PrefixedObjectValueSource("session", mavenSession));

        // add ${settings.foo}
        final Settings settings = mavenSession.getSettings();
        if (settings != null) {
            interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        }

        return interpolator;
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
                        List<?> availVersions = metadataSource.retrieveAvailableVersions(artifact, local, remoteRepos);
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

}
