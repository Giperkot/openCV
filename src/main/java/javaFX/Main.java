package javaFX;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;

public class Main extends Application {




    @Override
    public void start(Stage primaryStage) throws Exception {

        System.out.println(getClass().getResource("FXHelloCV.fxml"));
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Parent root = FXMLLoader.load(getClass().getResource("../FXHelloCV.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 395));
        primaryStage.show();
    }

    public static void main (String... args) throws IOException {

        launch(args);

    }


}
