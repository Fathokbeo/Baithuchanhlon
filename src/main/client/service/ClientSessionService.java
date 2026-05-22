package main.client.service;

import main.client.net.AuctionClientConnection;
import main.shared.dto.*;
import main.shared.protocol.MessageType;

import java.util.concurrent.CompletableFuture;

public final class ClientSessionService {
    private final AuctionClientConnection connection;

    public ClientSessionService(AuctionClientConnection connection) {
        this.connection = connection;
    }

    public CompletableFuture<AuthResponse> login(String username, String password) {
        return connection.sendRequest(MessageType.LOGIN, new LoginRequest(username, password), AuthResponse.class);
    }

    public CompletableFuture<AuthResponse> register(RegisterRequest request) {
        return connection.sendRequest(MessageType.REGISTER, request, AuthResponse.class);
    }

    public CompletableFuture<AuctionsResponse> listAuctions() {
        return connection.sendRequest(MessageType.LIST_AUCTIONS, null, AuctionsResponse.class);
    }

    public CompletableFuture<AuctionsResponse> listMyAuctions() {
        return connection.sendRequest(MessageType.LIST_MY_AUCTIONS, null, AuctionsResponse.class);
    }

    public CompletableFuture<AuctionDetailResponse> getAuctionDetail(java.util.UUID auctionId) {
        return connection.sendRequest(MessageType.GET_AUCTION_DETAIL, new AuctionIdRequest(auctionId),
                AuctionDetailResponse.class);
    }

    public CompletableFuture<AuctionDetailResponse> saveAuction(UpsertAuctionRequest request) {
        return connection.sendRequest(MessageType.UPSERT_AUCTION, request, AuctionDetailResponse.class);
    }

    public CompletableFuture<SimpleResponse> deleteAuction(java.util.UUID auctionId) {
        return connection.sendRequest(MessageType.DELETE_AUCTION, new AuctionIdRequest(auctionId), SimpleResponse.class);
    }

    public CompletableFuture<AuctionDetailResponse> placeBid(PlaceBidRequest request) {
        return connection.sendRequest(MessageType.PLACE_BID, request, AuctionDetailResponse.class);
    }

    public CompletableFuture<AuctionDetailResponse> configureAutoBid(ConfigureAutoBidRequest request) {
        return connection.sendRequest(MessageType.CONFIGURE_AUTO_BID, request, AuctionDetailResponse.class);
    }

    public CompletableFuture<AuctionDetailResponse> updateAuctionStatus(UpdateAuctionStatusRequest request) {
        return connection.sendRequest(MessageType.UPDATE_AUCTION_STATUS, request, AuctionDetailResponse.class);
    }

    public CompletableFuture<UsersResponse> listUsers() {
        return connection.sendRequest(MessageType.LIST_USERS, null, UsersResponse.class);
    }
}
