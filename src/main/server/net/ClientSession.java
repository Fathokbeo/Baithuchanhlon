package main.server.net;

import main.server.controller.ServerRequestController;
import main.shared.model.User;
import main.shared.protocol.ApiMessage;
import main.shared.util.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ClientSession implements Runnable {
    private final Socket socket;
    private final ServerRequestController controller;
    private final SessionRegistry registry;
    private volatile User authenticatedUser;
    private volatile boolean running = true;
    private PrintWriter writer;

    public ClientSession(Socket socket, ServerRequestController controller, SessionRegistry registry) {
        this.socket = socket;
        this.controller = controller;
        this.registry = registry;
    }

    @Override
    public void run() {
        registry.register(this);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            String line;
            while (running && (line = reader.readLine()) != null) {
                ApiMessage message = JsonUtils.read(line, ApiMessage.class);
                controller.handleRequest(this, message);
            }
        } catch (IOException exception) {
            if (running) {
                System.err.println("Client disconnected unexpectedly: " + exception.getMessage());
            }
        } finally {
            close();
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(User authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public void send(ApiMessage message) {
        sendRaw(JsonUtils.write(message));
    }

    public void sendRaw(String message) {
        PrintWriter currentWriter = writer;
        if (currentWriter != null) {
            currentWriter.println(message);
            currentWriter.flush();
        }
    }

    public void close() {
        running = false;
        registry.unregister(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}