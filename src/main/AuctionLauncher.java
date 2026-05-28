package main;

import main.client.AuctionClientApp;
import main.client.AppContext;
import main.server.AuctionEmbeddedServer;
import main.server.net.NetworkDiscovery;
import javafx.application.Application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

public final class AuctionLauncher {
    private AuctionLauncher() {
    }

    public static void main(String[] args) {
        AuctionEmbeddedServer embeddedServer = new AuctionEmbeddedServer();
        try {
            // Tìm server đang chạy trên mạng LAN trước
            String serverHost = NetworkDiscovery.findServerOnNetwork();

            if (serverHost == null) {
                if (canConnectToLocalServer()) {
                    // Discovery UDP có thể không thấy server cùng máy, nhưng TCP server vẫn đang chạy.
                    serverHost = "127.0.0.1";
                } else {
                    // Không tìm thấy server nào → khởi động server cục bộ cho chế độ chạy một app.
                    embeddedServer.startIfNeeded();
                    serverHost = "127.0.0.1";
                }
            }

            AppContext.setServerHost(serverHost);
            AppContext.setEmbeddedServer(embeddedServer);
            Application.launch(AuctionClientApp.class, args);
        } finally {
            embeddedServer.close();
        }
    }

    private static boolean canConnectToLocalServer() {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress("127.0.0.1", AuctionEmbeddedServer.DEFAULT_PORT),
                    (int) Duration.ofMillis(500).toMillis()
            );
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
