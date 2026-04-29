package main.client.state;

import main.shared.dto.SessionUserDto;

public final class ClientState {
    private SessionUserDto currentUser;

    public SessionUserDto getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(SessionUserDto currentUser) {
        this.currentUser = currentUser;
    }
}
