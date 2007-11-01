package org.apache.sling.microsling.helpers.exceptions;

import org.apache.sling.api.SlingException;

/** Indicates a missing Request attribute */
public class MissingRequestAttributeException extends SlingException {
    private static final long serialVersionUID = 1L;

    public MissingRequestAttributeException(String attributeName) {
        super("Missing request attribute '" + attributeName + "'");
    }
}
