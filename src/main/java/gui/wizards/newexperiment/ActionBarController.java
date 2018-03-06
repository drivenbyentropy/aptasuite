/**
 * 
 */
package gui.wizards.newexperiment;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * @author Jan Hoinka
 *
 */
public class ActionBarController {
	
	@FXML
	private Label errorLabel;
	
	@FXML
	private Button backButton;
	
	@FXML
	private Button nextButton;
	
	@FXML
	private Button finishButton;

	/**
	 * @return the errorLabel
	 */
	public Label getErrorLabel() {
		return errorLabel;
	}

	/**
	 * @return the backButton
	 */
	public Button getBackButton() {
		return backButton;
	}

	/**
	 * @return the nextButton
	 */
	public Button getNextButton() {
		return nextButton;
	}

	/**
	 * @return the finishButton
	 */
	public Button getFinishButton() {
		return finishButton;
	}
	
	

}
