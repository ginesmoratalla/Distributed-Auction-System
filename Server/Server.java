import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements AuctionSystem {
  private HashMap<Integer, AuctionItem> itemList;

  public Server() {
    super();
    itemList = new HashMap<Integer, AuctionItem>();
    AuctionItem hat = new AuctionItem(1, "Juozas Hat", "Hat that belonged to Juozas");
    AuctionItem gloves = new AuctionItem(2, "Gloves", "Pair of Gloves");
    itemList.put(hat.getItemId(), hat);
    itemList.put(gloves.getItemId(), gloves);
  }

  // From AuctionSystem
  public AuctionItem getSpec(int itemId, int clientId) throws RemoteException {
    if (this.itemList.containsKey(itemId)) {
      AuctionItem selectedItem = this.itemList.get(itemId);
      System.out.println("Client " + clientId + " requested item " + itemId);
      return selectedItem;
    }

    System.out.println("Item with itemId " + itemId + " does not exist");
    return null;
  }

  public static void main(String[] args) {

    try {
      Server s = new Server();
      String name = "myserver";
      AuctionSystem stub = (AuctionSystem) UnicastRemoteObject.exportObject(s, 0);
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(name, stub);

      System.out.println("Server ready");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
