import java.rmi.RemoteException;
import java.util.Random;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class AuctionServer implements IAuctionSystem {
  private HashMap<String, ArrayList<AuctionListing>> auctionListByType;
  private HashSet<String> userNames;
  private HashMap<Integer, AuctionListing> auctionList;
  private HashMap<Integer, AuctionUser> userList;
  private static Integer globalId = 0;
  private Random random;

  public AuctionServer() {
    super();
    this.auctionList = new HashMap<Integer, AuctionListing>();
    this.auctionListByType = new HashMap<String, ArrayList<AuctionListing>>();
    this.userList = new HashMap<Integer, AuctionUser>();
    this.userNames = new HashSet<String>();
    this.random = new Random();
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
   * Adds aucitoned item listing to server's global list
   *
   */
  private AuctionListing addListing(Integer id, AuctionListing listing) {
    this.auctionList.put(id, listing);
    return listing;
  }

  /*
   * Method for RMI
   *
   * Checks whether a username is used.
   */
  public Boolean userNameExists(String userName) throws RemoteException {
    return this.userNames.contains(userName);
  }

  /*
   * Method for RMI
   *
   * Creates and adds user ID to server's user list.
   * User random ID is created ensuring mutex.
   */
  public synchronized Integer addUser(String userName) throws RemoteException {
    Integer userId = random.nextInt(1000);
    while(this.userList.containsKey(userId)) {
      userId = random.nextInt();
    }
    System.out.println("User " + userName + " got assigned ID " + userId);
    this.userList.put(userId, new AuctionUser(userName, "NO_PASSWORD_YET"));
    this.userNames.add(userName);
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
   * Checks for an existing entry among types of items
   */
  public Boolean itemTypeExists(String typeStr) {
    String lowerCaseString = typeStr.toLowerCase();
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      if (type.getValue().toLowerCase().equals(lowerCaseString)) return true;
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Returs a formatted list of item types (for reverse auctions)
   *
   */
  public String returnItemTypes() throws RemoteException {
    String formattedString = "--- Item Types ---";
    int i = 1;
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      formattedString += i + ". " + type + "\n";
    }
    return formattedString;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public Integer openAuction(Integer userId, String itName, String itType, String itDesc, Integer itCond,
      Float resPrice, Float startPrice) throws RemoteException {

    AuctionItem item = new AuctionItem(assignItemId(), itName, itType, itDesc, itCond);
    AuctionListing listing = addListing(item.getItemId(), new AuctionListing(item, startPrice, resPrice));
    System.out.println("User \""
                        + this.userList.get(userId)
                        + "\" created auction for \""
                        + itName + "\", id: "
                        + item.getItemId());
    addAuctionByItemType(listing);
    return listing.getItem().getItemId();
  }

  /*
   * Add created auction to Auction List by item type.
   */
  public void addAuctionByItemType(AuctionListing listing) {
    this.auctionListByType.get(listing.getItem().getItemType().toLowerCase()).add(listing);
  }

  /*
   * Method for RMI
   *
   * Generate a formatted list containing all auctioned items
   * of a specific type.
   */
  public String retreiveItemsByType(String type) {
    if (!this.auctionListByType.containsKey(type.toLowerCase())) return null;
    String list = "--- All Available " + type + "---\n";
    for (AuctionListing listing: this.auctionListByType.get(type.toLowerCase())) {
      list += listing.getItem().getItemId() + ". \n"
           + "Item condition: " + listing.getItem().getItemCondition() + "\n"
           + "Minimum price seller is willing to accept: " + listing.getReservePrice() + "EUR\n"
           + "Current price: " + listing.getCurrentPrice() + " EUR\n\n";
    }
    return list;
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
   *
   * @return Boolean; whether the bid was placed succesfully (auction ID exists)
   */
  public void placeBid(Integer userId, Integer auctionListingId, Float bid) throws RemoteException {
    AuctionListing auctionListing = this.auctionList.get(auctionListingId);
    auctionListing.appendAuctionLog("[AUCTION LOG] User "
            + this.userList.get(userId)
            + " placed a bid of " + bid
            + " EUR.\n");
    if ((auctionListing.getCurrentPrice() < bid) && (bid >= auctionListing.getStartingPrice())) {
      auctionListing.setCurrentPrice(bid);
      auctionListing.setBestBidUser(this.userList.get(userId).getUserName());
    }
  }

  /*
   * Method for RMI
   *
   * Checks whether an id is amongst existing auctions.
   */
  public Boolean idMatchesExistingItem(Integer id) throws RemoteException {
    return this.auctionList.containsKey(id);
  }

  /*
   * Method for RMI
   *
   * Checks if the price prompted by a buyer exceeds the starting price for item
   */
  public Boolean isPriceAboveMinimum(Integer itemId, Float price) throws RemoteException {
    if (!this.auctionList.containsKey(itemId)) return true;
    return this.auctionList.get(itemId).getStartingPrice().compareTo(price) <= 0;
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
      AuctionServer s = new AuctionServer();
      String name = "LZSCC.311 auction server";
      IAuctionSystem remoteObject = (IAuctionSystem) UnicastRemoteObject.exportObject(s, 0);
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(name, remoteObject);

      System.out.println("Server ready");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
