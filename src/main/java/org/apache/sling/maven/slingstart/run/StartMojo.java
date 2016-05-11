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
package org.apache.sling.maven.slingstart.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.maven.slingstart.BuildConstants;

/**
 * Mojo for starting launchpad.
 */
@Mojo(
        name = "start",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        threadSafe = true
    )
public class StartMojo extends AbstractMojo {

    /**
     * Set this to "true" to skip starting the launchpad
     *
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    protected boolean skipLaunchpad;

    /**
     * Parameter containing the list of server configurations
     */
    @Parameter
    private List<ServerConfiguration> servers;
    
    /**
     * Overwrites debug parameter of all server configurations (if set).
     * Attaches a debugger to the forked JVM. If set to {@code "true"}, the process will allow a debugger to connect on port 8000. 
     * If set to some other string, that string will be appended to the server's {@code vmOpts}, allowing you to configure arbitrary debugging options.
     */
    @Parameter(property = "launchpad.debug")
    protected String debug;

    /**
     * Ready timeout in seconds. If the launchpad has not been started in this
     * time, it's assumed that the startup failed.
     */
    @Parameter(property = "launchpad.ready.timeout", defaultValue = "600")
    private int launchpadReadyTimeOutSec;

    /**
     * The launchpad jar. This option has precedence over "launchpadDependency".
     */
    @Parameter(property = "launchpad.jar")
    private File launchpadJar;

    /**
     * The launchpad jar as a dependency. This is only used if "launchpadJar" is not
     * specified.
     */
    @Parameter
    private Dependency launchpadDependency;

    /**
     * Clean the working directory before start.
     */
    @Parameter(property = "launchpad.clean.workdir", defaultValue = "false")
    private boolean cleanWorkingDirectory;

    /**
     * Keep the launchpad running.
     */
    @Parameter(property = "launchpad.keep.running", defaultValue = "false")
    private boolean keepLaunchpadRunning;

    /**
     * Set the execution of launchpad instances to be run in parallel (threads)
     */
    @Parameter(property = "launchpad.parallelExecution", defaultValue = "true")
    private boolean parallelExecution;

    /**
     * The system properties file will contain all started instances with their ports etc.
     */
    @Parameter(defaultValue = "${project.build.directory}/launchpad-runner.properties")
    protected File systemPropertiesFile;

    /**
     * The Maven project.
     */
    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;

    /**
     * The Maven session.
     */
    @Parameter(property = "session", readonly = true, required = true)
    private MavenSession mavenSession;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * Get a resolved Artifact from the coordinates provided
     *
     * @return the artifact, which has been resolved.
     * @throws MojoExecutionException
     */
    private Artifact getArtifact(final Dependency d)
            throws MojoExecutionException {
        final Artifact prjArtifact = new DefaultArtifact(d.getGroupId(),
                        d.getArtifactId(),
                        VersionRange.createFromVersion(d.getVersion()),
                        d.getScope(),
                        d.getType(),
                        d.getClassifier(),
                        this.artifactHandlerManager.getArtifactHandler(d.getType()));
        try {
            this.resolver.resolve(prjArtifact, this.project.getRemoteArtifactRepositories(), this.mavenSession.getLocalRepository());
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to get artifact for " + d, e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to get artifact for " + d, e);
        }

