package main.server;

import java.util.concurrent.CountDownLatch;

public final class AuctionServerMain {
    private AuctionServerMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        AuctionEmbeddedServer server = new AuctionEmbeddedServer();
        if (!server.startIfNeeded()) {
            System.err.println("Server da dang chay tren cong 5555.");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        new CountDownLatch(1).await();
    }
}