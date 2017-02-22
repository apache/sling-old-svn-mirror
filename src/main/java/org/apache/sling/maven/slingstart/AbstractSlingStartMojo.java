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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;

/**
 * Base class for all slingstart mojos.
 */
public abstract class AbstractSlingStartMojo extends AbstractMojo {

    /**
     * The model directory containing the provision models.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     * As default first <code>${basedir}/src/main/provisioning</code> and then
     * <code>${basedir}/src/test/provisioning</code> is used 
     * (in case the former does not exist).
     * <p>
     * This parameter is evaluated in the {@link ModelPreprocessor}, i.e. outside of the Mojo execution therefore it must not be 
     * configured within an <a href="https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_executions_Tag">execution tag</a>
     * but rather in the global configuration section for this plugin.
     * </p>
     */
    @Parameter(defaultValue="${basedir}/src/main/provisioning")
    private File modelDirectory;

    /**
     * The model file name pattern to consider.
     * <p>
     * This parameter is evaluated in the {@link ModelPreprocessor}, i.e. outside of the Mojo execution therefore it must not be 
     * configured within an <a href="https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_executions_Tag">execution tag</a>
     * but rather in the global configuration section for this plugin.
     * </p>
     */
    @Parameter(defaultValue=DEFAULT_MODEL_PATTERN)
    private String modelPattern;
    
    public static final String DEFAULT_MODEL_PATTERN = "((.*)\\.txt|(.*)\\.model)";

    /**
     * Inlined model. Is processed first and afterwards merged with any model found in {@link #modelDirectory}.
     * <p>
     * This parameter is evaluated in the {@link ModelPreprocessor}, i.e. outside of the Mojo execution therefore it must not be 
     * configured within an <a href="https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_executions_Tag">execution tag</a>
     * but rather in the global configuration section for this plugin.
     * </p>
     * @since 1.3
     * @see <a href="https://issues.apache.org/jira/browse/SLING-4912">SLING-4912</a>
     */
    @Parameter
    private String model;
    
    static final String CONFIGURATION_NAME_DISABLE_EXTENDING_CLASSPATH = "disableExtendingMavenClasspath";
    /**
     * If set to {@code true} the Maven classpath (either scope "provided" or "test") will not be extended by the artifacts being referenced in the model.
     * <p>
     * This parameter is evaluated in the {@link ModelPreprocessor}, i.e. outside of the Mojo execution therefore it must not be 
     * configured within an <a href="https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_executions_Tag">execution tag</a>
     * but rather in the global configuration section for this plugin.
     * </p>
     */
    @Parameter(name=CONFIGURATION_NAME_DISABLE_EXTENDING_CLASSPATH)
    private boolean disableExtendingMavenClasspath;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession mavenSession;

    /**
     * If set to true, properties from the Maven POM can be used as variables in the provisioning files.
     * The resolved variables are added to the generated provisioning file, so other tools using this model
     * do not have to resolve them themselves.
     */
    @Parameter(defaultValue="false")
    protected boolean usePomVariables;

    /**
     * If set to true, Artifact dependencies from provisioning file without explicit version are tried
     * to be resolved against the dependency versions from the Maven POM.
     */
    @Parameter(defaultValue="false")
    protected boolean usePomDependencies;

    /**
     * If set to true, an exception is throws when "usePomDependencies" is set to true and some
     * dependency version could not be resolved in the Maven POM.
     */
    @Parameter(defaultValue="false")
    protected boolean allowUnresolvedPomDependencies;

    protected File getTmpDir() {
        return new File(this.project.getBuild().getDirectory(), "slingstart-tmp");
    }

    /**
     * @return Resolving options to be used when building an effective provisioning model.
     */
    protected ResolverOptions getResolverOptions() {
        ResolverOptions options = new ResolverOptions();
        if (usePomVariables) {
            options.variableResolver(new PomVariableResolver(project));
        }
        if (usePomDependencies) {
            options.artifactVersionResolver(new PomArtifactVersionResolver(project, allowUnresolvedPomDependencies));
        }
        return options;
    }

    protected File getStandaloneOutputDirectory() {
        return new File(this.getTmpDir(), "standalone");
    }
}
