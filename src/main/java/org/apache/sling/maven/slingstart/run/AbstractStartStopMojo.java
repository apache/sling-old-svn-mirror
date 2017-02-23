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
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

public abstract class AbstractStartStopMojo extends AbstractMojo {

    /**
     * Set this to "true" to skip starting the launchpad
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    protected boolean skipLaunchpad;

    /**
     * Parameter containing the list of server configurations
     */
    @Parameter
    protected List<ServerConfiguration> servers;

    /**
     * The system properties file will contain all started instances with their ports etc.
     */
    @Parameter(defaultValue = "${project.build.directory}/launchpad-runner.properties")
    protected File systemPropertiesFile;

    /**
     * If {@code true} this mojo blocks until you press the Enter key.
     */
    @Parameter
    protected boolean shouldBlockUntilKeyIsPressed;

    @Component
    private Prompter prompter;
    
    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipLaunchpad) {
            this.getLog().info("Executing of this mojo is disabled by configuration.");
            return;
        }
        
        doExecute();
    }

    protected void blockIfNecessary() throws MojoFailureException {
        if (shouldBlockUntilKeyIsPressed) {
            // http://stackoverflow.com/a/21977269/5155923
            try {
                prompter.prompt("Press Enter to continue");
            } catch (PrompterException e) {
                throw new MojoFailureException("Could not prompt for user input. Maven is probably running in non-interactive mode! Do not use parameter 'shouldBlockUntilKeyIsPressed' in that case", e);
            }
        }

    }
}
