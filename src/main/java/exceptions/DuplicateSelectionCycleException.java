/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class handling the case where two or more selection cycles
 * have the same name.
 */
public class DuplicateSelectionCycleException extends RuntimeException {


	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5705077063460063730L;

	public DuplicateSelectionCycleException() {
	}

	public DuplicateSelectionCycleException(String message) {
		super(message);
	}

	public DuplicateSelectionCycleException(Throwable cause) {
		super(cause);
	}

	public DuplicateSelectionCycleException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateSelectionCycleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
