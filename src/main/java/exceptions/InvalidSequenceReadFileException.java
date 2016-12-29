/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class handling wrong configurations regarding
 * the sequence files
 */
public class InvalidSequenceReadFileException extends RuntimeException {


	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5705077063460063730L;

	public InvalidSequenceReadFileException() {
	}

	public InvalidSequenceReadFileException(String message) {
		super(message);
	}

	public InvalidSequenceReadFileException(Throwable cause) {
		super(cause);
	}

	public InvalidSequenceReadFileException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidSequenceReadFileException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
