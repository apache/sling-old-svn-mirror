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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;

public class BundleProjectValidator extends AbstractValidator {

    private final ServiceComponentHeaderValidator scValidator = new ServiceComponentHeaderValidator();

    @Override
    public ValidationResult validate(ValidationEvent event, ValidationState state, IProgressMonitor monitor) {
        
        ValidationResult res = new ValidationResult();

        IResource resource = event.getResource();

        if (!resource.getName().equals("MANIFEST.MF") || resource.getType() != IResource.FILE) {
            return res;
        }

        try {
            // sync resource with filesystem if necessary
            if (!resource.isSynchronized(IResource.DEPTH_ZERO)) {
                resource.refreshLocal(IResource.DEPTH_ZERO, null);
            }
            for (IFile descriptor : scValidator.findMissingScrDescriptors((IFile)resource)) {
                addValidatorMessage(res, resource, "No DS descriptor found at path " + descriptor.getProjectRelativePath() +".");
            }
        } catch (CoreException e) {
            getLogger().warn("Failed validating project " + resource.getFullPath(), e);
        }
        
        return res;
    }
}
