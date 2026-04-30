package main.server.service;


import main.shared.dto.AutoBidDto;
import main.shared.dto.AuctionDetailDto;
import main.shared.dto.AuctionSummaryDto;
import main.shared.dto.BidTransactionDto;
import main.shared.dto.PricePointDto;
import main.shared.model.Auction;

import java.util.ArrayList;
import java.util.List;

public final class AuctionViewMapper {
    private AuctionViewMapper() {
    }

    public static AuctionSummaryDto toSummary(Auction auction, String sellerName) {
        return new AuctionSummaryDto(
                auction.getId(),
                auction.getItem().getName(),
                auction.getItem().getType(),
                auction.getItem().getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getStatus(),
                auction.getStartTime(),
                auction.getEndTime(),
                sellerName,
                auction.getLeadingBidderName()
        );
    }

    public static AuctionDetailDto toDetail(Auction auction, String sellerName) {
        List<BidTransactionDto> bidHistory = auction.getBidHistory().stream()
                .map(bid -> new BidTransactionDto(
                        bid.getId(),
                        bid.getBidderName(),
                        bid.getAmount(),
                        bid.getBidTime(),
                        bid.getSource()
                ))
                .toList();
        List<PricePointDto> priceHistory = new ArrayList<>();
        priceHistory.add(new PricePointDto(auction.getStartTime(), auction.getItem().getStartingPrice()));
        auction.getBidHistory().forEach(bid -> priceHistory.add(new PricePointDto(bid.getBidTime(), bid.getAmount())));
        List<AutoBidDto> autoBids = auction.getAutoBidConfigs().stream()
                .filter(config -> config.isActive())
                .map(config -> new AutoBidDto(
                        config.getBidderName(),
                        config.getMaxBid(),
                        config.getIncrement(),
                        config.getRegisteredAt()
                ))
                .toList();
        return new AuctionDetailDto(
                auction.getId(),
                auction.getSellerId(),
                sellerName,
                auction.getItem().getName(),
                auction.getItem().getDescription(),
                auction.getItem().printInfo(),
                auction.getItem().getType(),
                auction.getItem().getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getStatus(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getLeadingBidderName(),
                auction.getWinnerBidderName(),
                auction.getExtensionCount(),
                bidHistory,
                priceHistory,
                autoBids
        );
    }
}