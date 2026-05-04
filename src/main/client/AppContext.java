package main.client;

import main.client.net.AuctionClientConnection;
import main.client.service.ClientSessionService;
import main.client.state.ClientState;
import main.server.AuctionEmbeddedServer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class AppContext {
    private static final int DEFAULT_SERVER_PORT = 5555;

    private static Stage primaryStage;
    private static AuctionClientConnection connection;
    private static ClientSessionService clientSessionService;
    private static ClientState clientState;
    private static AuctionEmbeddedServer embeddedServer;

    private AppContext() {
    }

    public static void setEmbeddedServer(AuctionEmbeddedServer embeddedServer) {
        AppContext.embeddedServer = embeddedServer;
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Auction App AI");
        connection = new AuctionClientConnection("127.0.0.1", DEFAULT_SERVER_PORT);
        connection.connectWithRetry();
        clientSessionService = new ClientSessionService(connection);
        clientState = new ClientState();
    }

    public static ClientSessionService service() {
        return clientSessionService;
    }

    public static AuctionClientConnection connection() {
        return connection;
    }

    public static ClientState state() {
        return clientState;
    }

    public static void showLoginView() {
        showScene("/fxml/login-view.fxml", 560, 700);
    }

    public static void showDashboardView() {
        showScene("/fxml/dashboard-view.fxml", 1400, 860);
    }

    public static void shutdown() {
        if (connection != null) {
            connection.close();
        }
    }

    private static void showScene(String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(AppContext.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(AppContext.class.getResource("/css/app.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load FXML: " + fxmlPath, exception);
        }
    }
}
