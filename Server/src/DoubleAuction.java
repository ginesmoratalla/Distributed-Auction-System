import java.util.HashMap;

public class DoubleAuction {

  private final int MAX_USER_COUNT = 3;

  private HashMap<AuctionUser, AuctionListing> listings;
  private HashMap<AuctionUser, Float> userBids;

  public DoubleAuction() {}

  public void addBidder(AuctionUser user, Float bid) {
    this.userBids.put(user, bid);
  }

  public Boolean finalizeDoubleAuction() {
    return (this.listings.size() == MAX_USER_COUNT &&
            this.userBids.size() == MAX_USER_COUNT)
        ? true
        : false;
  }

  public void closeDoubleAuction() {
  }

  public HashMap<AuctionUser, AuctionListing> getListings() { return this.listings; }

  public HashMap<AuctionUser, Float> getUserBids() { return this.userBids; }
}
