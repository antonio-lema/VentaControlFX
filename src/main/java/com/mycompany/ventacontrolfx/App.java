package com.mycompany.ventacontrolfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        var url = getClass().getResource("/view/main.fxml");
        System.out.println("FXML URL = " + url);

        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root);

        // 👇 AÑADE ESTA LÍNEA
        scene.getStylesheets().add(
                getClass().getResource("/view/style.css").toExternalForm()
        );

        primaryStage.setTitle("TPV Bazar Electrónico");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
