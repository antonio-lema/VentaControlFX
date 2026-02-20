package com.mycompany.ventacontrolfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Force load FontAwesome font to ensure icons render correctly
        try {
            javafx.scene.text.Font.loadFont(
                    de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.class
                            .getResource(de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.TTF_PATH).toExternalForm(),
                    10.0);
        } catch (Exception e) {
            System.err.println("Could not load FontAwesome font: " + e.getMessage());
        }

        // Initialize Database
        try (java.sql.Connection conn = com.mycompany.ventacontrolfx.dao.DBConnection.getConnection()) {
            com.mycompany.ventacontrolfx.dao.DatabaseInitializer.initialize(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        var url = getClass().getResource("/view/login.fxml");
        System.out.println("FXML URL = " + url);

        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 900, 600); // Set a reasonable size for login

        // 👇 AÑADE ESTA LÍNEA
        scene.getStylesheets().add(
                getClass().getResource("/view/style.css").toExternalForm());

        primaryStage.setTitle("Login - TPV Bazar Electrónico");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
