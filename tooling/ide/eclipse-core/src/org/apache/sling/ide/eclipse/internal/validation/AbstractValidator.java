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

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidatorMessage;

public abstract class AbstractValidator extends org.eclipse.wst.validation.AbstractValidator {

    protected final static String VALIDATION_MARKER_TYPE = "org.apache.sling.ide.eclipse-core.validationMarker";

    /**
     * Deletes all validation markers in the given resource.
     * Usually markers are automatically cleared by the validation framework before the validators are triggered for a specific resource.
     * This method only needs to be called in case the validator sets markers outside of the resource for which validation was triggered.
     * @param resource
     * @throws CoreException
     */
    protected void deleteValidationMarkers(IResource resource) throws CoreException {
        // delete validation markers
        resource.deleteMarkers(VALIDATION_MARKER_TYPE, false, IResource.DEPTH_ZERO);
    }
    
    protected void addValidatorMessage(ValidationResult res, IResource resource, String msg) {
        ValidatorMessage message = ValidatorMessage.create(msg, resource);
        message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        // ID is not taken over from the extension point (that only works for the IReporter (which is for validation V1)
        // reported at https://bugs.eclipse.org/bugs/show_bug.cgi?id=307093)
        message.setType(VALIDATION_MARKER_TYPE);
        res.add(message);
    }
    
    protected Logger getLogger() {
        return Activator.getDefault().getPluginLogger();
    }
}
