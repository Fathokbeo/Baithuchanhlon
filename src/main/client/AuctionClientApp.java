package main.client;

import javafx.application.Application;
import javafx.stage.Stage;

public final class AuctionClientApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        AppContext.initialize(primaryStage);
        AppContext.showLoginView();
    }

    @Override
    public void stop() {
        AppContext.shutdown();
    }
}
