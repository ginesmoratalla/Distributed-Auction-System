public class DoubleAuctionPair {

  private AuctionUser user;
  private AuctionListing listing;
  private Float bid;

  /*
   * Helper class to handle returns for double auction
   */


  /*
   * Constructor for seller
   */
  public DoubleAuctionPair(AuctionUser user, AuctionListing listing) {
    this.listing = listing;
    this.user = user;
  }

  /*
   * Constructor for buyer 
   */
  public DoubleAuctionPair(AuctionUser user, Float bid) {
    this.bid = bid;
    this.user = user;
  }

  public AuctionUser getUser() { return user; }
  public AuctionListing getListing() { return listing; }
  public Float getBid() { return bid; }


}
