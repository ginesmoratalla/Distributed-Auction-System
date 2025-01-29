import java.io.Serializable;
import java.util.ArrayList;

public class AuctionListing implements Serializable {

  private AuctionItem item;
  private Integer auctionId;
  private Float startingPrice;
  private Float reservePrice;
  private Float currentPrice;
  private String bestBidUser;
  private Boolean auctionOpen;
  private ArrayList<String> auctionLogs;

  public AuctionListing(Integer auctionId, AuctionItem item,
      Float startingPrice, Float reservePrice) {

    this.item = item;
    this.auctionId = auctionId;
    this.reservePrice = reservePrice;
    this.startingPrice = startingPrice;
    this.currentPrice = 0.0f;
    this.bestBidUser = "None";
    this.auctionOpen = true;
    this.auctionLogs = new ArrayList<String>();
  }

  public ArrayList<String> getAuctionLogs() {
    return this.auctionLogs;
  }

  public Boolean isAcutionOpen() {
    return this.auctionOpen;
  }
  
  public void changeAuctionStatus(Boolean newStat) {
    this.auctionOpen = newStat;
  }

  public String getBestBidUser() {
    return bestBidUser;
  }

  public void setBestBidUser(String bestBidUser) {
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

  public Integer getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(Integer auctionId) {
    this.auctionId = auctionId;
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
