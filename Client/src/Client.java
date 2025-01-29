import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {

  private final String userName;
  private final Integer userId;
  private HashMap<Integer, String> userAuctions;

  public Client(String userName, Integer userId) {
    this.userName = userName;
    this.userId = userId;
    this.userAuctions= new HashMap<Integer, String>();
  }

  public static void main(String[] args) {

    // Outer loop in case server connection fails
    while (true) {
      try {
        IAuctionSystem server = connectToServer("LZSCC.311 auction server");
        System.out.print("\nPlease, type your username: ");
        Scanner input = new Scanner(System.in);
        String uName = input.nextLine();
        Client user = new Client(uName, server.addUser(uName));

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
          if (exit) System.exit(0);
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(
            "\nCould not connect to the server. Trying again...\n");
      }
    }
  }

  public static IAuctionSystem connectToServer(String name)
      throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost");
      IAuctionSystem server = (IAuctionSystem) registry.lookup(name);
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

  /*
   * Executes the function logic corresponding to the user's selected
   * operaiton.
   */
  public boolean execOperation(Integer op, IAuctionSystem server, Scanner input)
      throws RemoteException {
    switch (op) {
      case 0:
        input.close();
        System.out.println("You chose to exit the auction system. Goodbye " +
            this.userName + "!");
        return true;

      case 1:
        System.out.print(
            "Which item would you like to consult? Please, type in an item ID: ");
        Integer itemId = getNumberedItemId(input);
        this.getItemSpec(server, itemId);
        return false;

      case 2:
        createAuction(server, input);
        return false;

      case 3:
        placeBid(server, input);
        return false;

      case 4:
        closeAuction(server, input);
        return false;

      default:
        System.out.println("\nERROR: Unrecognized operation, try again.");
        return false;
    }
  }

  /*
   * Gets an integer number from stdin as item ID
   */
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

  /*
   * Returns item details from server 
   */
  public AuctionItem getItemSpec(IAuctionSystem server, Integer itemId)
      throws RemoteException {
    try {
      AuctionItem item = server.getSpec((int) itemId, this.userName);
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

  public void createAuction(IAuctionSystem server, Scanner input) {

    System.out.println("\nCreating new auction...");
    System.out.print("\nWhat is the name of your item? ");
    String name = input.nextLine();
    System.out.print("\nGive an item description: ");
    String description = input.nextLine();

    System.out.print(
        "\nIs your item new or used?"
            + "\nRate its usage in a scale:\n"
            + "1 (new)\n" 
            + "2 (barely used)\n"
            + "3 (used)\n"
            + "4 (moderately used)\n"
            + "5 (heavily used)\n"
            + "PSA: Anything outside of this scale will default to \"Used\": ");
    Integer condition = 0;
    while (true) {
      try {
        condition = Integer.parseInt(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }

    System.out.print(
        "\nWhat is the minimum price you are willing to sell the item for? "
        + "(EUR)."
        + "\nMake sure cent decimals (if any) are separated by a dot: ");
    Float reservePrice = 0.0f;
    while (true) {
      try {
        reservePrice = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }
    System.out.print(
        "\nSelect a starting price (EUR)."
            + "\nMake sure cent decimals (if any) are separated by a dot: ");
    Float startingPrice = 0.0f;
    while (true) {
      try {
        startingPrice = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }
    try {
      Integer id = server.openAuction(this.userId, name, description,
          condition, reservePrice, startingPrice);
      this.userAuctions.put(id, name);
      System.out.println("\n[AUCTION SUCCESS] Created auction for \""
                          + name 
                          + "\".\nCorresponding ID: " + id
      );

    } catch (Exception e) {
      System.out.println("\nERROR: unable to create auction listing\n");
      e.printStackTrace();
    }
  }

  public void placeBid(IAuctionSystem server, Scanner input) {
    try {
      System.out.println("Retreiving available items...\n"
        + server.getAuctionedItems()
      );
    } catch (Exception e) {
      System.out.println("ERROR: unable to retreive item list from server. Going back...\n");
    }
  }

  /*
   * Closes an auction created, if any.
   *  (user's personal auctions)
   */
  public void closeAuction(IAuctionSystem server, Scanner input) {
    listPersonalAuctions();
    if (this.userAuctions.isEmpty()) return;

    System.out.print("\nSelect which auction to close (type corresponding ID): ");
    while(true) {
      // Try parsing input into Integer ID
      try {
        Integer idToClose = Integer.parseInt(input.nextLine());
        if(!this.userAuctions.containsKey(idToClose)) {
          System.out.print("\nID not amongst your auctioned items, please try again: ");
          continue;
        }
        // Try remote method
        try {
          AuctionListing sold = server.closeAuction(this.userId, idToClose);
          if(sold.getCurrentPrice() == null) {
            System.out.println("[ERROR] Something went wrong...\n");

          } else if (sold.getCurrentPrice() < sold.getReservePrice()) {
            System.out.println("[AUCTION INFO] Item was not sold (did not reach reserve price). Bid closed\n");

          } else {
            System.out.println("[AUCTION INFO] Item was succesfully sold for "
              + sold.getCurrentPrice() + " EUR."
              + "\nBuyer: " + sold.getBestBidUser() 
              + "\nBid closed.\n");
          }
          this.userAuctions.remove(idToClose);
          return;
        } catch (RemoteException e) {
          System.out.println("ERROR: Retreiving auction listing form server. Try again.\n");
          return;
        }

      } catch (Exception e) {
        System.out.print("Not a valid ID format, please try again: ");
        continue;
      }
    }
  }

  /*
   * Lists current auctioned items, if any.
   *  (user's personal auctions)
   */
  public void listPersonalAuctions() {
    if (this.userAuctions.isEmpty()) {
      System.out.println("You have no auctioned items, going back...");
      return;
    }
    System.out.println("\n---" + this.userName + " auctioned items ---");
    for (Map.Entry<Integer, String> entry: this.userAuctions.entrySet()) {
      System.out.println("ID: "
                          + entry.getKey()
                          + " | item: "
                          + entry.getValue()
      );
    }
  }
}
