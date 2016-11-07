/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class handling cases where potential aptamers do
 *         contain letters other that A C T or G.
 */
public class InvalidAlphabetException extends RuntimeException {

	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5166867691396154437L;

	public InvalidAlphabetException() {
	}

	public InvalidAlphabetException(String message) {
		super(message);
	}

	public InvalidAlphabetException(Throwable cause) {
		super(cause);
	}

	public InvalidAlphabetException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidAlphabetException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
