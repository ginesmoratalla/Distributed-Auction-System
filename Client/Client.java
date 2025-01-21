import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Client n.\nDid not provide stdin args");
      return;
    }
    int n = Integer.parseInt(args[0]);
    try {
      String name = "myserver";
      Registry registry = LocateRegistry.getRegistry("localhost");
      AuctionSystem server = (AuctionSystem) registry.lookup(name);
      System.out.println("Connected to server " + name);
      AuctionItem result = server.getSpec(2, n);
      System.out.println(result.getItemDescription());
    } catch (Exception e) {
      System.err.println("Exception:");
      e.printStackTrace();
    }
  }
}
