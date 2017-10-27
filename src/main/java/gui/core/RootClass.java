package gui.core;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class RootClass extends Application {

	private StackPane rootLayout;
	private Stage primaryStage;
    

    /* (non-Javadoc)
     * @see javafx.application.Application#start(javafx.stage.Stage)
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("AptaSUITE");

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
        primaryStage.show();

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