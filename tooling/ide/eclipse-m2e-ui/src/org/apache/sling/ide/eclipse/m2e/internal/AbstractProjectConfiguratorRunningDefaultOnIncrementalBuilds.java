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
package org.apache.sling.ide.eclipse.m2e.internal;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

/**
 * All configurators derived from this class, will optionally execute the default build action during incremental builds. This is
 * helpful for all configurators which are bound to goals of plugins, which already support 
 * <a href="https://www.eclipse.org/m2e/documentation/m2e-execution-not-covered.html#execute-plugin-goal">m2e incremental builds</a>.
 * 
 * Since the project configurator action overwrites all other actions bound to the same goal (defined in the same or
 * other lifecycle-mapping-metadata.xml) the goal would otherwise not work in incremental builds.
 * 
 * @see <a href="https://www.eclipse.org/m2e/documentation/m2e-execution-not-covered.html">Execution Not Covered</a>
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=506357">Bug 506357, Allow to merge lifecycle mapping from extension and plugin</a>
 */
public abstract class AbstractProjectConfiguratorRunningDefaultOnIncrementalBuilds
        extends AbstractProjectConfigurator {
    private final boolean runOnIncremental;

    public AbstractProjectConfiguratorRunningDefaultOnIncrementalBuilds(boolean runOnIncremental) {
        this.runOnIncremental = runOnIncremental;
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
            IPluginExecutionMetadata executionMetadata) {
        if (runOnIncremental) {
            // execute the default action also for incremental builds
            return new MojoExecutionBuildParticipant(execution, true, false);
        } else {
            return super.getBuildParticipant(projectFacade, execution, executionMetadata);
        }
    }

}
