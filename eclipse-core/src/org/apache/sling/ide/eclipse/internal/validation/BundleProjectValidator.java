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

import org.apache.sling.ide.eclipse.core.debug.PluginLogger;
import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.ValidatorMessage;

public class BundleProjectValidator extends AbstractValidator {

    private final ServiceComponentHeaderValidator scValidator = new ServiceComponentHeaderValidator();

    @Override
    public ValidationResult validate(ValidationEvent event, ValidationState state, IProgressMonitor monitor) {
        
        ValidationResult res = new ValidationResult();

        IResource resource = event.getResource();

        if (!resource.getName().equals("MANIFEST.MF") || resource.getType() != IResource.FILE) {
            return res;
        }

        IFile m = (IFile) resource;

        PluginLogger pluginLogger = Activator.getDefault() .getPluginLogger();

        try {

            for (IFile descriptor : scValidator.findMissingScrDescriptors(m)) {
                ValidatorMessage dsMessage = ValidatorMessage.create(
                        "No DS descriptor found at path " + descriptor.getProjectRelativePath(), m);
                dsMessage.setAttribute(IMarker.LOCATION, m.getName());
                dsMessage.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

                res.add(dsMessage);

            }

        } catch (CoreException e) {
            pluginLogger.warn("Failed validating project " + resource.getFullPath(), e);
        }
        
        return res;
    }
}
