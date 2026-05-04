package main.server;

import main.server.controller.ServerRequestController;
import main.server.dao.AuctionDao;
import main.server.dao.DatabaseManager;
import main.server.dao.UserDao;
import main.server.net.AuctionSocketServer;
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
            lifecycleScheduler = new AuctionLifecycleScheduler(auctionService, controller);
            lifecycleScheduler.start();
        }
        return startedByThisProcess;
    }

    public boolean isStartedByThisProcess() {
        return startedByThisProcess;
    }

    @Override
    public void close() {
        if (lifecycleScheduler != null) {
            lifecycleScheduler.close();
        }
        if (socketServer != null) {
            socketServer.close();
        }
    }
}