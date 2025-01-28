import java.io.Serializable;

public class AuctionListing implements Serializable {

  private AuctionItem item;
  private Integer auctionId;
  private Float startingPrice;
  private Float reservePrice;
  private Float currentPrice;
  private String bestBidUser;

  public AuctionListing(Integer auctionId, AuctionItem item,
      Float startingPrice, Float reservePrice) {

    this.item = item;
    this.auctionId = auctionId;
    this.reservePrice = reservePrice;
    this.startingPrice = startingPrice;
    this.currentPrice = startingPrice;
    this.bestBidUser = "None";
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