        return prjArtifact;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipLaunchpad) {
            this.getLog().info("Executing of the start launchpad mojo is disabled by configuration.");
            return;
        }

        // delete properties
        if ( systemPropertiesFile != null && systemPropertiesFile.exists() ) {
            FileUtils.deleteQuietly(this.systemPropertiesFile);
        }

        // get configurations
        final Collection<ServerConfiguration> configurations = getLaunchpadConfigurations();

        // create the common environment
        final LaunchpadEnvironment env = new LaunchpadEnvironment(this.findLaunchpadJar(),
                this.cleanWorkingDirectory,
                !this.keepLaunchpadRunning,
                this.launchpadReadyTimeOutSec,
                this.debug);

        // create callables
        final Collection<LauncherCallable> tasks = new LinkedList<LauncherCallable>();

        for (final ServerConfiguration launchpadConfiguration : configurations) {
            validateConfiguration(launchpadConfiguration);

            tasks.add(createTask(launchpadConfiguration, env));
        }

        // create the launchpad runner properties
        this.createLaunchpadRunnerProperties(configurations);

        if (parallelExecution) {
            // ExecutorService for starting launchpad instances in parallel
            final ExecutorService executor = Executors.newCachedThreadPool();
            try {
                final List<Future<ProcessDescription>> resultsCollector = executor.invokeAll(tasks);
                for (final Future<ProcessDescription> future : resultsCollector) {
                    try {
                        if (null == future.get()) {
                            throw new MojoExecutionException("Cannot start all the instances");
                        }
                    } catch (final ExecutionException e) {
                        throw new MojoExecutionException(e.getLocalizedMessage(), e);
                    }
                }
            } catch ( final InterruptedException e) {
                throw new MojoExecutionException(e.getLocalizedMessage(), e);
            }
        } else {
            for (final LauncherCallable task : tasks) {
                try {
                    if (null == task.call()) {
                        throw new MojoExecutionException("Cannot start all the instances");
                    }
                } catch (final Exception e) {
                    throw new MojoExecutionException(e.getLocalizedMessage(), e);
                }
            }
        }
        if (this.keepLaunchpadRunning) {
            getLog().info("Press CTRL-C to stop launchpad instance(s)...");
            while ( true && this.isRunning(tasks)) {
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException ie) {
                    break;
                }
            }
        }
    }

    /**
     * Are all launchpads still running?
     */
    private boolean isRunning(final Collection<LauncherCallable> tasks) {
        for(final LauncherCallable task : tasks) {
            if ( !task.isRunning() ) {
                return false;
            }
        }
        return true;
    }

    private void createLaunchpadRunnerProperties(final Collection<ServerConfiguration> configurations)
    throws MojoExecutionException {
        // create properties
        OutputStream writer = null;
        final Properties props = new Properties();
        try {
            writer = new FileOutputStream(this.systemPropertiesFile);

            // disable sling startup check
            props.put("launchpad.skip.startupcheck", "true");

            // write out all instances
            int index = 0;
            for (final ServerConfiguration launchpadConfiguration : configurations) {
                index++;
                props.put("launchpad.instance.id." + String.valueOf(index), launchpadConfiguration.getId());
                String runMode = launchpadConfiguration.getRunmode();
                if ( runMode == null ) {
                    runMode = "";
                }
                props.put("launchpad.instance.runmode." + String.valueOf(index), runMode);
                props.put("launchpad.instance.server." + String.valueOf(index), launchpadConfiguration.getServer());
                props.put("launchpad.instance.port." + String.valueOf(index), launchpadConfiguration.getPort());
                props.put("launchpad.instance.contextPath." + String.valueOf(index), launchpadConfiguration.getContextPath());
                final String url = createServerUrl(launchpadConfiguration);
                props.put("launchpad.instance.url." + String.valueOf(index), url);
            }
            props.put("launchpad.instances", String.valueOf(index));

            props.store(writer, null);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getLocalizedMessage(), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private static String createServerUrl(final ServerConfiguration qc) {
        final StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(qc.getServer());
        if ( !qc.getPort().equals("80") ) {
            sb.append(':');
            sb.append(qc.getPort());
        }
        final String contextPath = qc.getContextPath();
        if ( contextPath != null && contextPath.trim().length() > 0 && !contextPath.equals("/") ) {
            if ( !contextPath.startsWith("/") ) {
                sb.append('/');
            }
            if ( contextPath.endsWith("/") ) {
                sb.append(contextPath, 0, contextPath.length()-1);
            } else {
                sb.append(contextPath);
            }
        }
        return sb.toString();
    }

    /**
     * @param launchpadConfiguration
     */
    private LauncherCallable createTask(final ServerConfiguration launchpadConfiguration,
                                               final LaunchpadEnvironment env)
    throws MojoExecutionException, MojoFailureException {
        final String id = launchpadConfiguration.getId();
        getLog().debug(new StringBuilder("Starting ").append(id).
                append(" with runmode ").append(launchpadConfiguration.getRunmode()).
                append(" on port ").append(launchpadConfiguration.getPort()).
                append(" in folder ").append(launchpadConfiguration.getFolder().getAbsolutePath()).toString());

        // create task
        return new LauncherCallable(this.getLog(), launchpadConfiguration, env);

    }

    /**
     * Validate a configuration
     * @param launchpadConfiguration The launchpad configuration
     * @throws MojoExecutionException
     */
    private void validateConfiguration(final ServerConfiguration launchpadConfiguration)
    throws MojoExecutionException {
        if ( launchpadConfiguration.getPort() == null ) {
            launchpadConfiguration.setPort(String.valueOf(PortHelper.getNextAvailablePort()));
        }

        if ( launchpadConfiguration.getControlPort() == null ) {
            launchpadConfiguration.setControlPort(String.valueOf(PortHelper.getNextAvailablePort()));
        }

        // set the id of the launchpad
        if ( launchpadConfiguration.getId() == null || launchpadConfiguration.getId().trim().length() == 0 ) {
            String runMode = launchpadConfiguration.getRunmode();
            if ( runMode == null ) {
                runMode = "_";
            }
            final String id = new StringBuilder(runMode.replace(',', '_')).append('-').append(launchpadConfiguration.getPort()).toString();
            launchpadConfiguration.setId(id);
        }

        // populate folder if not set
        if (launchpadConfiguration.getFolder() == null) {
            final File folder = new File(new StringBuilder(this.project.getBuild().getDirectory()).append('/').append(launchpadConfiguration.getId()).toString());
            launchpadConfiguration.setFolder(folder);
        }
        // context path should not be null
        if ( launchpadConfiguration.getContextPath() == null ) {
            launchpadConfiguration.setContextPath("");
        }

        if ( launchpadConfiguration.getInstances() < 0 ) {
            launchpadConfiguration.setInstances(1);
        }
    }

    /**
     * Finds the launchpad.jar artifact of the project being built.
     *
     * @return the launchpad.jar artifact
     * @throws MojoFailureException if a launchpad.jar artifact was not found
     */
    private File findLaunchpadJar() throws MojoFailureException, MojoExecutionException {

        // If a launchpad JAR is specified, use it
        if (launchpadJar != null) {
            return launchpadJar;
        }

        // If a launchpad dependency is configured, resolve it
        if (launchpadDependency != null) {
            return getArtifact(launchpadDependency).getFile();
        }

        // If the current project is a slingstart project, use its JAR artifact
        if (this.project.getPackaging().equals(BuildConstants.PACKAGING_SLINGSTART)) {
            final File jarFile = new File(this.project.getBuild().getDirectory(), this.project.getBuild().getFinalName() + ".jar");
            if (jarFile.exists()) {
                return jarFile;
            }
        }

        // Last chance: use the first declared dependency with type "slingstart"
        final Set<Artifact> dependencies = this.project.getDependencyArtifacts();
        for (final Artifact dep : dependencies) {
            if (BuildConstants.PACKAGING_SLINGSTART.equals(dep.getType())) {
                final Dependency d = new Dependency();
                d.setGroupId(dep.getGroupId());
                d.setArtifactId(dep.getArtifactId());
                d.setVersion(dep.getVersion());
                d.setScope(Artifact.SCOPE_RUNTIME);
                d.setType(BuildConstants.TYPE_JAR);
                return getArtifact(d).getFile();
            }
        }

        // Launchpad has not been found, throw an exception
        throw new MojoFailureException("Could not find the launchpad jar. " +
                "Either specify the 'launchpadJar' configuration or use this inside a slingstart project.");
    }

    /**
     * Get all configurations
     * @return Collection of configurations.
     */
    private Collection<ServerConfiguration> getLaunchpadConfigurations() {
        final List<ServerConfiguration> configs = new ArrayList<ServerConfiguration>();
        if ( this.servers != null && !this.servers.isEmpty() ) {
            for(final ServerConfiguration config : this.servers) {
                // if instances is set to 0, no instance is added
                if ( config.getInstances() != 0 ) {
                    configs.add(config);
                    for(int i=2; i<=config.getInstances();i++) {
                        final ServerConfiguration replicaConfig = config.copy();
                        replicaConfig.setPort(null);
                        final File folder = replicaConfig.getFolder();
                        if ( folder != null ) {
                            replicaConfig.setFolder(new File(folder.getParentFile(), folder.getName() + '-' + String.valueOf(i)));
                        }
                        configs.add(replicaConfig);
                    }
                    config.setInstances(1);
                }
            }
        } else {
            // use single default instance
            configs.add(new ServerConfiguration());
        }
        return configs;
    }
}
