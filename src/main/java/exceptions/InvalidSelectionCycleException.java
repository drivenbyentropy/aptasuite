/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class when an invalid selection cycle number is entered.
 * Eg. a round number < 0.
 */
public class InvalidSelectionCycleException extends RuntimeException {

	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5166867691396154437L;

	public InvalidSelectionCycleException() {
	}

	public InvalidSelectionCycleException(String message) {
		super(message);
	}

	public InvalidSelectionCycleException(Throwable cause) {
		super(cause);
	}

	public InvalidSelectionCycleException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidSelectionCycleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}