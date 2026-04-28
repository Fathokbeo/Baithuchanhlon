package main.shared.dto;

import main.shared.model.AuctionStatus;

import java.util.UUID;

public record UpdateAuctionStatusRequest(UUID auctionId, AuctionStatus status) {
}

