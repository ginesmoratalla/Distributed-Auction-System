import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements AuctionSystem {
  private HashMap<Integer, AuctionItem> itemList;

  public Server() {
    super();
    this.itemList = new HashMap<Integer, AuctionItem>();
    addItem(new AuctionItem(1, "Leather Hat", "Hat that belonged to Juozas", 3));
    addItem(new AuctionItem(2, "Gloves", "Pair of Gloves", 2));
    addItem(new AuctionItem(3, "Oakley Windbreaker", "Windbreaker from 2006.", 5));
  }
  
  private void addItem(AuctionItem item) {
    this.itemList.put(item.getItemId(), item);
  }

  // From AuctionSystem
  public AuctionItem getSpec(int itemId, String clientId) throws RemoteException {
    System.out.println("Client " + clientId + " requested item " + itemId);

    if (this.itemList.containsKey(itemId)) {
      System.out.println("Succesfully retreived item with ID: " + itemId);
      AuctionItem selectedItem = this.itemList.get(itemId);
      return selectedItem;
    }

    System.out.println("Item with itemId " + itemId + " does not exist");
    return null;
  }

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
