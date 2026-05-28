package main.server;

import main.server.controller.ServerRequestController;
import main.server.dao.AuctionDao;
import main.server.dao.DatabaseManager;
import main.server.dao.UserDao;
import main.server.net.AuctionSocketServer;
import main.server.net.NetworkDiscovery;
import main.server.net.SessionRegistry;
import main.server.scheduler.AuctionLifecycleScheduler;
import main.server.seed.DataSeeder;
import main.server.service.AuctionRulesEngine;
import main.server.service.AuctionService;
import main.server.service.AuthService;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

public final class AuctionEmbeddedServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 5555;

    private AuctionSocketServer socketServer;
    private AuctionLifecycleScheduler lifecycleScheduler;
    private Closeable discoveryResponder;
    private boolean startedByThisProcess;

    public boolean startIfNeeded() {
        if (!isPortAvailable(DEFAULT_PORT)) {
            return false;
        }

        DatabaseManager databaseManager = DatabaseManager.getInstance();
        databaseManager.initializeSchema();

        UserDao userDao = new UserDao(databaseManager);
        AuctionDao auctionDao = new AuctionDao(databaseManager);
        new DataSeeder(userDao, auctionDao).seedIfEmpty();

        AuthService authService = new AuthService(userDao);
        AuctionService auctionService = new AuctionService(auctionDao, userDao, new AuctionRulesEngine());
        SessionRegistry sessionRegistry = new SessionRegistry();
        ServerRequestController controller = new ServerRequestController(authService, auctionService, sessionRegistry);

        socketServer = new AuctionSocketServer(DEFAULT_PORT, controller, sessionRegistry);
        startedByThisProcess = socketServer.start();
        if (startedByThisProcess) {
            discoveryResponder = NetworkDiscovery.startDiscoveryResponder();
            lifecycleScheduler = new AuctionLifecycleScheduler(auctionService, controller);
            lifecycleScheduler.start();
            System.out.println("[Server] Server dang chay tren TCP port " + DEFAULT_PORT + " va UDP port " + NetworkDiscovery.DISCOVERY_PORT);
        }
        return startedByThisProcess;
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public boolean isStartedByThisProcess() {
        return startedByThisProcess;
    }

    @Override
    public void close() {
        if (discoveryResponder != null) {
            try {
                discoveryResponder.close();
            } catch (IOException ignored) {}
        }
        if (lifecycleScheduler != null) {
            lifecycleScheduler.close();
        }
        if (socketServer != null) {
            socketServer.close();
        }
    }
}
