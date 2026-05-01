package main.server.net;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public void closeAll() {
        sessions.forEach(ClientSession::close);
        sessions.clear();
    }
}