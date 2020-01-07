package openCVBasics;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import java.io.IOException;

public class Main extends Application {




    @Override
    public void start(Stage primaryStage) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../OpenCVBasics.fxml"));
//        Parent root = FXMLLoader.load();

        Parent root = loader.load();
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 395));
        primaryStage.show();

        // set the proper behavior on closing the application
        Controller controller = loader.getController();
        primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we)
            {
                controller.setClosed();
            }
        }));
    }

    public static void main (String... args) throws IOException {

        launch(args);

    }


}
