package org.apache.sling.commons.json;

/**
 * The JSONException is thrown by the JSON.org classes then things are amiss.
 * @author JSON.org
 * @version 2
 */
public class JSONException extends Exception {
    private static final long serialVersionUID = 8262447656274268887L;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable t) {
        super(t);
    }

    public JSONException(String message, Throwable t) {
        super(message, t);
    }
}
