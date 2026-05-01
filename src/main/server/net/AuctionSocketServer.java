package main.server.net;

import main.server.controller.ServerRequestController;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuctionSocketServer implements AutoCloseable {
    private final int port;
    private final ServerRequestController controller;
    private final SessionRegistry registry;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public AuctionSocketServer(int port, ServerRequestController controller, SessionRegistry registry) {
        this.port = port;
        this.controller = controller;
        this.registry = registry;
    }

    public boolean start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (BindException bindException) {
            return false;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start socket server", exception);
        }

        acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientPool.submit(new ClientSession(clientSocket, controller, registry));
                } catch (IOException exception) {
                    if (!serverSocket.isClosed()) {
                        throw new IllegalStateException("Cannot accept client connection", exception);
                    }
                }
            }
        }, "auction-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    @Override
    public void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        registry.closeAll();
        clientPool.shutdownNow();
    }
}