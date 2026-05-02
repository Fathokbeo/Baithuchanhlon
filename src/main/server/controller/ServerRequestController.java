package main.server.controller;

import main.server.net.ClientSession;
import main.server.net.SessionRegistry;
import main.server.service.AuctionService;
import main.server.service.AuctionViewMapper;
import main.server.service.AuthService;
import main.server.service.UserViewMapper;
import main.shared.dto.AuctionDetailResponse;
import main.shared.dto.AuctionEventDto;
import main.shared.dto.AuctionIdRequest;
import main.shared.dto.AuctionsResponse;
import main.shared.dto.AuthResponse;
import main.shared.dto.ConfigureAutoBidRequest;
import main.shared.dto.LoginRequest;
import main.shared.dto.PlaceBidRequest;
import main.shared.dto.RegisterRequest;
import main.shared.dto.SimpleResponse;
import main.shared.dto.UpdateAuctionStatusRequest;
import main.shared.dto.UpsertAuctionRequest;
import main.shared.dto.UsersResponse;
import main.shared.model.Auction;
import main.shared.model.User;
import main.shared.protocol.ApiMessage;
import main.shared.protocol.MessageCategory;
import main.shared.protocol.MessageType;
import main.shared.util.JsonUtils;

import java.time.LocalDateTime;

public final class ServerRequestController {
    private final AuthService authService;
    private final AuctionService auctionService;
    private final SessionRegistry sessionRegistry;

    public ServerRequestController(AuthService authService, AuctionService auctionService, SessionRegistry sessionRegistry) {
        this.authService = authService;
        this.auctionService = auctionService;
        this.sessionRegistry = sessionRegistry;
    }

    public void handleRequest(ClientSession session, ApiMessage request) {
        try {
            switch (request.getType()) {
                case LOGIN -> handleLogin(session, request);
                case REGISTER -> handleRegister(session, request);
                case LIST_AUCTIONS -> handleListAuctions(session, request);
                case LIST_MY_AUCTIONS -> handleListMyAuctions(session, request);
                case GET_AUCTION_DETAIL -> handleAuctionDetail(session, request);
                case UPSERT_AUCTION -> handleUpsertAuction(session, request);
                case DELETE_AUCTION -> handleDeleteAuction(session, request);
                case PLACE_BID -> handlePlaceBid(session, request);
                case CONFIGURE_AUTO_BID -> handleConfigureAutoBid(session, request);
                case UPDATE_AUCTION_STATUS -> handleUpdateStatus(session, request);
                case LIST_USERS -> handleListUsers(session, request);
                case AUCTION_CHANGED -> sendError(session, request, "Client khong the gui su kien realtime");
            }
        } catch (RuntimeException exception) {
            sendError(session, request, exception.getMessage());
        }
    }

    public void broadcastAuctionChange(Auction auction, String eventName) {
        String sellerName = auctionService.sellerName(auction);
        AuctionEventDto event = new AuctionEventDto(
                eventName,
                AuctionViewMapper.toSummary(auction, sellerName),
                AuctionViewMapper.toDetail(auction, sellerName)
        );
        sessionRegistry.broadcast(JsonUtils.write(new ApiMessage(
                MessageCategory.EVENT,
                MessageType.AUCTION_CHANGED,
                null,
                true,
                null,
                JsonUtils.toJsonNode(event)
        )));
    }

    private void handleLogin(ClientSession session, ApiMessage request) {
        LoginRequest payload = JsonUtils.fromJsonNode(request.getPayload(), LoginRequest.class);
        User user = authService.login(payload.username(), payload.password());
        session.setAuthenticatedUser(user);
        sendSuccess(session, request, new AuthResponse(UserViewMapper.toSessionUser(user), "Dang nhap thanh cong"));
    }

    private void handleRegister(ClientSession session, ApiMessage request) {
        RegisterRequest payload = JsonUtils.fromJsonNode(request.getPayload(), RegisterRequest.class);
        User user = authService.register(payload.username(), payload.password(), payload.displayName(), payload.role());
        session.setAuthenticatedUser(user);
        sendSuccess(session, request, new AuthResponse(UserViewMapper.toSessionUser(user), "Dang ky thanh cong"));
    }

    private void handleListAuctions(ClientSession session, ApiMessage request) {
        requireUser(session);
        sendSuccess(session, request, new AuctionsResponse(auctionService.listAuctions().stream()
                .map(auction -> AuctionViewMapper.toSummary(auction, auctionService.sellerName(auction)))
                .toList()));
    }

