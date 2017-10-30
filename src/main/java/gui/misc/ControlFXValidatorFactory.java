/**
 * 
 */
package gui.misc;

import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.Validator;

import javafx.scene.control.Control;

/**
 * @author Jan Hoinka Static factory returning validators related to aptamers
 *         for ControlFX elements
 */
public class ControlFXValidatorFactory {

	/**
	 * Custom validator to ensure primers and barcodes contain only ACGTs
	 */
	public static Validator<String> DNAStringValidator = new Validator<String>() {
		@Override
		public ValidationResult apply(Control control, String value) {
			boolean condition = value != null ? !value.matches("[ACGT]+") : value == null;

			return ValidationResult.fromMessageIf(control, "Invalid Alphabet", Severity.ERROR, condition);
		}
	};

	/**
	 * Custom validator to ensure primers and barcodes contain either only ACGTs OR
	 * that the field is empty
	 */
	public static Validator<String> DNAStringOrEmptyValidator = new Validator<String>() {
		@Override
		public ValidationResult apply(Control control, String value) {
			boolean condition = value != null ? value.matches("[ACGT]+") : value == null;
			return ValidationResult.fromMessageIf(control, "Invalid Alphabet", Severity.ERROR, !(condition || value.isEmpty()));
		}
	};
	
	
	/**
	 * Custom validator to ensure primers and barcodes contain either only ACGTs OR
	 * that the field is empty
	 */
	public static Validator<String> AllwaysWrongValidator(String message){
		
		Validator<String> validator = new Validator<String>() {
		
			@Override
			public ValidationResult apply(Control control, String value) {
				boolean condition = true;
				return ValidationResult.fromMessageIf(control, message, Severity.ERROR, condition );
			}
		
		};
		
		return validator;	
	}
	
}
