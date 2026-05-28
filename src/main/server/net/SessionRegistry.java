package main.server.net;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SessionRegistry {
    private final Set<ClientSession> sessions = ConcurrentHashMap.newKeySet();

    public void register(ClientSession session) {
        sessions.add(session);
    }

    public void unregister(ClientSession session) {
        sessions.remove(session);
    }

    public void broadcast(String message) {
        sessions.forEach(session -> session.sendRaw(message));
    }

    public void forEach(Consumer<ClientSession> consumer) {
        sessions.forEach(consumer);
    }

    public void closeAll() {
        sessions.forEach(ClientSession::close);
        sessions.clear();
    }
}
