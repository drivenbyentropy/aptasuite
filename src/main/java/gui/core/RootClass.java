package gui.core;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import gui.misc.FXConcurrent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import utilities.Version;

public class RootClass extends Application {

	private StackPane rootLayout;
	private Stage primaryStage;
    

    /* (non-Javadoc)
     * @see javafx.application.Application#start(javafx.stage.Stage)
     */
    @Override
    public void start(Stage primaryStage) {
    	
    	// Read version 
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("AptaSUITE v"+Version.versionString());
        
        // Add icon
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("logo.png")));

        try {
	        // Load root layout from fxml file.
	        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("gui/core/RootLayout.fxml"));
	        rootLayout = (StackPane) loader.load();
	
	        // Get a handle to the corresponding controller and pass the primary stage
	        RootLayoutController rootLayoutController = loader.getController();
	        rootLayoutController.setPrimaryStage(primaryStage);
	        
	        // Show the scene containing the root layout.
	        Scene scene = new Scene(rootLayout);
	        primaryStage.setScene(scene);
	        primaryStage.setOnHidden(e -> Platform.exit());
	        primaryStage.show();
	        
	        // Now check for new versions online and alert the user if there is one
			if( Version.newerVersionAvailable() ) {
				
				Alert alert = new Alert(AlertType.CONFIRMATION);
					
				alert.setTitle("Newer version available");
				alert.setHeaderText("A newer verion of AptaSuite is available.");
				alert.setContentText("It is highly suggested to update to the latest release.\nPress OK to open the download page for AptaSuite and download and extract the new version to a directory of your choice.");
	
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK){
	
			    	// TODO find JavaFX version of this
			        try {
			        	URI u = new URI("https://github.com/drivenbyentropy/aptasuite/releases/latest");
						java.awt.Desktop.getDesktop().browse(u);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				}		
				
			}
				
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    
    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void lauchMainWindow() {
        launch();
    }
}