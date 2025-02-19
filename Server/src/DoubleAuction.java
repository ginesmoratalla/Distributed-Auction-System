import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DoubleAuction {

  private String auctionItemType;
  private Integer buyerCount;
  private Integer sellerCount;

  private HashMap<Integer, DoubleAuctionPair> listings;
  private HashMap<Integer, DoubleAuctionPair> userBids;
  private ArrayList<Integer> succesfulSellers;
  private ArrayList<Integer> succesfulBuyers;
  private Random randomGenerator;
  private Comparator<Map.Entry<Integer, DoubleAuctionPair>> compareBySellerPrice;

  public DoubleAuction(String itemType) {
    this.listings = new HashMap<Integer, DoubleAuctionPair>();
    this.compareBySellerPrice = Comparator.comparing(entry -> entry.getValue().getListing().getReservePrice());
    this.userBids = new HashMap<Integer, DoubleAuctionPair>();
    this.succesfulBuyers = new ArrayList<Integer>();
    this.succesfulSellers = new ArrayList<Integer>();
    this.auctionItemType = itemType.toLowerCase();
    this.randomGenerator = new Random();
    this.buyerCount = 0;
    this.sellerCount = 0;
  }

  public Boolean finalizeDoubleAuction() {
    System.out.println("> Seller count (double auction " + this.auctionItemType + "): " + this.sellerCount);
    System.out.println("> Buyer count (double auction " + this.auctionItemType + "):" + this.buyerCount);
    return (sellerCount > 1 && buyerCount == sellerCount)
        ? true
        : false;
  }

  public synchronized void closeDoubleAuction() {

    List<Integer> orderedListings = this.listings.entrySet()
                    .stream()
                    .sorted(compareBySellerPrice.reversed())
                    .map(Map.Entry::getKey)
                    .toList();

    List<Integer> orderedBids = this.userBids.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getValue().getBid()))
                    .map(Map.Entry::getKey)
                    .toList();

    try {
      System.out.println("\n[DOUBLE AUCTION INFO]");
      System.out.println("+ Item type: " + this.auctionItemType);
      System.out.println("+ Listings: " + listings.size());
      System.out.println("+ Bids: " + userBids.size());
      int i = 0;
      for (Integer sellerId: orderedListings) {
        // A bid matches (>=) a reserve price from a seller
        if(listings.get(sellerId).getListing().getReservePrice() <= userBids.get(orderedBids.get(i)).getBid()) {
          listings.get(sellerId).getListing().setCurrentPrice(userBids.get(orderedBids.get(i)).getBid());
          listings.get(sellerId).getListing().setBestBidUser(userBids.get(orderedBids.get(i)).getUser().getUserName());
          this.succesfulSellers.add(sellerId);
          this.succesfulBuyers.add(orderedBids.get(i));
          this.sellerCount--;
          this.buyerCount--;
        }
        System.out.println("> Item: " + listings.get(sellerId).getListing().getItem().getItemTitle()
                            + " | Seller: " + listings.get(sellerId).getUser().getUserName()
                            + " | Listed for: " + listings.get(sellerId).getListing().getReservePrice() + " EUR"
                            + " | Buyer: " + listings.get(sellerId).getListing().getBestBidUser()
                            + " | Price: " + ((listings.get(sellerId).getListing().getCurrentPrice() > 0.0f)
                                      ? listings.get(sellerId).getListing().getCurrentPrice() + " EUR"
                                      : "Not sold"));
        i++;
      }
      System.out.println("\n");
      removeSuccesfulAuctions();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  private void removeSuccesfulAuctions() {
    for (int i=0; i < succesfulBuyers.size(); i++) {
      this.listings.remove(succesfulSellers.get(i));
      this.userBids.remove(succesfulBuyers.get(i));
    }
    this.succesfulSellers.clear();
    this.succesfulBuyers.clear();
  }

  public HashMap<Integer, DoubleAuctionPair> getListings() { return this.listings; }
  public HashMap<Integer, DoubleAuctionPair> getUserBids() { return this.userBids; }

  public synchronized void addBuyer(AuctionUser user, Float bid) {
    Integer buyerDoubleAuctionId = this.randomGenerator.nextInt(1000);
    while(this.userBids.containsKey(buyerDoubleAuctionId)) {
      buyerDoubleAuctionId = this.randomGenerator.nextInt();
    }
    this.userBids.put(buyerDoubleAuctionId, new DoubleAuctionPair(user, bid));
    this.buyerCount++;
  }
  public synchronized void addSeller(AuctionUser user, AuctionListing listing) {
    Integer sellerDoubleAuctionId = this.randomGenerator.nextInt(1000);
    while(this.listings.containsKey(sellerDoubleAuctionId)) {
      sellerDoubleAuctionId = this.randomGenerator.nextInt();
    }
    this.listings.put(sellerDoubleAuctionId, new DoubleAuctionPair(user, listing));
    this.sellerCount++;
  }
}
