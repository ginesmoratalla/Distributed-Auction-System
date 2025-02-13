import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {

  private final String userName;
  private final Integer userId;
  private HashMap<Integer, AuctionListing> userAuctions;
  private ClientInputManager inputManager;

  public Client(String userName, Integer userId) {
    this.userName = userName;
    this.userId = userId;
    this.userAuctions = new HashMap<Integer, AuctionListing>();
    this.inputManager = new ClientInputManager();
  }

  public static void main(String[] args) {

    Scanner input = new Scanner(System.in);

    while (true) {
      try {
        IAuctionSystem server = connectToServer("LZSCC.311 auction server");
        System.out.print("\nPlease, type your username: ");
        String uName = null;
        while (true) {
          uName = input.nextLine();
          if (!server.userNameExists(uName))
            break;
          System.out.print("\nUsername already in use, try another username: ");
        }

        System.out.println("Logging in...");
        Client user = new Client(uName, server.addUser(uName));

        // Main client Loop
        Integer operation = 0;
        while (true) {
          System.out.println("\n--- Available Operations ---"
                             + "\n1. Create auction for an item"
                             + "\n2. Close your auction"
                             + "\n3. Select forward auction mode"
                             + "\n4. Select reverse auction mode"
                             + "\n5. Select double auction mode"
                             + "\n0. Exit"
                             + "\n");

          System.out.print("Select an operation (type the number): ");
          operation = user.inputManager.getOperation(input.nextLine());
          boolean exit = user.execOperation(operation, server, input);
          if (exit) { System.exit(0); }
        }

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(
            "\nCould not connect to the server. Trying again...\n");
      }
    }
  }

  /*
   * Connect to RMI registry and get stub
   *
   */
  public static IAuctionSystem connectToServer(String name)
      throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost");
      IAuctionSystem stub = (IAuctionSystem) registry.lookup(name);
      System.out.println("Connected to server \"" + name + "\"");
      return stub;
    } catch (Exception e) {
      System.err.println("Exception:");
      e.printStackTrace();
    }
    return null;
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
      createAuction(server, input, false);
      return false;

    case 2:
      closeAuction(server, input);
      return false;

    case 3:
      viewItems(server, input);
      return false;

    case 4:
      viewReverseAuction(server, input);
      return false;

    case 5:
      viewDoubleAuction(server, input);
      return false;

    default:
      System.out.println("\nERROR: Unrecognized operation, try again.");
      return false;
    }
  }

  /*
   * Returns item details from server
   */
  public void getItemSpec(IAuctionSystem server, Scanner input, Integer itemId)
      throws RemoteException
  {
    try {
      AuctionItem item = server.getSpec(itemId, this.userName);
      if (item != null) {
        String introString = "\n--- Item " + itemId + " details ---";
        System.out.println(introString + "\nName: "
                           + item.getItemTitle()
                           + "\nDescription: " + item.getItemDescription()
                           + "\nCondition: " + item.getItemCondition() + "\n"
                           + "-".repeat(introString.length()) + "\n");
        System.out.print("Do you want to place a bid on this " + item.getItemTitle() + "? (yes/no): ");
        if(this.inputManager.getStringFromClient(input).equals("yes")) {
          this.executeItemOperation(2, itemId, server, input);
        }

      } else {
        System.out.println("\nERROR: Item with ID: " + itemId +
                           " could not be found.\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void createAuction(IAuctionSystem server, Scanner input, Boolean isDoubleAuction) {

    System.out.print("\nCreating new auction...\nWhat is the name of your item? ");
    String name = this.inputManager.getStringFromClient(input);
    System.out.print("\nGive an item description: ");
    String description = this.inputManager.getStringFromClient(input);

    String type = null;
    try {
      System.out.println(server.retrieveItemTypes());
      System.out.print("Select the type of your item among the list: ");
      while(true) {
        type = this.inputManager.getStringFromClient(input);
        if (server.itemTypeExists(type)) {
          break;
        }
        System.out.print("Type does not exist. Try again: ");
      }
    } catch (Exception e) {
      System.out.println("ERROR: Connecting to the server. Try again...");
      return;
    }

    System.out.print(
        "\nIs your item new or used?"
        + "\nRate its usage in a scale:\n"
        + "1 (new)\n"
        + "2 (barely used)\n"
        + "3 (used)\n"
        + "4 (moderately used)\n"
        + "5 (heavily used)\n"
        + "PSA: Anything outside of this scale will default to \"Used\": ");
    Integer condition = this.inputManager.getIntegerFromClient(input);

    System.out.print(
        "\nWhat is the minimum price you are willing to sell the item for? "
        + "(EUR)."
        + "\nMake sure cent decimals (if any) are separated by a dot: ");
    Float reservePrice = this.inputManager.getFloatFromClient(input);

    Float startingPrice = 0.0f;
    if (!isDoubleAuction) {
      System.out.print(
          "\nSelect a starting price (EUR)."
          + "\nMake sure cent decimals (if any) are separated by a dot: ");
      startingPrice = this.inputManager.getFloatFromClient(input);
    }

    try {
      if (!isDoubleAuction) {
        AuctionListing listing = server.openAuction(this.userId, name, type, description, condition,
                                        reservePrice, startingPrice);
        this.userAuctions.put(listing.getItem().getItemId(), listing);
        System.out.println("\n[AUCTION SUCCESS] Created auction for \"" + name +
                           "\".\nCorresponding ID: " + listing.getItem().getItemId());
      } else {
        server.addSellerForDoubleAuction(this.userId, name, type, description, condition,
                                        reservePrice, startingPrice);
        System.out.println("\n[DOUBLE AUCTION SUCCESS] Created double auction listing for " + name);
      }

    } catch (Exception e) {
      System.out.println("\nERROR: unable to create auction listing\n");
      e.printStackTrace();
    }
  }

  /*
   *
   */
  public void viewReverseAuction(IAuctionSystem server, Scanner input) {
    String type = null;
    Integer idToView = null;
    try {
      System.out.println(server.retrieveItemTypes());
      System.out.print("Select the type of item for the reverse auction: ");
      while(true) {
        type = this.inputManager.getStringFromClient(input);
        if (server.itemTypeExists(type)) {
          break;
        }
        System.out.print("Wrong type. Try again: ");
      }
      String listByType = server.retrieveItemsByType(type);
      if (listByType == null) {
        System.out.println("No items of type " + type + ". Going back...");
        return;
      }
      System.out.println("\n" + listByType);
      System.out.print("Select item by ID: ");
      while(true) {
        idToView = this.inputManager.getIntegerFromClient(input);
        if(server.idMatchesExistingItem(idToView)) break;
        System.out.print("ID does not match an entry in the database, try anotherone: ");
      }
      getItemSpec(server, input, idToView);
    } catch (Exception e) {
      System.out.println("ERROR: Connecting to the server. Try again...");
      return;
    }
  }

  /*
   * Executes an operation from a double auction
   */
  public void doubleAuctionOperation (IAuctionSystem server, Scanner input, Integer operation) {
    try {
      switch (operation) {
      case 1:
        createAuction(server, input, true);
        break;

      case 2:
        placeBidDoubleAuction(server, input);
        break;

      case 0:
        break;
      default:
        System.out.println("\nUnrecognized operation, going back...\n");
        break;
      }
    } catch (Exception e) {
      System.out.println("ERROR: Error during double auction operation execution");
    }
  }

  public void placeBidDoubleAuction(IAuctionSystem server, Scanner input) {
    String type = null;
    Float bid = null;
    try {
      System.out.println(server.retrieveItemTypes());
      System.out.print("Select the type of your item among the list: ");
      while(true) {
        type = this.inputManager.getStringFromClient(input);
        if (server.itemTypeExists(type)) {
          break;
        }
        System.out.print("Type does not exist. Try again: ");
      }

      System.out.print(
          "\nPlease, type the ammount of money (EUR) of your bid."
          + "\nNote - decimal cents (if any) must be separated by a dot: ");
      bid = this.inputManager.getFloatFromClient(input);
      server.addBuyerForDoubleAuction(this.userId, type, bid);
      System.out.println("[DOUBLE AUCTION INFO]: Bid placed succesfully.\n");

    } catch (Exception e) {
      System.out.println("ERROR: Connecting to the server. Try again...");
      return;
    }
  }

  /*
   *
   */
  public void viewDoubleAuction(IAuctionSystem server, Scanner input) {
    Integer operation = 0;
    System.out.println("\n--- Double auction operations ---"
                       + "\n1. Sell an item"
                       + "\n2. Place a bid"
                       + "\n0. Return to home"
                       + "\n");
    System.out.print("Please, select an operation: ");
    operation = this.inputManager.getIntegerFromClient(input);
    doubleAuctionOperation(server, input, operation);
  }


  public void viewItems(IAuctionSystem server, Scanner input) {
    try {
      System.out.println("Retreiving available items...\n");
      String auctionedItems = server.getAuctionedItems();
      if (auctionedItems == null) {
        System.out.println("No auctioned items. Going back...");
        return;
      }
      System.out.println(auctionedItems);
    } catch (Exception e) {
      System.out.println(
          "ERROR: unable to retreive item list from server. Going back...\n");
    }
    System.out.print("Please, type the item ID from the corresponding item " +
                     "you want to operate on: ");
    Integer selectedId = null;
    while (true) {
      try {
        selectedId = this.inputManager.getIntegerFromClient(input);
        if (!server.idMatchesExistingItem(selectedId)) {
          System.out.print("Item corresponding to " + selectedId +
                           " does not exist, try another ID: ");
          continue;
        }
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }
    Integer operation = 0;
    System.out.println("\n--- Item operations ---"
                       + "\n1. Get item specs"
                       + "\n2. Place a bid on the item"
                       + "\n3. Select a different item"
                       + "\n0. Return to home"
                       + "\n");
    System.out.print("Please, select an operation: ");
    operation = this.inputManager.getIntegerFromClient(input);
    this.executeItemOperation(operation, selectedId, server, input);
    return;
  }

  public void executeItemOperation(Integer operation, Integer selectedId,
                                   IAuctionSystem server, Scanner input) {
    try {
      switch (operation) {
      case 1:
        this.getItemSpec(server, input, selectedId);
        break;
      case 2:
        this.placeBid(server, input, selectedId);
        break;
      case 3:
        this.viewItems(server, input);
        break;
      case 0:
        break;
      default:
        System.out.println("\nUnrecognized operation, going back...\n");
        break;
      }
    } catch (Exception e) {
      System.out.println("ERROR: Error during item operation execution");
    }
  }

  public void placeBid(IAuctionSystem server, Scanner input, Integer idToBid) {
    System.out.print(
        "\nPlease, type the ammount of money (EUR) of your bid."
        + "\nNote - decimal cents (if any) must be separated by a dot: ");
    Float bid = null;
    while (true) {
    bid = this.inputManager.getFloatFromClient(input);
      try {
        if (!server.isBidPriceAcceptable(idToBid, bid)) {
          System.out.print("This offer is below the current price, try another amount: ");
          continue;
        }
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }

    try {
      // Item was sold before bid could be placed
      if (!server.idMatchesExistingItem(idToBid)) {
        System.out.println("ERROR connecting to the server. Bid was not placed");
      }
      server.placeBid(this.userId, idToBid, bid);
      System.out.println("[BID INFO] Bid placed succesfully.");
    } catch (Exception e) {
      System.out.println("ERROR connecting to the server. Bid was not placed");
    }
    return;
  }

  /*
   * Closes an auction created, if any.
   * (user's personal auctions)
   */
  public void closeAuction(IAuctionSystem server, Scanner input) {
    listPersonalAuctions();
    if (this.userAuctions.isEmpty())
      return;

    System.out.print(
        "\nSelect which auction to close (type corresponding ID): ");

    Integer idToClose = null;
    while (true) {
      idToClose = this.inputManager.getIntegerFromClient(input);
      if (this.userAuctions.containsKey(idToClose)) {
        break;
      }
      System.out.print(
          "\nID not amongst your auctioned items, please try again: ");
    }
    // Try remote method
    try {
      AuctionListing sold = server.closeAuction(idToClose, this.userAuctions.get(idToClose).getItem().getItemType(), this.userId);
      if (sold.getCurrentPrice() == null) {
        System.out.println("[ERROR] Something went wrong...\n");

      } else if (sold.getCurrentPrice() < sold.getReservePrice()) {
        System.out.println("[AUCTION INFO] Item was not sold (did not " +
                           "reach reserve price). Bid closed\n");
      } else {
        System.out.println(
            "[AUCTION INFO] Item was succesfully sold for " +
            sold.getCurrentPrice() + " EUR."
            + "\n[AUCTION INFO] Buyer: " + sold.getBestBidUser() +
            "\n[AUCTION INFO] Bid closed.\n");
      }
      System.out.println(sold.getAuctionLogs());
      this.userAuctions.remove(idToClose);
    } catch (RemoteException e) {
      e.printStackTrace();
      System.out.println(
          "ERROR: Retrieving auction listing from server.\n");
    }
  }

  /*
   * Lists current auctioned items, if any.
   * (user's personal auctions)
   */
  public void listPersonalAuctions() {
    if (this.userAuctions.isEmpty()) {
      System.out.println("You have no auctioned items, going back...");
      return;
    }
    System.out.println("\n---" + this.userName + " auctioned items ---");
    for (Map.Entry<Integer, AuctionListing> entry : this.userAuctions.entrySet()) {
      System.out.println("ID: " + entry.getKey() +
                         " | item: " + entry.getValue().getItem().getItemTitle());
    }
  }
}
