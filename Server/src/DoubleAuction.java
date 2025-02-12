import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DoubleAuction {

  private Integer buyerCount;
  private Integer sellerCount;
  private HashMap<AuctionUser, AuctionListing> listings;
  private HashMap<AuctionUser, Float> userBids;
  private Comparator<Map.Entry<AuctionUser, AuctionListing>> compareBySellerPrice;

  public DoubleAuction() {
    this.compareBySellerPrice = Comparator.comparing(entry -> entry.getValue().getReservePrice());
    this.buyerCount = 0;
    this.sellerCount = 0;
  }

  public Boolean finalizeDoubleAuction() {
    return (sellerCount > 0 && buyerCount == sellerCount)
        ? true
        : false;
  }

  public void closeDoubleAuction() {

    List<AuctionUser> orderedListings = this.listings.entrySet()
                    .stream()
                    .sorted(compareBySellerPrice.reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

    List<AuctionUser> orderedBids = this.userBids.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());


    int i = 0;
    for (AuctionUser seller: orderedListings) {
      if(listings.get(seller).getReservePrice() <= userBids.get(orderedBids.get(i))) {
        listings.get(seller).setCurrentPrice(userBids.get(orderedBids.get(i)));
      }
      i++;
    }
  }

  public HashMap<AuctionUser, AuctionListing> getListings() { return this.listings; }
  public HashMap<AuctionUser, Float> getUserBids() { return this.userBids; }

  public synchronized void addBuyer(AuctionUser user, Float bid) {
    this.userBids.put(user, bid);
    this.buyerCount++;
  }
  public synchronized void addSeller(AuctionUser user, AuctionListing listing) {
    this.listings.put(user, listing);
    this.sellerCount++;
  }
}
