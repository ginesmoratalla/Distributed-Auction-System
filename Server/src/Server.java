import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements AuctionSystem {
  private HashMap<Integer, AuctionItem> itemList;
  private HashMap<Integer, AuctionListing> auctionList;
  private static Integer globalId = 0;

  public Server() {
    super();
    this.itemList = new HashMap<Integer, AuctionItem>();
    this.auctionList = new HashMap<Integer, AuctionListing>();

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
   * Adds listing to server's global list
   */
  private AuctionListing addListing(Integer id, AuctionListing listing) {
    this.auctionList.put(id, listing);
    return listing;
  }

  /*
   * Method for RMI
   *
   * Removes listing to server's global list
   */
  public Float closeAuction(Integer listingId) throws RemoteException {
    if (this.auctionList.containsKey(listingId)) {
      AuctionListing listing = this.auctionList.get(listingId);
      if (listing.getCurrentPrice().compareTo(listing.getReservePrice()) <= 0) {
        return null;
      } else {
        return listing.getCurrentPrice();
      }
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public Integer openAuction(String itName, String itDesc, Integer itCond,
      Float resPrice, Float startPrice) throws RemoteException {

    AuctionItem item = (new AuctionItem(globalId, itName, itDesc, itCond));
    AuctionListing listing = addListing(item.getItemId(), new AuctionListing(item.getItemId(), item, startPrice, resPrice));
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
      AuctionSystem stub = (AuctionSystem) UnicastRemoteObject.exportObject(s, 0);
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(name, stub);

      System.out.println("Server ready");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
