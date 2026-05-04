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
import java.net.URL;

public final class AppContext {
    private static final int DEFAULT_SERVER_PORT = 5555;
    private static final String[] RESOURCE_ROOTS = {"", "/main/resources"};

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
            connection = null;
        }
        if (embeddedServer != null) {
            embeddedServer.close();
            embeddedServer = null;
        }
    }

    private static void showScene(String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(resolveResource("/css/app.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load FXML: " + fxmlPath, exception);
        }
    }

    private static URL resolveResource(String resourcePath) {
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        for (String root : RESOURCE_ROOTS) {
            URL resource = AppContext.class.getResource(root + normalizedPath);
            if (resource != null) {
                return resource;
            }
        }
        throw new IllegalStateException("Cannot find resource: " + normalizedPath);
    }
}
