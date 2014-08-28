package org.apache.sling.models.factory;

public class InvalidModelException extends RuntimeException {

    /**
     * Exception which is triggered when the Model could not be instanciated due to
     * model class is not having a model annotation, some reflection error, invalid constructors or 
     * some exception within the post construct method was triggered.
     * @see ModelFactory
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
