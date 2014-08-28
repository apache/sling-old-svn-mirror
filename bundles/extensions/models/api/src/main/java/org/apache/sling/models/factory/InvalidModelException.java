package org.apache.sling.models.factory;

public class InvalidModelException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4323592065808565135L;

    public InvalidModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidModelException(String message) {
        super(message);
    }

    public InvalidModelException(Throwable cause) {
        super(cause);
    }
}
