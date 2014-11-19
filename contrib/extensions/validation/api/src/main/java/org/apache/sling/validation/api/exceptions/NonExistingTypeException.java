package org.apache.sling.validation.api.exceptions;

import org.apache.sling.validation.api.Type;

/**
 * {@link RuntimeException} indicating the usage of a non-defined {@link Type}.
 */
public class NonExistingTypeException extends RuntimeException {

    public NonExistingTypeException(String message) {
        super(message);
    }

}
