package org.apache.sling.models.factory;


/**
 * Exception which is triggered whenever a Sling Model could not be instanciated because it could not be adapted
 * from the given adaptable.
 * 
 * @see ModelFactory
 *
 */
public class InvalidAdaptableException extends RuntimeException {
    private static final long serialVersionUID = -1209301268928038702L;

    public InvalidAdaptableException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAdaptableException(String message) {
        super(message);
    }

    public InvalidAdaptableException(Throwable cause) {
        super(cause);
    }
}
