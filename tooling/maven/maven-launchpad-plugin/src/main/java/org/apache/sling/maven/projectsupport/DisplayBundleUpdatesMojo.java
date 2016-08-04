/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.maven.projectsupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.settings.Settings;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.DefaultVersionsHelper;
import org.codehaus.mojo.versions.api.UpdateScope;
import org.codehaus.mojo.versions.api.VersionsHelper;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Displays all bundles that have newer versions available. Highly based upon the
 * display-dependency-updates goal from the Versions plugin.
 *
 * @since 2.0.8
 *
 */
@Mojo(name = "display-bundle-updates")
public class DisplayBundleUpdatesMojo extends AbstractMojo {

    /**
     * The width to pad info messages.
     */
    private static final int INFO_PAD_SIZE = 72;

    @Component
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     */
    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    private List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter( defaultValue = "${project.pluginArtifactRepositories}", readonly = true)
    private List<ArtifactRepository> remotePluginRepositories;

    @Parameter( defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Component
    private WagonManager wagonManager;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    /**
     * settings.xml's server id for the URL. This is used when wagon needs extra
     * authentication information.
     */
    @Parameter(property = "maven.version.rules.serverId", defaultValue = "serverId")
    private String serverId;

    /**
     * The Wagon URI of a ruleSet file containing the rules that control how to
     * compare version numbers.
     */
    @Parameter(defaultValue = "maven.version.rules")
    private String rulesUri;

    /**
     * The Maven Session.
     */
    @Parameter(defaultValue = "${session}", required = true)
    private MavenSession session;

    @Component
    private PathTranslator pathTranslator;

    @Parameter(defaultValue = "${basedir}/src/main/bundles/list.xml")
    private File bundleListFile;

    /**
     * Whether to allow snapshots when searching for the latest version of an
     * artifact.
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false")
    private boolean allowSnapshots;

    /**
     * Our versions helper.
     */
    private VersionsHelper helper;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            BundleList bundleList = readBundleList(bundleListFile);

            Set<Dependency> bundlesAsDependencies = new HashSet<Dependency>();

            for (StartLevel startLevel : bundleList.getStartLevels()) {
                for (Bundle bundle : startLevel.getBundles()) {
                    bundlesAsDependencies.add(asDependency(bundle));
                }
            }

            logUpdates(getHelper().lookupDependenciesUpdates(bundlesAsDependencies, false));
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to read bundle list.", e);
        }

    }

    private Dependency asDependency(Bundle bundle) {
        Dependency dep = new Dependency();
        dep.setGroupId(bundle.getGroupId());
        dep.setArtifactId(bundle.getArtifactId());
        dep.setClassifier(bundle.getClassifier());
        dep.setVersion(bundle.getVersion());
        dep.setType(bundle.getType());

        return dep;
    }

    private VersionsHelper getHelper() throws MojoExecutionException {
        if (helper == null) {
            helper = new DefaultVersionsHelper(artifactFactory, artifactMetadataSource, remoteArtifactRepositories,
                    remotePluginRepositories, localRepository, wagonManager, settings, serverId, rulesUri, getLog(),
                    session, pathTranslator);
        }
        return helper;
    }

    private void logUpdates(Map<Dependency, ArtifactVersions> updates) {
        List<String> withUpdates = new ArrayList<String>();
        List<String> usingCurrent = new ArrayList<String>();
        for (ArtifactVersions versions : updates.values()) {
            String left = "  " + ArtifactUtils.versionlessKey(versions.getArtifact()) + " ";
            final String current = versions.isCurrentVersionDefined() ? versions.getCurrentVersion().toString()
                    : versions.getArtifact().getVersionRange().toString();
            ArtifactVersion latest = versions.getNewestUpdate(UpdateScope.ANY, Boolean.TRUE.equals(allowSnapshots));
            if (latest != null && !versions.isCurrentVersionDefined()) {
                if (versions.getArtifact().getVersionRange().containsVersion(latest)) {
                    latest = null;
                }
            }
            String right = " " + (latest == null ? current : current + " -> " + latest.toString());
            List<String> t = latest == null ? usingCurrent : withUpdates;
            if (right.length() + left.length() + 3 > INFO_PAD_SIZE) {
                t.add(left + "...");
                t.add(StringUtils.leftPad(right, INFO_PAD_SIZE));

            } else {
                t.add(StringUtils.rightPad(left, INFO_PAD_SIZE - right.length(), ".") + right);
            }
        }

        if (usingCurrent.isEmpty() && !withUpdates.isEmpty()) {
            getLog().info("No bundles are using the newest version.");
            getLog().info("");
        } else if (!usingCurrent.isEmpty()) {
            getLog().info("The following bundles are using the newest version:");
            for (String str : usingCurrent) {
                getLog().info(str);
            }
            getLog().info("");
        }
        if (withUpdates.isEmpty() && !usingCurrent.isEmpty()) {
            getLog().info("No bundles have newer versions.");
            getLog().info("");
        } else if (!withUpdates.isEmpty()) {
            getLog().info("The following bundles have newer versions:");
            for (String str : withUpdates) {
                getLog().info(str);
            }
            getLog().info("");
        }
    }

    private BundleList readBundleList(File file) throws IOException, XmlPullParserException {
        BundleListXpp3Reader reader = new BundleListXpp3Reader();
        FileInputStream fis = new FileInputStream(file);
        try {
            return reader.read(fis);
        } finally {
            fis.close();
        }
    }

}
