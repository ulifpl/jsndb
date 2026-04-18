package org.jsndb;

/**
 * Represents an error within the jsndb database engine.
 */
public class error {

	/**
	 * Defines the types of errors that can occur in the engine.
	 */
	public static enum errortype {
		/** Error in the database engine logic. */
		engineDbError, 
		/** Database files are missing or inaccessible. */
		dbIsGone, 
		/** Database integrity is compromised. */
		dbBrokenError, 
		/** Missing object ID. */
		noIdError, 
		/** Error in an index operation. */
		indexError, 
		/** Exception occurred in an external library or system. */
		externalException
	}

	private String detail;
	private errortype type;
	private Exception exception;

	/**
	 * Constructs a new error.
	 * @param detalle Description of the error.
	 * @param typerror The category of the error.
	 * @param xception The underlying exception, if any.
	 */
	public error(String detalle, errortype typerror, Exception xception) {
		detail = detalle;
		type = typerror;
		exception = xception;
	}

	/**
	 * Gets the error description.
	 * @return description string.
	 */
	public String getDetail() {
		return detail;
	}

	/**
	 * Gets the type of the error.
	 * @return error type enum.
	 */
	public errortype getType() {
		return type;
	}

	/**
	 * Gets the underlying exception.
	 * @return the exception object or null.
	 */
	public Exception getException() {
		return exception;
	}

}
