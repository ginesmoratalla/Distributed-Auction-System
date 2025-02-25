import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

public class DoubleAuction {

  private String auctionItemType;
  private Integer buyerCount;
  private Integer sellerCount;

  private HashMap<Integer, DoubleAuctionPair> listings;
  private HashMap<Integer, DoubleAuctionPair> userBids;
  private Random randomGenerator;
  private Comparator<Map.Entry<Integer, DoubleAuctionPair>> compareBySellerPrice;

  public DoubleAuction(String itemType) {
    this.listings = new HashMap<Integer, DoubleAuctionPair>();
    this.compareBySellerPrice = Comparator.comparing(entry -> entry.getValue().getListing().getReservePrice());
    this.userBids = new HashMap<Integer, DoubleAuctionPair>();
    this.auctionItemType = itemType.toLowerCase();
    this.randomGenerator = new Random();
    this.buyerCount = 0;
    this.sellerCount = 0;
  }

  /*
   * Check if auction needs to be closed
   */
  public Boolean finalizeDoubleAuction() {
    System.out.println("> Seller count (double auction " + this.auctionItemType + "): " + this.sellerCount);
    System.out.println("> Buyer count (double auction " + this.auctionItemType + "):" + this.buyerCount);
    return (sellerCount > 1 && buyerCount == sellerCount)
        ? true
        : false;
  }

  /*
   * Matches buyers with sellers
   */
  public synchronized HashMap<Integer, HashMap<Integer, String>> closeDoubleAuction() {
    HashMap<Integer, HashMap<Integer, String>> returnMap = new HashMap<Integer, HashMap<Integer, String>>();

    // Sort sellers from highest to lowest minimum price
    List<Integer> orderedListings = this.listings.entrySet()
                    .stream()
                    .sorted(compareBySellerPrice.reversed())
                    .map(Map.Entry::getKey)
                    .toList();

    // Sort buyers from lowest to highest bid
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
        AuctionItem soldItem = listings.get(sellerId).getListing().getItem();
        String soldString = null;
        String boughtString = null;
        if(listings.get(sellerId).getListing().getReservePrice() <= userBids.get(orderedBids.get(i)).getBid()) {
          listings.get(sellerId).getListing().setCurrentPrice(userBids.get(orderedBids.get(i)).getBid());
          listings.get(sellerId).getListing().setBestBidUser(userBids.get(orderedBids.get(i)).getUser().getUserName());

          soldString = "> Your double auction item was sold: "
                              + soldItem.getItemTitle()
                              + " (" + soldItem.getItemType() + ")"
                              + " || ID: " + soldItem.getItemId()
                              + "\n> Sold for: "
                              + listings.get(sellerId).getListing().getCurrentPrice()
                              + " EUR\n"
                              + "> Sold to: " + listings.get(sellerId).getListing().getBestBidUser();

          boughtString = "> Your bid for "
                              + soldItem.getItemTitle()
                              + " (" + soldItem.getItemType() + ")"
                              + " was succesful\n> Bid: "
                              + listings.get(sellerId).getListing().getCurrentPrice()
                              + " EUR\n"
                              + "> Seller: " + listings.get(sellerId).getUser().getUserName();


        } else {
          soldString = "> Your double auction item was NOT sold: "
                              + soldItem.getItemTitle()
                              + " (" + soldItem.getItemType() + ")"
                              + " || ID: " + soldItem.getItemId()
                              + "\n> Listed for: "
                              + listings.get(sellerId).getListing().getReservePrice();

          boughtString = "> Your bid for item "
                              + soldItem.getItemType()
                              + " was NOT succesful\n> Bid: "
                              + userBids.get(orderedBids.get(i)).getBid()
                              + " EUR\n";
        }
        HashMap<Integer, String> soldMap = new HashMap<Integer, String>();
        soldMap.put(listings.get(sellerId).getUser().getUserId(), soldString);
        returnMap.put(sellerId, soldMap);
        HashMap<Integer, String> boughtMap = new HashMap<Integer, String>();
        boughtMap.put(userBids.get(orderedBids.get(i)).getUser().getUserId(), boughtString);
        returnMap.put(orderedBids.get(i), boughtMap);
        this.sellerCount--;
        this.buyerCount--;
        System.out.println("> Item: " + soldItem.getItemTitle()
                            + " | Seller: " + listings.get(sellerId).getUser().getUserName()
                            + " | Listed for: " + listings.get(sellerId).getListing().getReservePrice() + " EUR"
                            + " | Buyer: " + listings.get(sellerId).getListing().getBestBidUser()
                            + " | Price: " + ((listings.get(sellerId).getListing().getCurrentPrice() > 0.0f)
                                      ? listings.get(sellerId).getListing().getCurrentPrice() + " EUR"
                                      : "Not sold"));
        i++;
      }
      System.out.println("\n");
      removeAuctions();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return returnMap;
  }
  private void removeAuctions() {
    this.listings.clear();
    this.userBids.clear();
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
