import java.rmi.RemoteException;
import java.util.Random;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements IAuctionSystem {
  private HashMap<Integer, AuctionListing> auctionList;
  private HashMap<Integer, AuctionUser> userList;
  private static Integer globalId = 0;
  private Random random;

  public Server() {

    super();
    this.auctionList = new HashMap<Integer, AuctionListing>();
    this.userList = new HashMap<Integer, AuctionUser>();
    this.random = new Random();

    // PLACEHOLDER ITEMS
    AuctionItem placeholder1 = new AuctionItem(assignItemId(), "Leather Hat", "Hat that belonged to Juozas", 3);
    addListing(placeholder1.getItemId(), new AuctionListing(placeholder1, 10.5f, 50f));

    AuctionItem placeholder2 = new AuctionItem(assignItemId(), "Gloves", "Pair of Gloves", 2);
    addListing(placeholder2.getItemId(), new AuctionListing(placeholder2, 7.5f, 20f));

    AuctionItem placeholder3 = new AuctionItem(assignItemId(), "Oakley Windbreaker", "Windbreaker from 2006.", 5);
    addListing(placeholder3.getItemId(), new AuctionListing(placeholder3, 73f, 120f));
  }

  /*
   * Adds item to server's global list
   *
   * Global ID is updated ensuring mutex.
   */
  private synchronized Integer assignItemId() {
    return globalId++;
  }

  /*
   * Adds listing to server's global list
   */
  private AuctionListing addListing(Integer id, AuctionListing listing) {
    this.auctionList.put(id, listing);
    return listing;
  }

  /*
   * Creates and adds user ID to server's user list
   *
   * User random ID is created ensuring mutex.
   */
  public synchronized Integer addUser(String userName) throws RemoteException {
    Integer userId = random.nextInt(1000);
    while(this.userList.containsKey(userId)) {
      userId = random.nextInt();
    }
    System.out.println("User " + userName + " got assigned ID " + userId);
    this.userList.put(userId, new AuctionUser(userName, userId));
    return userId;
  }

  /*
   * Method for RMI
   *
   * Removes listing from server's global list
   */
  public AuctionListing closeAuction(Integer userId, Integer listingId) throws RemoteException {
    if (this.auctionList.containsKey(listingId)) {
      AuctionListing returnListing = this.auctionList.get(listingId);
      this.auctionList.remove(listingId);
      return returnListing;
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public Integer openAuction(Integer userId, String itName, String itDesc, Integer itCond,
      Float resPrice, Float startPrice) throws RemoteException {

    AuctionItem item = new AuctionItem(assignItemId(), itName, itDesc, itCond);
    AuctionListing listing = addListing(item.getItemId(), new AuctionListing(item, startPrice, resPrice));
    System.out.println("User \"" + this.userList.get(userId).getUserName() + "\" created auction for \"" + itName + "\", id: " + item.getItemId());
    return listing.getItem().getItemId();
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpec(int itemId, String clientId)
      throws RemoteException {
    System.out.println("Client " + clientId + " requested item " + itemId);

    if (this.auctionList.containsKey(itemId)) {
      System.out.println("Succesfully retreived item with ID: " + itemId);
      AuctionItem selectedItem = this.auctionList.get(itemId).getItem();
      return selectedItem;
    }
    System.out.println("Item with itemId " + itemId + " does not exist");
    return null;
  }

  /*
   * Method for RMI
   *
   * Place a bid.
   * Note: Checking wether the auction listing exists
   *       has happened before this method is invoked.
   */
  public void placeBid(Integer userId, Integer auctionListingId, Float bid) throws RemoteException {
    AuctionListing auctionListing = this.auctionList.get(auctionListingId);
    auctionListing.getAuctionLogs().add("[AUCTION LOG] User "
                                        + this.userList.get(userId).getUserName()
                                        + " placed a bid of " + bid
                                        + " EUR.");
    if ((auctionListing.getCurrentPrice() < bid) && (bid >= auctionListing.getStartingPrice())) {
      auctionListing.setCurrentPrice(bid);
      auctionListing.setBestBidUser(this.userList.get(userId));
    }
  }

  public String getAuctionedItems() throws RemoteException {
    String introStr = "\n===== ALL AVAILABLE AUCTIONED ITEMS ======\n";
    String strToStd = introStr;
    for (HashMap.Entry<Integer, AuctionListing> entry: this.auctionList.entrySet()) {
      strToStd += "ID: " + entry.getKey()
                + " || item: " + entry.getValue().getItem().getItemTitle()
                + " || starting price: " + entry.getValue().getStartingPrice() + " EUR"
                + " || current best bid: "
                + ((entry.getValue().getCurrentPrice() == 0.0f) ? "No best bid yet" : entry.getValue().getCurrentPrice())
                + "\n";
    }
    strToStd += "=".repeat(introStr.length()) + "\n";
    return strToStd;
  }

  /*
   * Server initiation.
   */
  public static void main(String[] args) {
    try {
      Server s = new Server();
      String name = "LZSCC.311 auction server";
      IAuctionSystem stub = (IAuctionSystem) UnicastRemoteObject.exportObject(s, 0);
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(name, stub);

      System.out.println("Server ready");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
