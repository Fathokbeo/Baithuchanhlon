package main.shared.dto;

import main.shared.model.BidSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidTransactionDto(
        UUID bidId,
        String bidderName,
        BigDecimal amount,
        LocalDateTime timestamp,
        BidSource source
) {
}
