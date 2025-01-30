import java.io.Serializable;
import java.util.ArrayList;

public class AuctionListing implements Serializable {

  private AuctionItem item;
  private Float startingPrice;
  private Float reservePrice;
  private Float currentPrice;
  private AuctionUser bestBidUser;
  private Boolean auctionOpen;
  private String auctionLogs;

  public AuctionListing(AuctionItem item,
      Float startingPrice, Float reservePrice) {

    this.item = item;
    this.reservePrice = reservePrice;
    this.startingPrice = startingPrice;
    this.currentPrice = 0.0f;
    this.bestBidUser = null;
    this.auctionOpen = true; // unused for the moment
    this.auctionLogs = "--- AUCTION LOGS ---\n";
  }

  public String getAuctionLogs() {
    return this.auctionLogs;
  }

  public void appendAuctionLog(String newLog) {
    this.auctionLogs += newLog;
  }

  public Boolean isAcutionOpen() {
    return this.auctionOpen;
  }
  
  public void changeAuctionStatus(Boolean newStat) {
    this.auctionOpen = newStat;
  }

  public AuctionUser getBestBidUser() {
    return bestBidUser;
  }

  public void setBestBidUser(AuctionUser bestBidUser) {
    this.bestBidUser = bestBidUser;
  }

  public Float getCurrentPrice() {
	return currentPrice;
  }

  public void setCurrentPrice(Float currentPrice) {
    this.currentPrice = currentPrice;
  }

  public AuctionItem getItem() {
      return item;
  }

  public void setItem(AuctionItem item) {
    this.item = item;
  }

  public Float getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(Float startingPrice) {
    this.startingPrice = startingPrice;
  }


  public void setReservePrice(Float reservePrice) {
    this.reservePrice = reservePrice;
  }

  public Float getReservePrice() {
    return reservePrice;
  }
}
