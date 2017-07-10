/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class handling generic configuration errors
 */
public class InformationNotFoundException extends RuntimeException {


	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5705077063460063730L;

	public InformationNotFoundException() {
	}

	public InformationNotFoundException(String message) {
		super(message);
	}

	public InformationNotFoundException(Throwable cause) {
		super(cause);
	}

	public InformationNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public InformationNotFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
