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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.io.ModelWriter;

/**
 * Attaches the model as a project artifact.
 */
@Mojo(
        name = "attach-slingfeature",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
    )
public class AttachSlingStartModel extends AbstractSlingStartMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Model model = ProjectHelper.getRawModel(this.project);
        if (usePomVariables) {
            model = ModelUtility.applyVariables(model, new PomVariableResolver(project));
        }
        if (usePomDependencies) {
            model = ModelUtility.applyArtifactVersions(model, new PomArtifactVersionResolver(project, allowUnresolvedPomDependencies));
        }

        // write the model
        final File outputFile = new File(this.project.getBuild().getDirectory() + File.separatorChar + BuildConstants.MODEL_ARTIFACT_NAME);
        outputFile.getParentFile().mkdirs();

        Writer writer = null;
        try {
            writer = new FileWriter(outputFile);
            ModelWriter.write(writer, model);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write model to " + outputFile, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

        // if this project is a partial bundle list, it's the main artifact
        if ( project.getPackaging().equals(BuildConstants.PACKAGING_PARTIAL_SYSTEM) ) {
            project.getArtifact().setFile(outputFile);
        } else {
            // otherwise attach it as an additional artifact
            projectHelper.attachArtifact(project, BuildConstants.PACKAGING_PARTIAL_SYSTEM,
                    BuildConstants.CLASSIFIER_PARTIAL_SYSTEM, outputFile);
        }
    }
}
