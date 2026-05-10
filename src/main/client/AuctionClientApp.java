package main.client;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class AuctionClientApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.getIcons().add(
                new Image(AuctionClientApp.class.getResourceAsStream("/images/app-icon.png"))
        );

        AppContext.initialize(primaryStage);
        AppContext.showLoginView();
    }

    @Override
    public void stop() {
        AppContext.shutdown();
    }
}
