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

public final class AuctionEmbeddedServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 5555;

    private AuctionSocketServer socketServer;
    private AuctionLifecycleScheduler lifecycleScheduler;
    private Thread discoveryThread;
    private boolean startedByThisProcess;

    public boolean startIfNeeded() {
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
            discoveryThread = NetworkDiscovery.startDiscoveryResponder();
            lifecycleScheduler = new AuctionLifecycleScheduler(auctionService, controller);
            lifecycleScheduler.start();
            System.out.println("[Server] Server dang chay tren TCP port " + DEFAULT_PORT + " va UDP port " + NetworkDiscovery.DISCOVERY_PORT);
        }
        return startedByThisProcess;
    }

    public boolean isStartedByThisProcess() {
        return startedByThisProcess;
    }

    @Override
    public void close() {
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
        if (lifecycleScheduler != null) {
            lifecycleScheduler.close();
        }
        if (socketServer != null) {
            socketServer.close();
        }
    }
}