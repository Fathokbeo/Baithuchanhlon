package main.shared.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Auction extends Entity {
    private final Item item;
    private final UUID sellerid;
    private BigDecimal currentprice;
    private UUID winnerBidderId;
    private String winnerBiddername;
    private UUID leadingBidderId;
    private String leadingBiddername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int extentionCount;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;

    public Auction(
            UUID id,
            LocalDateTime createAt,
            LocalDateTime updateAt,
            Item item,
            UUID sellerid,
            BigDecimal currentprice,
            UUID winnerBidderId,
            String winnerBiddername,
            UUID leadingBidderId,
            String leadingBiddername,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int extentionCount,
            List<BidTransaction> bidHistory,
            AuctionStatus status
    ){
        super(id,createAt,updateAt);
        this.item = Objects.requireNonNull(item,"item");
        this.sellerid = Objects.requireNonNull(sellerid,"sellerId");
        this.currentprice = Objects.requireNonNull(currentprice,"currentprice");
        this.winnerBidderId = winnerBidderId;
        this.winnerBiddername = winnerBiddername;
        this.leadingBidderId = leadingBidderId;
        this.leadingBiddername = leadingBiddername;
        this.startTime = Objects.requireNonNull(startTime,"startTime");
        this.endTime = Objects.requireNonNull(endTime,"endTime");
        this.extentionCount = extentionCount;
        this.bidHistory = new ArrayList<>(Objects.requireNonNull(bidHistory,"bidHistory"));
        this.status = Objects.requireNonNull(status,"status");
    }

    public Item getItem() {
        return item;
    }

    public UUID getSellerid() {
        return sellerid;
    }

    public BigDecimal getCurrentprice() {
        return currentprice;
    }

    public UUID getWinnerBidderId() {
        return winnerBidderId;
    }

    public String getWinnerBiddername() {
        return winnerBiddername;
    }

    public UUID getLeadingBidderId() {
        return leadingBidderId;
    }

    public String getLeadingBiddername() {
        return leadingBiddername;
    }

    public void setLeader(UUID leadingBidderId, String leadingBiddername, BigDecimal amount, LocalDateTime timestamp){
        this.leadingBidderId = leadingBidderId;
        this.leadingBiddername = leadingBiddername;
        this.currentprice = amount;
        touch(timestamp);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime, LocalDateTime timestamp){
        this.startTime = startTime;
        touch(timestamp);
    }

    public int getExtentionCount() {
        return extentionCount;
    }

    public void increaseExtensioncount(){
        extentionCount++;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime, LocalDateTime timestamp) {
        this.endTime = endTime;
        touch(timestamp);
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void addBid(BidTransaction bid, LocalDateTime timestamp){
        bidHistory.add(Objects.requireNonNull(bid,"bid"));
        setLeader(bid.getBidderId(),bid.getBidderName(),bid.getAmount(),timestamp);
    }

    public AuctionStatus getStatus(){
        return status;
    }

    public void setStatus(AuctionStatus status, LocalDateTime timestamp){
        this.status = status;
        touch(timestamp);
    }

    public boolean canEdit(){
        return bidHistory.isEmpty() && status != AuctionStatus.FINISHED && status != AuctionStatus.PAID;
    }
// ham kiem tra trang thai cua phien dau gia
    public boolean refreshLifecycle(LocalDateTime now){
        boolean changed = false;

        if(status ==  AuctionStatus.CANCELED || status == AuctionStatus.PAID){
            return false;
        }

        if(status == AuctionStatus.OPEN && !now.isBefore(startTime)){
            status = AuctionStatus.RUNNING;
            changed = true;
        }

        if(status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING && !now.isBefore(endTime)){
            status = AuctionStatus.FINISHED;
            changed = true;
        }

        if(changed){
            touch(now);
        }

        return changed;
    }

    public void markPaid(LocalDateTime timestamp){
        if(status != AuctionStatus.FINISHED){
            throw new IllegalStateException("only finished auction can be marked as paid");
        }

        status = AuctionStatus.PAID;
        touch(timestamp);
    }

    public void cancel(LocalDateTime timestamp){
        if(status != AuctionStatus.PAID){
            throw new IllegalStateException("paid auction could not be canceled");
        }

        status = AuctionStatus.CANCELED;
        touch(timestamp);
    }

}
