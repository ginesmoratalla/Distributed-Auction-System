import java.io.Serializable;

public class AuctionListing implements Serializable {

  private AuctionItem item;
  private Float startingPrice;
  private Float reservePrice;
  private Float currentPrice;
  private String bestBidUser;
  private Boolean auctionOpen;
  private String auctionLogs;

  /**
   * Auction listing for forward, reverse and double auction
   *
   * Includes item, listing prices and logs
   *
   */
  public AuctionListing(AuctionItem item, Float startingPrice, Float reservePrice) {

    this.item = item;
    this.reservePrice = reservePrice;
    this.startingPrice = startingPrice;
    this.currentPrice = 0.0f;
    this.bestBidUser = null;
    this.auctionLogs = "--- AUCTION LOGS ---\n";
    this.auctionOpen = true; // unused for the moment
  }

  public AuctionListing() {}
  public String getAuctionLogs() { return this.auctionLogs; }
  public void appendAuctionLog(String newLog) { this.auctionLogs += newLog; }
  public Boolean isAcutionOpen() { return this.auctionOpen; }
  public void changeAuctionStatus(Boolean newStat) { this.auctionOpen = newStat; }
  public String getBestBidUser() { return bestBidUser; }
  public void setBestBidUser(String bestBidUser) { this.bestBidUser = bestBidUser; }
  public Float getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(Float currentPrice) { this.currentPrice = currentPrice; }
  public AuctionItem getItem() { return item; }
  public void setItem(AuctionItem item) { this.item = item; }
  public Float getStartingPrice() { return startingPrice; }
  public void setStartingPrice(Float startingPrice) { this.startingPrice = startingPrice; }
  public void setReservePrice(Float reservePrice) { this.reservePrice = reservePrice; }
  public Float getReservePrice() { return reservePrice; }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    AuctionListing other = (AuctionListing) obj;
    return item.getItemId().equals(other.item.getItemId());
  }
}
