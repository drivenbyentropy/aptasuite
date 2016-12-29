/**
 * 
 */
package exceptions;

/**
 * @author Jan Hoinka Exception class handling generic configuration errors
 */
public class InvalidConfigurationException extends RuntimeException {


	/**
	 * Auto generated version ID for serialization purposes
	 */
	private static final long serialVersionUID = -5705077063460063730L;

	public InvalidConfigurationException() {
	}

	public InvalidConfigurationException(String message) {
		super(message);
	}

	public InvalidConfigurationException(Throwable cause) {
		super(cause);
	}

	public InvalidConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
