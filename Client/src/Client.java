import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

  private String userName;
  private ArrayList<AuctionItem> userAuctions;

  public Client(String userName) {
    this.userName = userName;
    this.userAuctions = new ArrayList<AuctionItem>();
  }

  public static void main(String[] args) {

    System.out.print("\nPlease, type your username: ");
    Scanner input = new Scanner(System.in);
    Client user = new Client(input.nextLine());

    // Outer loop in case server connection fails
    while (true) {
      try {

        AuctionSystem server = user.connectToServer("LZSCC.311 auction server");
        System.out.println("\nWelcome to the Auction System LZSCC.311, " +
            user.userName);

        // Client Loop
        Integer operation = 0;
        while (true) {
          System.out.println("\n--- Available Operations ---"
              + "\n1. Get item details"
              + "\n2. Create auction for an item"
              + "\n3. Bid on an existing auction"
              + "\n4. Close your auction"
              + "\n0. Exit"
              + "\n");

          System.out.print("Select an operation (type the number): ");
          operation = user.getOperation(input.nextLine());
          boolean exit = user.execOperation(operation, server, input);
          if (exit)
            break;
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(
            "\nCould not connect to the server. Trying again...\n");
      }
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
      System.out.println("Connected to server \"" + name + "\"");
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
      return -1;
    }
  }

  public boolean execOperation(Integer op, AuctionSystem server, Scanner input)
      throws RemoteException {
    switch (op) {
      case 0:
        input.close();
        System.out.println("You chose to exit the auction system. Goodbye " +
            getUserName() + "!");
        System.exit(0);
      case 1:
        System.out.print(
            "Which item would you like to consult? Please, type in an item ID: ");
        Integer itemId = getNumberedItemId(input);
        this.getItemSpec(server, itemId);
        return false;
      case 2:
        createAuction(server, input);
        return false;
      default:
        System.out.println("\nERROR: Unrecognized operation, try again.");
        return false;
    }
  }

  public Integer getNumberedItemId(Scanner input) {
    while (true) {
      try {
        Integer id = Integer.parseInt(input.nextLine());
        return id;
      } catch (Exception e) {
        System.out.println("Not a valid item ID. Try again:\n");
        continue;
      }
    }
  }

  public AuctionItem getItemSpec(AuctionSystem server, Integer itemId)
      throws RemoteException {
    try {
      AuctionItem item = server.getSpec((int) itemId, getUserName());
      if (item != null) {
        String introString = "\n--- Item " + itemId + " details ---";
        System.out.println(introString + "\nName: " + item.getItemTitle() +
            "\nDescription: " + item.getItemDescription() +
            "\nCondition: " + item.getItemCondition() + "\n"
            + "-".repeat(introString.length()) + "\n");
      } else {
        System.out.println("\nERROR: Item with ID: " + itemId +
            " could not be found.\n");
      }
      return item;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void createAuction(AuctionSystem server, Scanner input) {

    System.out.println("\nCreating new auction...");

    System.out.print("\nWhat is the name of your item? ");
    String name = input.nextLine();

    System.out.println("\nGive an item description");
    String description = input.nextLine();

    System.out.println(
            "\nIs your item new or used?"
            + "\nRate its usage in a scale from 1 (new) to 5 (heavily used)."
            + "\nAnything outside of this scale will default to \"Used\":");
    Integer condition = 0;
    while (true) {
      try {
        condition = Integer.parseInt(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.println("Not a valid input type, please try again.");
      }
    }
    System.out.println("\nWhat is the minimum price you are willing to sell the item for? (eur)."
                      + "\nMake sure cent decimals (if any) are separated by a dot.");
    Float reservePrice = 0.0f;
    while (true) {
      try {
        reservePrice = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.println("Not a valid input type, please try again.");
      }
    }
    System.out.println("\nSelect a starting price."
                      + "\nMake sure cent decimals (if any) are separated by a dot.");
    Float startingPrice = 0.0f;
    while (true) {
      try {
        startingPrice = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.println("Not a valid input type, please try again.");
      }
    }
    try {
      Integer id = server.openAuction(this.userName, name, description, condition, reservePrice, startingPrice);
      System.out.println("Created auction for \"" + name + "\".\nCorresponding ID: " + id);
    } catch(Exception e) {
      System.out.println("\nERROR: unable to create listing\n");
      e.printStackTrace();
    }
  }

  /*
   * Closes an auction created by this user (personal auctions)
   */
  public void closeAuction(AuctionSystem server, Scanner input) {
    listPersonalAuctions();
    if (this.userAuctions.isEmpty())
      return;

    System.out.println("Select aunction to close (type id): ");
    input.nextLine();
  }

  /*
   * Lists current user's personally auctioned items
   */
  public void listPersonalAuctions() {
    if (this.userAuctions.isEmpty()) {
      System.out.println("You have no auctioned items");
      return;
    }
    System.out.println("\"" + this.getUserName() + " \"auctioned items:");
    for (AuctionItem item : this.userAuctions) {
      System.out.println("ID: " + item.getItemId() + ", item: " +
          item.getItemTitle());
    }
  }
}
