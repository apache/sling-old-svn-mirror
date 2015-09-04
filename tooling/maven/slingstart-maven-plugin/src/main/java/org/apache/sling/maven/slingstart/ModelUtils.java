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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.util.Set;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;

public abstract class ModelUtils {


    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    public static Artifact getArtifact(final MavenProject project,
            final MavenSession session,
            final ArtifactHandlerManager artifactHandlerManager,
            final ArtifactResolver resolver,
            final String groupId, final String artifactId, final String version, final String type, final String classifier)
    throws MojoExecutionException {
        final Set<Artifact> artifacts = project.getDependencyArtifacts();
        for(final Artifact artifact : artifacts) {
            if ( artifact.getGroupId().equals(groupId)
               && artifact.getArtifactId().equals(artifactId)
               && artifact.getVersion().equals(version)
               && artifact.getType().equals(type)
               && ((classifier == null && artifact.getClassifier() == null) || (classifier != null && classifier.equals(artifact.getClassifier()))) ) {
                return artifact;
            }
        }
        final Artifact prjArtifact = new DefaultArtifact(groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                Artifact.SCOPE_PROVIDED,
                type,
                classifier,
                artifactHandlerManager.getArtifactHandler(type));
        try {
            resolver.resolve(prjArtifact, project.getRemoteArtifactRepositories(), session.getLocalRepository());
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to get artifact for " + groupId + ":" + artifactId + ":" + version, e);
        } catch (final ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to get artifact for " + groupId + ":" + artifactId + ":" + version, e);
        }
        return prjArtifact;
    }

    public static org.apache.sling.provisioning.model.Artifact findBaseArtifact(final Model model)
    throws MavenExecutionException {
        final Feature base = model.getFeature(ModelConstants.FEATURE_LAUNCHPAD);
        if ( base == null ) {
            throw new MavenExecutionException("No launchpad feature found.", (File)null);
        } else {
            // get global run mode
            final RunMode runMode = base.getRunMode();
            if ( runMode == null ) {
                throw new MavenExecutionException("No global run mode found in launchpad feature.", (File)null);
            } else {
                if ( runMode.getArtifactGroups().isEmpty() ) {
                    throw new MavenExecutionException("No base artifacts defined.", (File)null);
                } else if ( runMode.getArtifactGroups().size() > 1 ) {
                    throw new MavenExecutionException("Base run mode should only have a single start level.", (File)null);
                } else {
                    org.apache.sling.provisioning.model.Artifact firstArtifact = null;
                    for(final org.apache.sling.provisioning.model.Artifact a : runMode.getArtifactGroups().get(0)) {
                        if ( firstArtifact == null ) {
                            firstArtifact = a;
                        } else {
                            throw new MavenExecutionException("Base run mode should contain exactly one artifact: " + runMode.getArtifactGroups().get(0), (File)null);
                        }
                    }
                    if ( firstArtifact == null ) {
                        throw new MavenExecutionException("No base artifacts defined.", (File)null);
                    }
                    return firstArtifact;
                }
            }
        }
    }

    public static String toString(final Dependency d) {
        return "Dependency {groupId=" + d.getGroupId() + ", artifactId=" + d.getArtifactId() + ", version=" + d.getVersion() +
                (d.getClassifier() != null ? ", classifier=" + d.getClassifier() : "") +
                ", type=" + d.getType() + "}";
    }
}
