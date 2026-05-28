package main;

import main.client.AuctionClientApp;
import main.client.AppContext;
import main.server.AuctionEmbeddedServer;
import main.server.net.NetworkDiscovery;
import javafx.application.Application;

public final class AuctionLauncher {
    private AuctionLauncher() {
    }

    public static void main(String[] args) {
        AuctionEmbeddedServer embeddedServer = new AuctionEmbeddedServer();
        try {
            // Tìm server đang chạy trên mạng LAN trước
            String serverHost = NetworkDiscovery.findServerOnNetwork();

            if (serverHost == null) {
                // Không tìm thấy server trên mạng → khởi động server cục bộ
                embeddedServer.startIfNeeded();
                serverHost = "127.0.0.1";
            }

            AppContext.setServerHost(serverHost);
            AppContext.setEmbeddedServer(embeddedServer);
            Application.launch(AuctionClientApp.class, args);
        } finally {
            embeddedServer.close();
        }
    }
}
