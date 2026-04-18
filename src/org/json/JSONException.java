package org.json;

/**
 * Exception thrown when a syntax error or other issue occurs during JSON processing.
 * 
 * @author JSON.org
 * @version 2013-02-10
 */
public class JSONException extends RuntimeException {
    private static final long serialVersionUID = 0;
    /** The cause of the exception. */
    private Throwable cause;

    /**
     * Constructs a JSONException with an explanatory message.
     *
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
    }

    /**
     * Constructs a new JSONException with the specified cause.
     * @param cause the cause of the exception.
     */
    public JSONException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the cause of this exception or null if unknown.
     */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
