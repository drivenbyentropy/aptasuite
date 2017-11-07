/**
 * 
 */
package gui.misc;

import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.StyleClassDecoration;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;

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
	 * Custom validator which is allways correct
	 */
	public static Validator<String> AllwaysCorrectValidator = new Validator<String>() {
		@Override
		public ValidationResult apply(Control control, String value) {
			return ValidationResult.fromErrorIf(control, "", false);
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
	
	
	/**
	 * Macro which places a validation support <code>validation_support</code> on element <code>node</code>
	 * once the user has clicked on it and subsequently removes it when the user moves the mouse out of the 
	 * element
	 * 
	 * Applies to all elements which are editable.
	 *  
	 * @param node the JavaFX element in question
	 * @param message the error message to display
	 * @param validation_support the validation support class handinling the event
	 * @param fallback a Validation instance to which the element will fall back to once <code>validator</code> has been removed
	 * @param fallback_required true if the fallback validator is required 
	 */
	public static void setTemporaryValidation(Control node, Validator validator, ValidationSupport validation_support, Validator fallback, boolean fallback_required) {
		
		// Add warning decoration and validator
		StyleClassDecoration decoration = new StyleClassDecoration("warning");
    	Decorator.addDecoration(node, decoration);
    	validation_support.registerValidator(node, validator);
		
		node.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t) {
            	node.setOnMouseExited(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent t) {
                        Decorator.removeDecoration(node, decoration);
                        validation_support.registerValidator(node, fallback_required, fallback);
                    }
                });
            }
        });
		
	}
	
	
}
