package org.apache.sling.models.spi;

/**
 * Exception which is triggered whenever a Sling Model cannot be instanciated due to some validation errors (i.e. required fields/methods could not be injected).
 * @see ModelFactory
 *
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 7870762030809272254L;
    
    public ValidationException(String message) {
        super(message);
    }
    
}
