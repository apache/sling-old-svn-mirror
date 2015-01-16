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
package org.apache.sling.ide.eclipse.internal.validation;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.ValidatorMessage;

/**
 * The <tt>ContentProjectValidator</tt> validates that the defined content sync root of a project exists and is a
 * directory
 *
 */
public class ContentProjectValidator extends AbstractValidator {

    private boolean okToValidate = false;

    @Override
    public void validationStarting(IProject project, ValidationState state, IProgressMonitor monitor) {

        okToValidate = true;
    }

    @Override
    public ValidationResult validate(ValidationEvent event, ValidationState state, IProgressMonitor monitor) {

        ValidationResult res = new ValidationResult();

        if (!okToValidate) {
            return res;
        }

        okToValidate = false;

        IResource resource = event.getResource();

        IProject project = resource.getProject();
        IPath syncDir = ProjectUtil.getSyncDirectoryValue(project);

        IResource member = project.findMember(syncDir);
        if (member == null) {
            addValidatorMessage(res, project, "Configured sync dir " + syncDir + " does not exist");
        } else if (member.getType() != IResource.FOLDER) {
            addValidatorMessage(res, project, "Configured sync dir " + syncDir + " is not a directory");
        }

        return res;
    }

    private void addValidatorMessage(ValidationResult res, IProject project, String msg) {

        ValidatorMessage message = ValidatorMessage.create(msg, project);
        message.setAttribute(IMarker.LOCATION, project.getName());
        message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

        res.add(message);
    }
}
