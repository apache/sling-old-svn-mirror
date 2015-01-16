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
package org.apache.sling.ide.eclipse.core.internal;

import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The <tt>ProjectDescriptionManager</tt> exposes convenient APIs for managing a project's description
 *
 */
public class ProjectDescriptionManager {

    private static final String VALIDATION_BUILDER_NAME = "org.eclipse.wst.validation.validationbuilder";

    private final Logger logger;

    public ProjectDescriptionManager(Logger logger) {
        this.logger = logger;
    }

    public void enableValidationBuilderAndCommand(IProject project, IProgressMonitor monitor) throws CoreException {

        IProjectDescription description = project.getDescription();
        ICommand[] builders = description.getBuildSpec();
        for (ICommand builder : builders) {
            if (builder.getBuilderName().equals(VALIDATION_BUILDER_NAME)) {
                logger.trace("Validation builder already installed, skipping");
                return;
            }
        }

        logger.trace("Installing validation builder");

        ICommand[] newBuilders = new ICommand[builders.length + 1];
        System.arraycopy(builders, 0, newBuilders, 0, builders.length);
        ICommand validationCommand = description.newCommand();
        validationCommand.setBuilderName(VALIDATION_BUILDER_NAME);
        newBuilders[newBuilders.length - 1] = validationCommand;

        description.setBuildSpec(newBuilders);

        project.setDescription(description, monitor);

        logger.trace("Installed validation builder");
    }
}
