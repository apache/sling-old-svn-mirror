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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Prepares the project
 *
 */
@Mojo(
        name = "prepare",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class PrepareSlingStartMojo extends AbstractSlingStartMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ModelUtils.prepareModel(this.project, this.mavenSession);

        if ( project.getPackaging().equals(BuildConstants.PACKAGING_SLINGSTART ) ) {
            // add dependencies for base artifact
            final ModelUtils.SearchResult result = ModelUtils.findBaseArtifact(ModelUtils.getEffectiveModel(project));
            if ( result.artifact != null ) {
                final String[] classifiers = new String[] {null, BuildConstants.CLASSIFIER_APP, BuildConstants.CLASSIFIER_WEBAPP};
                for(final String c : classifiers) {
                    final Dependency dep = new Dependency();
                    dep.setGroupId(result.artifact.getGroupId());
                    dep.setArtifactId(result.artifact.getArtifactId());
                    dep.setVersion(result.artifact.getVersion());
                    dep.setType(result.artifact.getType());
                    dep.setClassifier(c);
                    if ( BuildConstants.CLASSIFIER_WEBAPP.equals(c) ) {
                        dep.setType(BuildConstants.TYPE_WAR);
                    }
                    dep.setScope(Artifact.SCOPE_PROVIDED);

                    getLog().debug("Adding base dependency " + dep);
                    project.getDependencies().add(dep);
                }
            } else {
                throw new MojoExecutionException(result.errorMessage);
            }
        }
    }
}
