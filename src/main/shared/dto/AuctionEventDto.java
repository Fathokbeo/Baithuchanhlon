package main.shared.dto;

public record AuctionEventDto(String eventName, AuctionSummaryDto summary, AuctionDetailDto detail) {
}
