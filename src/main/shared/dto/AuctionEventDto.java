package main.shared.dto;

public record AuctionEventDto(
        String eventName,
        AuctionSummaryDto summary,
        AuctionDetailDto detail,
        SessionUserDto currentUser
) {
    public AuctionEventDto(String eventName, AuctionSummaryDto summary, AuctionDetailDto detail) {
        this(eventName, summary, detail, null);
    }
}