    private void handleListMyAuctions(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        sendSuccess(session, request, new AuctionsResponse(auctionService.listMyAuctions(user).stream()
                .map(auction -> AuctionViewMapper.toSummary(auction, auctionService.sellerName(auction)))
                .toList()));
    }

    private void handleAuctionDetail(ClientSession session, ApiMessage request) {
        requireUser(session);
        AuctionIdRequest payload = JsonUtils.fromJsonNode(request.getPayload(), AuctionIdRequest.class);
        Auction auction = auctionService.getAuction(payload.auctionId());
        sendSuccess(session, request, new AuctionDetailResponse(
                AuctionViewMapper.toDetail(auction, auctionService.sellerName(auction))
        ));
    }

    private void handleUpsertAuction(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        UpsertAuctionRequest payload = JsonUtils.fromJsonNode(request.getPayload(), UpsertAuctionRequest.class);
        Auction auction = auctionService.saveAuction(user, payload, LocalDateTime.now());
        sendSuccess(session, request, new AuctionDetailResponse(
                AuctionViewMapper.toDetail(auction, auctionService.sellerName(auction))
        ));
        broadcastAuctionChange(auction, payload.auctionId() == null ? "CREATED" : "UPDATED");
    }

    private void handleDeleteAuction(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        AuctionIdRequest payload = JsonUtils.fromJsonNode(request.getPayload(), AuctionIdRequest.class);
        Auction auction = auctionService.getAuction(payload.auctionId());
        auctionService.deleteAuction(user, payload.auctionId());
        sendSuccess(session, request, new SimpleResponse("Da xoa phien dau gia"));
        broadcastAuctionChange(auction, "DELETED");
    }

    private void handlePlaceBid(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        PlaceBidRequest payload = JsonUtils.fromJsonNode(request.getPayload(), PlaceBidRequest.class);
        Auction auction = auctionService.placeBid(user, payload.auctionId(), payload.amount(), LocalDateTime.now());
        sendSuccess(session, request, new AuctionDetailResponse(
                AuctionViewMapper.toDetail(auction, auctionService.sellerName(auction))
        ));
        broadcastAuctionChange(auction, "BID_PLACED");
    }

    private void handleConfigureAutoBid(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        ConfigureAutoBidRequest payload = JsonUtils.fromJsonNode(request.getPayload(), ConfigureAutoBidRequest.class);
        Auction auction = auctionService.configureAutoBid(user, payload.auctionId(), payload.maxBid(), payload.increment(),
                LocalDateTime.now());
        sendSuccess(session, request, new AuctionDetailResponse(
                AuctionViewMapper.toDetail(auction, auctionService.sellerName(auction))
        ));
        broadcastAuctionChange(auction, "AUTO_BID_UPDATED");
    }

    private void handleUpdateStatus(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        UpdateAuctionStatusRequest payload = JsonUtils.fromJsonNode(request.getPayload(), UpdateAuctionStatusRequest.class);
        Auction auction = auctionService.updateStatus(user, payload.auctionId(), payload.status(), LocalDateTime.now());
        sendSuccess(session, request, new AuctionDetailResponse(
                AuctionViewMapper.toDetail(auction, auctionService.sellerName(auction))
        ));
        broadcastAuctionChange(auction, "STATUS_UPDATED");
    }

    private void handleListUsers(ClientSession session, ApiMessage request) {
        User user = requireUser(session);
        sendSuccess(session, request, new UsersResponse(authService.listUsers(user).stream()
                .map(UserViewMapper::toRow)
                .toList()));
    }

    private User requireUser(ClientSession session) {
        User user = session.getAuthenticatedUser();
        if (user == null) {
            throw new IllegalStateException("Ban can dang nhap truoc");
        }
        return user;
    }

    private void sendSuccess(ClientSession session, ApiMessage request, Object payload) {
        session.send(new ApiMessage(
                MessageCategory.RESPONSE,
                request.getType(),
                request.getRequestId(),
                true,
                null,
                JsonUtils.toJsonNode(payload)
        ));
    }

    private void sendError(ClientSession session, ApiMessage request, String errorMessage) {
        session.send(new ApiMessage(
                MessageCategory.RESPONSE,
                request.getType(),
                request.getRequestId(),
                false,
                errorMessage == null ? "Co loi xay ra" : errorMessage,
                null
        ));
    }
}