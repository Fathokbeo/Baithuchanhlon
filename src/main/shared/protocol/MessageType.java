package main.shared.protocol;

public enum MessageType {
    LOGIN,
    REGISTER,
    LIST_AUCITONS,
    GET_AUCTION_DETAIL,
    UPSERT_AUCTION,
    DELETE_AUCTION,
    PLACE_BID,
    UPDATE_AUCTION_STATUS,
    LIST_USERS,
    LIST_MY_AUCTIONS,
    AUCTION_CHANGED
}
