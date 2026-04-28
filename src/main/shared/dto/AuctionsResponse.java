package main.shared.dto;

import java.util.List;

public record AuctionsResponse(List<AuctionSummaryDto> auctions) {
}
