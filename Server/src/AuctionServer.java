// RMI
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


// Data structs
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


// Misc imports
import java.util.Random;

public class AuctionServer implements IAuctionSystem {

  private HashMap<String, HashMap<Integer, AuctionListing>> auctionList;
  private HashMap<String, DoubleAuction> doubleAuctionList;
  private static Integer globalId = 0;

  private HashMap<Integer, AuctionUser> userList;
  private HashSet<String> userNames;

  private Random random;

  public AuctionServer() {
    super();
    this.auctionList = new HashMap<String, HashMap<Integer, AuctionListing>>();
    this.doubleAuctionList = new HashMap<String, DoubleAuction>();
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
  private AuctionListing addListing(AuctionListing listing) {
    String itemType = listing.getItem().getItemType().toLowerCase();
    this.auctionList
      .computeIfAbsent(itemType, k -> new HashMap<Integer, AuctionListing>())
      .put(listing.getItem().getItemId(), listing);

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
    System.out.println("> User " + userName + " got assigned ID " + userId);
    this.userList.put(userId, new AuctionUser(userId, userName, "NO_PASSWORD_YET"));
    this.userNames.add(userName);
    return userId;
  }

  /*
   * Method for RMI
   */
  public void addBuyerForDoubleAuction(Integer userId, String itemType, Float bid) throws RemoteException {
    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(itemType.toLowerCase(), k -> new DoubleAuction());
    doubleAuciton.addBuyer(this.userList.get(userId), bid);

    if (this.doubleAuctionList.get(itemType.toLowerCase()).finalizeDoubleAuction()) {
      doubleAuciton.closeDoubleAuction();
      this.doubleAuctionList.remove(itemType);
    }
  }

  /*
   * Method for RMI
   */
  public void addSellerForDoubleAuction(Integer userId, String itemName, String itemType, String itemDesc, Integer itemCond, Float resPrice, Float startPrice) throws RemoteException {
    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(itemType.toLowerCase(), k -> new DoubleAuction());
    AuctionItem item = new AuctionItem(assignItemId(), itemName, itemType, itemDesc, itemCond);
    AuctionListing listing = new AuctionListing(item, startPrice, resPrice);
    doubleAuciton.addSeller(this.userList.get(userId), listing);

    if (this.doubleAuctionList.get(itemType.toLowerCase()).finalizeDoubleAuction()) {
      doubleAuciton.closeDoubleAuction();
      this.doubleAuctionList.remove(itemType);
    }
  }

  /*
   * Method for RMI
   *
   * Removes auction listing from server's global list
   */
  public AuctionListing closeAuction(Integer listingId, String itemType, Integer userId) throws RemoteException {
    String itemLowerCase = itemType.toLowerCase();
    if (this.auctionList.containsKey(itemLowerCase) && this.auctionList.get(itemLowerCase).containsKey(listingId)) {
      AuctionListing returnListing = this.auctionList.get(itemLowerCase).get(listingId);
      this.auctionList.get(itemLowerCase).remove(listingId);
      System.out.println("> User "
                        + this.userList.get(userId).getUserName()
                        + " closed auction with ID: " + listingId
      );
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
  public String retrieveItemTypes() throws RemoteException {
    String formattedString = "--- Item Types ---\n";
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      formattedString += "> " + type.getValue() + "\n";
    }
    formattedString += "\nRESPECT SPACES WHEN TYPING ITEM TYPE\n";
    return formattedString;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public AuctionListing openAuction(Integer userId, String itName, String itType, String itDesc, Integer itCond,
      Float resPrice, Float startPrice) throws RemoteException {

    AuctionItem item = new AuctionItem(assignItemId(), itName, itType, itDesc, itCond);
    AuctionListing listing = addListing(new AuctionListing(item, startPrice, resPrice));
    System.out.println("> User "
                        + this.userList.get(userId).getUserName()
                        + " created auction for \""
                        + itName + "\", id: "
                        + item.getItemId());
    return listing;
  }

  /*
   * Method for RMI
   *
   * Generate a formatted list containing all auctioned items
   * of a specific type.
   */
  public String retrieveItemsByType(String type) throws RemoteException {
    if (!this.auctionList.containsKey(type.toLowerCase())
          || this.auctionList.get(type.toLowerCase()).size() == 0) return null;

    String list = "--- All Available " + type + "---\n";
    for (Map.Entry<Integer,AuctionListing> listing: this.auctionList.get(type.toLowerCase()).entrySet()) {
      list += "ID: " + listing.getKey() + "\n"
           + "Item condition: " + listing.getValue().getItem().getItemCondition() + "\n"
           + "Current price: "  + ((listing
                                          .getValue()
                                          .getCurrentPrice()
                                          .compareTo(listing.getValue().getStartingPrice()) < 0)
                                              ? listing.getValue().getStartingPrice() : listing.getValue().getCurrentPrice())
                                + " EUR\n\n";
    }
    return list;
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpec(Integer itemId, String clientId)
      throws RemoteException {
    System.out.println("> User " + clientId + " requested item " + itemId);

    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry : this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(itemId)) {
        System.out.println("> Succesfully retreived item with ID: " + itemId);
        AuctionItem selectedItem = entry.getValue().get(itemId).getItem();
        return selectedItem;
      }
    }

    System.out.println("> Item with itemId " + itemId + " does not exist");
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
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry : this.auctionList.entrySet()) {
      if(entry.getValue().containsKey(auctionListingId)) {
        AuctionListing auctionListing = entry.getValue().get(auctionListingId);
        auctionListing.appendAuctionLog("[AUCTION LOG] User "
                + this.userList.get(userId).getUserName()
                + " placed a bid of " + bid
                + " EUR.\n");
        if ((auctionListing.getCurrentPrice() < bid) && (bid >= auctionListing.getStartingPrice())) {
          auctionListing.setCurrentPrice(bid);
          auctionListing.setBestBidUser(this.userList.get(userId).getUserName());
        }
      break;
      }
    }

  }

  /*
   * Method for RMI
   *
   * Checks whether an id is amongst existing auctions.
   */
  public Boolean idMatchesExistingItem(Integer id) throws RemoteException {
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry : this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(id)) return true;
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Checks if the price prompted by a buyer exceeds the starting price for item
   */
  public Boolean isBidPriceAcceptable(Integer listingId, Float price) throws RemoteException {
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry : this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(listingId)) {
        if (entry.getValue().get(listingId).getCurrentPrice() > 0.0f) {
          return entry.getValue().get(listingId).getCurrentPrice().compareTo(price) < 0;
        } else {
          return entry.getValue().get(listingId).getStartingPrice().compareTo(price) <= 0;
        }
      }
    }
    return true;
  }

  public String getAuctionedItems() throws RemoteException {
    if (this.auctionList.isEmpty() || this.auctionList.values().stream().allMatch(Map::isEmpty)) return null;
    String introStr = "\n===== ALL AVAILABLE AUCTIONED ITEMS ======\n";
    String strToStd = introStr;
    for(HashMap.Entry<String, HashMap<Integer, AuctionListing>> listing : this.auctionList.entrySet()) {
      for(HashMap.Entry<Integer, AuctionListing> entry : this.auctionList.get(listing.getKey()).entrySet()) {

        strToStd += "ID: " + entry.getValue().getItem().getItemId()
                  + " || item: " + entry.getValue().getItem().getItemTitle()
                  + " || starting price: " + entry.getValue().getStartingPrice() + " EUR"
                  + " || current best bid: "
                  + ((entry.getValue().getCurrentPrice() == 0.0f) ? "No best bid yet" : entry.getValue().getCurrentPrice())
                  + "\n";
      }
    }
    strToStd += "=".repeat(introStr.length() - 2) + "\n";
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
