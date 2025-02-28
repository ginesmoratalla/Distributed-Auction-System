// RMI
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

// Data structs
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class AuctionServerBackend {

  private HashMap<String, HashMap<Integer, AuctionListing>> auctionList = null;
  private HashMap<String, DoubleAuction> doubleAuctionList = null;
  private HashMap<Integer, AuctionUser> userList = null;

  private int requestCount;
  private static Integer globalId = null;
  private final int DISPATCHER_TIMEOUT = 1000;

  private JChannel groupChannel;
  private RpcDispatcher dispatcher;

  public AuctionServerBackend() {

    this.groupChannel = GroupUtils.connect();
    if (this.groupChannel == null) { System.exit(1); }
    this.dispatcher = new RpcDispatcher(this.groupChannel, this);

    this.auctionList = new HashMap<String, HashMap<Integer, AuctionListing>>();
    this.doubleAuctionList = new HashMap<String, DoubleAuction>();
    this.userList = new HashMap<Integer, AuctionUser>();
    this.requestCount = 0;
    try {
      syncBackendState();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Adds item to server's global list
   * @return Global ID
   */
  private Integer assignItemId() { return globalId++; }

  /*
   * Adds aucitoned item listing to server's global list
   */
  private AuctionListing addListing(AuctionListing listing) {
    String itemType = listing.getItem().getItemType().toLowerCase();
    this.auctionList
        .computeIfAbsent(itemType, k -> new HashMap<Integer, AuctionListing>())
        .put(listing.getItem().getItemId(), listing);
    return listing;
  }


  public Boolean proposedIdExistsBackend(Integer proposedId) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for proposedIdExists() | total requests: %d\n", this.requestCount);
    return this.userList.containsKey(proposedId);
  }

  /*
   * Method for RMI
   *
   * Creates and adds user ID to server's user list.
   */
  public Integer addUserBackend(Integer proposedId, String userName, byte[] userPublicKeyEncoded) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for addUser() | total requests: %d\n", this.requestCount);
    try {
      System.out.println("[BACKEND LOG] User " + userName + " got assigned ID " + proposedId);
      PublicKey userPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(userPublicKeyEncoded));
      this.userList.put(proposedId, new AuctionUser(proposedId, userName, "NO_PASSWORD_YET", userPublicKey));
    } catch (Exception e) {
      System.out.println("[BACKEND ERROR]: Deserializing user public key.");
    }
    return proposedId;
  }

  /*
   * Method for RMI
   *
   * Adds an auction listing to a list of double auctions by item type.
   */
  public HashMap<Integer, HashMap<Integer, String>> addBuyerForDoubleAuctionBackend(Integer userId, String itemType, Float bid) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for addBuyerForDoulbeAuction() | total requests: %d\n", this.requestCount);
    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(
        itemType.toLowerCase(), k -> new DoubleAuction(itemType));
    doubleAuciton.addBuyer(this.userList.get(userId), bid);
    if (this.doubleAuctionList.get(itemType.toLowerCase()).finalizeDoubleAuction()) {
      HashMap<Integer, HashMap<Integer, String>> doubleAuctionResults = doubleAuciton.closeDoubleAuction();
      if (!doubleAuctionResults.isEmpty()) {
        return doubleAuctionResults;
      }
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Adds a bid to a double auction for a specific type.
   */
  public HashMap<Integer, HashMap<Integer, String>> addSellerForDoubleAuctionBackend(Integer userId, String itemName,
                                        String itemType, String itemDesc,
                                        Integer itemCond, Float resPrice,
                                        Float startPrice)
  {
    this.requestCount++;
    System.out.printf("📩 Frontend request for addSellerForDoubleAuction() | total requests: %d\n", this.requestCount);
    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(itemType.toLowerCase(), k -> new DoubleAuction(itemType));
    AuctionItem item = new AuctionItem(assignItemId(), itemName, itemType.toLowerCase(), itemDesc, itemCond);
    AuctionListing listing = new AuctionListing(item, startPrice, resPrice);
    doubleAuciton.addSeller(this.userList.get(userId), listing);

    if (this.doubleAuctionList.get(itemType.toLowerCase())
            .finalizeDoubleAuction()) {
      HashMap<Integer, HashMap<Integer, String>> doubleAuctionResults = doubleAuciton.closeDoubleAuction();
      if (!doubleAuctionResults.isEmpty()) {
        return doubleAuctionResults;
      }
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Removes auction listing from server's global list
   */
  public AuctionListing closeAuctionBackend(Integer listingId, String itemType,
                                     Integer userId) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for closeAuction() | total requests: %d\n", this.requestCount);
    String itemLowerCase = itemType.toLowerCase();
    if (this.auctionList.containsKey(itemLowerCase) &&
        this.auctionList.get(itemLowerCase).containsKey(listingId)) {
      AuctionListing returnListing =
          this.auctionList.get(itemLowerCase).get(listingId);
      this.auctionList.get(itemLowerCase).remove(listingId);
      System.out.println("> User " + this.userList.get(userId).getUserName() +
                         " closed auction with ID: " + listingId);
      return returnListing;
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public AuctionListing openAuctionBackend(Integer userId, String itName,
                                    String itType, String itDesc,
                                    Integer itCond, Float resPrice,
                                    Float startPrice) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for openAuction() | total requests: %d\n", this.requestCount);
    AuctionItem item = new AuctionItem(assignItemId(), itName, itType, itDesc, itCond);
    AuctionListing listing =
        addListing(new AuctionListing(item, startPrice, resPrice));
    System.out.println("> User " + this.userList.get(userId).getUserName() +
                       " created auction for \"" + itName +
                       "\", id: " + item.getItemId());
    return listing;
  }

  /*
   * Method for RMI
   *
   * Generate a formatted list containing all auctioned items
   * of a specific type.
   */
  public String retrieveItemsByTypeBackend(String type) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for retrieveItemsByType() | total requests: %d\n", this.requestCount);
    if (!this.auctionList.containsKey(type.toLowerCase()) || this.auctionList.get(type.toLowerCase()).size() == 0)
      return null;

    String barrier = "--- All Available " + type.toUpperCase() + " ---\n";
    String list = "-".repeat(barrier.length() - 1) + "\n" + barrier;
    for (Map.Entry<Integer, AuctionListing> listing :
         this.auctionList.get(type.toLowerCase()).entrySet()) {
      list += "ID: " + listing.getKey() + "\n"
              + "Item condition: " +
              listing.getValue().getItem().getItemCondition() + "\n"
              + "Current price: " +
              ((listing.getValue().getCurrentPrice().compareTo(
                    listing.getValue().getStartingPrice()) < 0)
                   ? listing.getValue().getStartingPrice()
                   : listing.getValue().getCurrentPrice()) +
              " EUR\n\n";
    }
    list += "-".repeat(barrier.length() - 1) + "\n" + "-".repeat(barrier.length() - 1) + "\n";
    return list;
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpecBackend(Integer itemId, String clientId) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for getSpec() | total requests: %d\n", this.requestCount);
    System.out.println("> User " + clientId + " requested item " + itemId);

    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
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
   * @return Boolean: whether the bid was placed succesfully (auction ID exists)
   */
  public Boolean placeBidBackend(Integer userId, Integer auctionListingId, Float bid) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for placeBid() | total requests: %d\n", this.requestCount);
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry : this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(auctionListingId)) {
        AuctionListing auctionListing = entry.getValue().get(auctionListingId);
        auctionListing.appendAuctionLog(
            "[AUCTION LOG] User " + this.userList.get(userId).getUserName() +
            " requested to place a bid of " + bid + " EUR.\n");
        if ((auctionListing.getCurrentPrice() < bid) && (bid >= auctionListing.getStartingPrice())) {
          auctionListing.setCurrentPrice(bid);
          auctionListing.setBestBidUser(this.userList.get(userId).getUserName());
          auctionListing.appendAuctionLog(
              "[AUCTION LOG] User " + this.userList.get(userId).getUserName() +
              " bid accepted: " + bid + " EUR.\n");
        } else {
          auctionListing.appendAuctionLog(
              "[AUCTION LOG] User " + this.userList.get(userId).getUserName() +
              " bid NOT accepted: " + bid + " EUR.\n");
        }
        return true;
      }
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Checks whether an id is amongst existing auctions.
   */
  public Boolean idMatchesExistingItemBackend(Integer id) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for idMatchesExistingItem() | total requests: %d\n", this.requestCount);
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(id))
        return true;
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Checks if the price prompted by a buyer exceeds the starting/current price for item
   */
  public Boolean isBidPriceAcceptableBackend(Integer listingId, Float price) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for isBidPriceAcceptable() | total requests: %d\n", this.requestCount);
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(listingId)) {
        if (entry.getValue().get(listingId).getCurrentPrice() > 0.0f) {
          return entry.getValue().get(listingId).getCurrentPrice().compareTo(
                     price) < 0;
        } else {
          return entry.getValue().get(listingId).getStartingPrice().compareTo(
                     price) <= 0;
        }
      }
    }
    return true;
  }

  public AuctionUser getUserByInt(Integer userId) {
    this.requestCount++;
    System.out.printf("📩 Frontend request for getUserByInt() | total requests: %d\n", this.requestCount);
    System.out.println("✅ User with ID " + userId + " -> " + this.userList.get(userId));
    return this.userList.get(userId);
  }

  /*
   * Method for RMI
   *
   * Sends complete list of auctioned items (forward auction)
   */
  public String getAuctionedItemsBackend() {
    this.requestCount++;
    System.out.printf("📩 Frontend request for getAuctionedItems() | total requests: %d\n", this.requestCount);
    if (this.auctionList.isEmpty() || this.auctionList.values().stream().allMatch(Map::isEmpty))
      return null;

    String introStr = "\n|---- Forward Auction List (all available items) ----|\n";
    introStr += "|----------------------------------------------------|\n";
    String strToStd = introStr;
    for (HashMap.Entry<String, HashMap<Integer, AuctionListing>> listing :
         this.auctionList.entrySet()) {
      for (HashMap.Entry<Integer, AuctionListing> entry :
           this.auctionList.get(listing.getKey()).entrySet()) {
        strToStd += "\n------------------------------------------" + String.format(
          "\n| %-22s %-15s |" +
          "\n| %-22s %-15s |" +
          "\n| %-22s %-15s |" +
          "\n| %-22s %-15s |",
          "ID:", entry.getValue().getItem().getItemId(),
          "Item:", entry.getValue().getItem().getItemTitle(),
          "Starting price:", entry.getValue().getStartingPrice(),
          "Current best bid:", (entry.getValue().getCurrentPrice() == 0.0f)
              ? "No bids yet"
              : (entry.getValue().getCurrentPrice().toString() + " EUR")
        ) + "\n------------------------------------------\n\n";
      }
    }
    strToStd += "|----------------------------------------------------|\n";
    strToStd += "|----------------------------------------------------|\n";
    return strToStd;
  }

  /*
   * Server initiation.
   */
  public static void main(String[] args) {
    new AuctionServerBackend();
    System.out.println("✅ Backend replica ready");
  }

  public HashMap<String, HashMap<Integer, AuctionListing>> getAuctionListState() {
    return this.auctionList;
  }

  public HashMap<String, DoubleAuction> getDoubleAuctionListState() {
    return this.doubleAuctionList;
  }

  public HashMap<Integer, AuctionUser> getUserListState() {
    return this.userList;
  }

  public Integer getItemCounterId() {
    return globalId;
  }

  /**
   * Gets the state of the Backend Replica up with the rest
   */
  public void syncBackendState() throws Exception {
    System.out.println("📩 Backend replica state organising function request via rmi\n");
    syncAuctionListState();
    syncDoubleAuctionListState();
    syncUserListState();
    syncCounter();
  }

  private void syncCounter() {
    System.out.println("📩 Backend replica state: synchronizing the item counter...\n");
    try {
      RspList<Integer> counterState =
        this.dispatcher.callRemoteMethods(null, "getItemCounterId",
        new Object[] {},
        new Class[] {},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      Integer syncedCounter = matchAllReplicaResponses(counterState);
      if (syncedCounter == null) {
        globalId = 0;
      } else {
        globalId = syncedCounter;
      }
    } catch (Exception e) {
      System.err.println("🆘 Backend replica item counter syncrhonization error - dispatcher exception:");
      e.printStackTrace();
    }
  }

  private void syncUserListState() {
    System.out.println("📩 Backend replica state: synchronizing the user list...\n");
    try {
      RspList<HashMap<Integer, AuctionUser>> userListState =
        this.dispatcher.callRemoteMethods(null, "getUserListState",
        new Object[] {},
        new Class[] {},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      HashMap<Integer, AuctionUser> syncedUserList = matchAllReplicaResponses(userListState);
      if (syncedUserList == null) {
        this.userList = new HashMap<Integer, AuctionUser>();
      } else {
        this.userList = syncedUserList;
      }
    } catch (Exception e) {
      System.err.println("🆘 Backend replica auction list syncrhonization error - dispatcher exception:");
      e.printStackTrace();
    }
  }

  private void syncAuctionListState() {
    System.out.println("📩 Backend replica state: synchronizing the auction list...\n");
    try {
      RspList<HashMap<String, HashMap<Integer, AuctionListing>>> auctionListState =
        this.dispatcher.callRemoteMethods(null, "getAuctionListState",
        new Object[] {},
        new Class[] {},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));

      HashMap<String, HashMap<Integer, AuctionListing>> syncedAuctions = matchAllReplicaResponses(auctionListState);
      if (syncedAuctions == null) {
        this.auctionList = new HashMap<String, HashMap<Integer, AuctionListing>>();
      } else {
        this.auctionList = syncedAuctions;
      }
    } catch (Exception e) {
      System.err.println("🆘 Backend replica auction list syncrhonization error - dispatcher exception:");
      e.printStackTrace();
    }
  }

  private void syncDoubleAuctionListState() {
    System.out.println("📩 Backend replica state: synchronizing the double auction list...\n");
    try {
      RspList<HashMap<String, DoubleAuction>> doubleAuctionState =
        this.dispatcher.callRemoteMethods(null, "getDoubleAuctionListState",
        new Object[] {},
        new Class[] {},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));

      HashMap<String, DoubleAuction> syncedDoubleAuctions = matchAllReplicaResponses(doubleAuctionState);
      print("🆘🆘  SYNCED DOUBLE AUCTION LIST " + syncedDoubleAuctions);

      if (syncedDoubleAuctions == null) {
        this.doubleAuctionList = new HashMap<String, DoubleAuction>();
      } else {
        this.doubleAuctionList = syncedDoubleAuctions;
      }
    } catch (Exception e) {
      System.err.println("🆘 Backend replica double auction list syncrhonization error - dispatcher exception:");
      e.printStackTrace();
    }
  }

  /**
   * Check whether replicas return the same response
   */
  private <T> T matchAllReplicaResponses(RspList<T> responses) {
    if (responses.isEmpty()) return null;
    T firstResponse = responses.getFirst();
    for (T response : responses.getResults()) {
      if (!firstResponse.equals(response)) {
        return null;
      }
    }
    return firstResponse;
  }

  public void print(String print) {
    System.out.println(print);
  }
}
