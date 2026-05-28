package main.shared.dto;

public record AuctionDetailResponse(AuctionDetailDto auction, SessionUserDto currentUser) {
    public AuctionDetailResponse(AuctionDetailDto auction) {
        this(auction, null);
    }
}
