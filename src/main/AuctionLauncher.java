package main;

import main.client.AuctionClientApp;
import main.client.AppContext;
import main.server.AuctionEmbeddedServer;
import javafx.application.Application;

public final class AuctionLauncher {
    private AuctionLauncher() {
    }

    public static void main(String[] args) {
        AuctionEmbeddedServer embeddedServer = new AuctionEmbeddedServer();
        embeddedServer.startIfNeeded();
        AppContext.setEmbeddedServer(embeddedServer);
        Application.launch(AuctionClientApp.class, args);
    }
}
