import java.rmi.RemoteException;
import java.util.Random;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class Server implements IAuctionSystem {
  private HashMap<Integer, AuctionItem> itemList;
  private HashMap<Integer, AuctionListing> auctionList;
  private ArrayList<Integer> userList;
  private static Integer globalId = 0;
  private Random random;

  public Server() {
    super();
    this.itemList = new HashMap<Integer, AuctionItem>();
    this.auctionList = new HashMap<Integer, AuctionListing>();
    this.userList = new ArrayList<Integer>();
    this.random = new Random();

    addItem(new AuctionItem(globalId, "Leather Hat", "Hat that belonged to Juozas", 3));
    addItem(new AuctionItem(globalId, "Gloves", "Pair of Gloves", 2));
    addItem(new AuctionItem(globalId, "Oakley Windbreaker", "Windbreaker from 2006.", 5));
  }

  /*
   * Adds item to server's global list
   *
   * Global ID is updated ensuring mutex.
   */
  private synchronized AuctionItem addItem(AuctionItem item) {
    this.itemList.put(item.getItemId(), item);
    globalId++;
    return item;
  }

  /*
   * Creates and adds user ID to server's user list
   *
   * User ID is created ensuring mutex.
   */
  public synchronized Integer addUser() throws RemoteException {
    Integer userId = random.nextInt(100);
    while(this.userList.contains(userId)) {
      userId = random.nextInt();
    }
    this.userList.add(userId);
    return userId;
  }

  /*
   * Adds listing to server's global list
   */
  private AuctionListing addListing(Integer id, AuctionListing listing) {
    this.auctionList.put(id, listing);
    return listing;
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
  public Integer openAuction(String userName, String itName, String itDesc, Integer itCond,
      Float resPrice, Float startPrice) throws RemoteException {

    AuctionItem item = addItem(new AuctionItem(globalId, itName, itDesc, itCond));
    AuctionListing listing = addListing(item.getItemId(), new AuctionListing(item.getItemId(), item, startPrice, resPrice));
    System.out.println("User \"" + userName + "\" created auction for \"" + itName + "\", id: " + item.getItemId());
    return listing.getAuctionId();
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpec(int itemId, String clientId)
      throws RemoteException {
    System.out.println("Client " + clientId + " requested item " + itemId);

    if (this.itemList.containsKey(itemId)) {
      System.out.println("Succesfully retreived item with ID: " + itemId);
      AuctionItem selectedItem = this.itemList.get(itemId);
      return selectedItem;
    }
    System.out.println("Item with itemId " + itemId + " does not exist");
    return null;
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
