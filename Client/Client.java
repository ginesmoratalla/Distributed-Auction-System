import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;


public class Client {

  private String userName;
  public Client(String userName) {
    this.userName = userName;
  }

  public static void main(String[] args) {

    System.out.print("\nPlease, type your username: ");
    Scanner input = new Scanner(System.in);
    Client user = new Client(input.nextLine());
    try {
      AuctionSystem server = user.connectToServer("LZSCC.311 auction server");
      System.out.println("\n\nWelcome to the Auction System LZSCC.311, "
        + user.userName);
      
      // Client Loop
      Integer operation = 0;
      while(true) {
        System.out.println("\n\nSelect an operation:\n"
          + "1. Get item details"
          + "2. Nothing"
          + "0. Exit"
        );
        operation = user.getOperation(input.nextLine());
        boolean exit = user.execOperation(operation, server, input);
        if (exit) break;
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUserName() {
	  return this.userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public AuctionSystem connectToServer(String name) throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost");
      AuctionSystem server = (AuctionSystem) registry.lookup(name);
      System.out.println("Connected to server " + name);
      return server;
    } catch (Exception e) {
      System.err.println("Exception:");
      e.printStackTrace();
    }
    return null;
  }

  public Integer getOperation(String operation) {
    try {
      Integer opInteger = Integer.parseInt(operation);
      return opInteger;
    } catch (Exception e) {
      System.out.println("Unrecognized operation with identifier \""
        + operation + "\". Try again.");
      return -1;
    }
  }

  public boolean execOperation(Integer op, AuctionSystem server, Scanner input) throws RemoteException {
    switch (op) {
      case -1:
        return false;
      case 0:
        input.close();
        break;
      case 1:
        System.out.println("Which item would you like to consult? Please, type in an item ID: ");
        Integer itemId = getNumberedItemId(input);
        this.getItemSpec(server, itemId);
        return false;
    }
    System.out.println("You chose to exit the auction system. Goodbye "
      + getUserName() + "!");
    return true;
  }

  public Integer getNumberedItemId(Scanner input) {
    while(true) {
      try {
        Integer id = Integer.parseInt(input.nextLine());
        return id;
      } catch (Exception e) {
        System.out.println("Not a valid item ID. Try again");
        continue;
      }
    }
  }

  public AuctionItem getItemSpec(AuctionSystem server, Integer itemId) throws RemoteException {
    try {
      AuctionItem item = server.getSpec((int) itemId, getUserName());
      if (item != null) {
        System.out.println("\n--- Item " + itemId + " details ---"
          + "\nName: " + item.getItemTitle()
          + "\nDescription: " + item.getItemDescription()
          + "\nCondition: " + item.getItemCondition()
          + "---------------------------------\n"
        );
      }
      return item;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
