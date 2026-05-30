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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class AuctionEmbeddedServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 5555;
    private static final Path PID_FILE = Path.of("./data/server.pid");

    private AuctionSocketServer socketServer;
    private AuctionLifecycleScheduler lifecycleScheduler;
    private Closeable discoveryResponder;
    private boolean startedByThisProcess;

    public boolean startIfNeeded() {
        if (!isPortAvailable(DEFAULT_PORT)) {
            if (!killPreviousInstance()) {
                return false;
            }
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
            writePidFile();
            discoveryResponder = NetworkDiscovery.startDiscoveryResponder();
            lifecycleScheduler = new AuctionLifecycleScheduler(auctionService, controller);
            lifecycleScheduler.start();
            System.out.println("[Server] Server đang chạy trên TCP port " + DEFAULT_PORT + " và UDP port " + NetworkDiscovery.DISCOVERY_PORT);
        }
        return startedByThisProcess;
    }

    private static boolean killPreviousInstance() {
        if (!Files.exists(PID_FILE)) {
            System.err.println("[Server] Port " + DEFAULT_PORT + " đang bị chiếm bởi tiến trình khác (không phải server này).");
            return false;
        }
        try {
            long pid = Long.parseLong(Files.readString(PID_FILE).strip());
            var handle = ProcessHandle.of(pid);
            if (handle.isEmpty()) {
                Files.deleteIfExists(PID_FILE);
                return isPortAvailable(DEFAULT_PORT);
            }
            System.out.println("[Server] Đang dừng instance cũ (PID " + pid + ")...");
            handle.get().destroyForcibly();
            handle.get().onExit().get(5, TimeUnit.SECONDS);
            Files.deleteIfExists(PID_FILE);
            for (int i = 0; i < 25; i++) {
                if (isPortAvailable(DEFAULT_PORT)) {
                    return true;
                }
                Thread.sleep(200);
            }
            System.err.println("[Server] Port " + DEFAULT_PORT + " vẫn chưa được giải phóng sau khi dừng instance cũ.");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.err.println("[Server] Không thể dừng instance cũ: " + e.getMessage());
            return false;
        }
    }

    private static void writePidFile() {
        try {
            Files.createDirectories(PID_FILE.getParent());
            Files.writeString(PID_FILE, String.valueOf(ProcessHandle.current().pid()));
        } catch (IOException ignored) {}
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
        try {
            Files.deleteIfExists(PID_FILE);
        } catch (IOException ignored) {}
    }
}
