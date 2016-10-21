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

    protected void deleteValidationMarkers(IResource resource) throws CoreException {
        
        // delete validation markers
        resource.deleteMarkers(VALIDATION_MARKER_TYPE, false, IResource.DEPTH_ZERO);
    }
    
    protected void addValidatorMessage(ValidationResult res, IResource resource, String msg) {
        ValidatorMessage message = ValidatorMessage.create(msg, resource);
        message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        // ID is not taken over from the extension point (that only works for the IReporter, which is still provisional)
        message.setType(VALIDATION_MARKER_TYPE);
        res.add(message);
    }
    
    protected Logger getLogger() {
        return Activator.getDefault().getPluginLogger();
    }
}
