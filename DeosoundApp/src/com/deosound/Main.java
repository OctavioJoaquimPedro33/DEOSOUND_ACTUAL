package com.deosound;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
	    FXMLLoader loader = new FXMLLoader(getClass().getResource("TrackView.fxml"));
	    Parent root = loader.load();

	    // Acessa o controlador e carrega várias músicas
	    TrackController controller = loader.getController();
	    controller.loadTracks("37i9dQZF1DXcBWIGoYBM5M");  // ID de uma playlist exemplo

	    Scene scene = new Scene(root);
	    scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
	    primaryStage.setTitle("DeosoundApp");
	    primaryStage.setScene(scene);
	    primaryStage.show();
	}

    public static void main(String[] args) {
        launch(args);
    }
}
