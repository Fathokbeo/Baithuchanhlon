package main.client.net;

import main.shared.dto.AuctionEventDto;
import main.shared.protocol.ApiMessage;
import main.shared.protocol.MessageCategory;
import main.shared.protocol.MessageType;
import main.shared.util.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuctionClientConnection implements AutoCloseable {
    private final String host;
    private final int port;
    private final Map<String, CompletableFuture<ApiMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final List<java.util.function.Consumer<AuctionEventDto>> auctionListeners = new CopyOnWriteArrayList<>();

    private Socket socket;
    private PrintWriter writer;
    private Thread readerThread;

    public AuctionClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connectWithRetry() {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                connect();
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                try {
                    Thread.sleep(Duration.ofMillis(250));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }
        throw lastException == null ? new IllegalStateException("Cannot connect to server") : lastException;
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            readerThread = new Thread(() -> readLoop(reader), "auction-client-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the ket noi toi server", exception);
        }
    }

    public <T> CompletableFuture<T> sendRequest(MessageType type, Object payload, Class<T> responseType) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<ApiMessage> responseFuture = new CompletableFuture<>();
        pendingRequests.put(requestId, responseFuture);
        writer.println(JsonUtils.write(new ApiMessage(
                MessageCategory.REQUEST,
                type,
                requestId,
                true,
                null,
                payload == null ? null : JsonUtils.toJsonNode(payload)
        )));
        writer.flush();
        return responseFuture.thenApply(response -> {
            if (!response.isSuccess()) {
                throw new CompletionException(new IllegalStateException(response.getErrorMessage()));
            }
            if (responseType == Void.class || response.getPayload() == null) {
                return null;
            }
            return JsonUtils.fromJsonNode(response.getPayload(), responseType);
        });
    }

    public void addAuctionListener(java.util.function.Consumer<AuctionEventDto> listener) {
        auctionListeners.add(listener);
    }

    public void removeAuctionListener(java.util.function.Consumer<AuctionEventDto> listener) {
        auctionListeners.remove(listener);
    }

    private void readLoop(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                ApiMessage message = JsonUtils.read(line, ApiMessage.class);
                if (message.getCategory() == MessageCategory.RESPONSE) {
                    CompletableFuture<ApiMessage> future = pendingRequests.remove(message.getRequestId());
                    if (future != null) {
                        future.complete(message);
                    }
                } else if (message.getCategory() == MessageCategory.EVENT
                        && message.getType() == MessageType.AUCTION_CHANGED) {
                    AuctionEventDto event = JsonUtils.fromJsonNode(message.getPayload(), AuctionEventDto.class);
                    auctionListeners.forEach(listener -> listener.accept(event));
                }
            }
        } catch (IOException exception) {
            pendingRequests.values().forEach(future -> future.completeExceptionally(exception));
            pendingRequests.clear();
        }
    }

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
