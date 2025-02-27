// RMI
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// Cryptography and Security
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;

// Misc imports
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Auction Client
 */
public class Client extends UnicastRemoteObject implements IAuctionSubscriber {

  private final String userName;
  private final Integer userId;
  private HashMap<Integer, AuctionListing> userAuctions;
  private ClientInputManager inputManager;
  private PublicKey serverPublicKey;
  private static KeyPair clientKeyPair = CryptoManager.generateRSAKeys();

  public Client(String userName, Integer userId) throws RemoteException {
    super();
    this.userName = userName;
    this.userId = userId;
    this.userAuctions = new HashMap<Integer, AuctionListing>();
    this.inputManager = new ClientInputManager();
    this.serverPublicKey = CryptoManager.loadPublicKey("../keys/server_auction_rsa.pub");
  }

  public static void main(String[] args) {
    Scanner input = new Scanner(System.in);
    while (true) {
      try {
        API server = connectToServer("LZSCC.311 auction server");
        System.out.print("\nPlease, type your username: ");
        String uName = null;
        while (true) {
          uName = input.nextLine();
          if (!server.userNameExists(uName))
            break;
          System.out.print("\nUsername already in use, try another username: ");
        }
        System.out.println("Logging in...");
        Client user = new Client(uName, server.addUser(uName, clientKeyPair.getPublic().getEncoded()));
        server.registerSubscriber(user.userId, user);
        if (!verifyServerSignature(server, user.userName, clientKeyPair, user.serverPublicKey, user.userId)) {
          System.out.println("Could not validate server identity. Exiting...");
          System.exit(0);
        }

        // Main client Loop
        Integer operation = 0;
        while (true) {
          System.out.println(ClientInputManager.MAIN_MENU_OPERATIONS);
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
   * Print message from double auction directed to the client
   */
  @Override
  public void getMessage(Integer directedUserId, String message) throws RemoteException {
    if (directedUserId.equals(userId)) {
      System.out.println("\n[INCOMING SERVER NOTIFICATION]\n" + message + "\n");
    }
  }

  /*
   * Connect to RMI registry and get server stub
   *
   */
  public static API connectToServer(String name)
      throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry("localhost");
      API stub = (API) registry.lookup(name);
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
  public boolean execOperation(Integer op, API server, Scanner input)
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
   * Returns item details from server (forward + reverse auction)
   */
  public void getItemSpec(API server, Scanner input, Integer itemId)
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

  /*
   * Create auction listing (valid for all auction types)
   */
  public void createAuction(API server, Scanner input, Boolean isDoubleAuction) {

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

    System.out.print(ClientInputManager.ITEM_USAGE_SCALE);
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
   * Return items filtered by category
   */
  public void viewReverseAuction(API server, Scanner input) {
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
  public void doubleAuctionOperation (API server, Scanner input, Integer operation) {
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

  /*
   * Double auction options menu
   */
  public void viewDoubleAuction(API server, Scanner input) {
    Integer operation = 0;
    System.out.println(ClientInputManager.DOUBLE_AUCTION_OPERATIONS);
    System.out.print("Please, select an operation: ");
    operation = this.inputManager.getIntegerFromClient(input);
    doubleAuctionOperation(server, input, operation);
  }


  /*
   * Return a list of all auctioned items (forward auction)
   *
   * Can additionally select an item to operate on
   */
  public void viewItems(API server, Scanner input) {
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
          "[SERVER ERROR]: Unable to retreive item list from server. Going back...\n");
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
        System.out.print("[SERVER ERROR]: Unable to retrieve item from server.");
      }
    }
    Integer operation = 0;
    System.out.println(ClientInputManager.REGULAR_AUCTION_OPERATIONS);
    System.out.print("Please, select an operation: ");
    operation = this.inputManager.getIntegerFromClient(input);
    this.executeItemOperation(operation, selectedId, server, input);
    return;
  }

  /*
   * Execute operation on a specific item (forward + reverse auction)
   * */
  public void executeItemOperation(Integer operation, Integer selectedId,
                                   API server, Scanner input) {
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
      System.out.println("[ERROR]: Error during item operation execution");
    }
  }

  /*
   * Place a bid on a selected item (forward + reverse auction)
   */
  public void placeBid(API server, Scanner input, Integer idToBid) {
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
        System.out.println("[ERROR] Connecting to the server. Bid was not placed");
      }
    }
    try {
      // Item was sold before bid could be placed
      if (!server.idMatchesExistingItem(idToBid)) {
        System.out.println("[ERROR] Connecting to the server. Bid was not placed");
      }
      server.placeBid(this.userId, idToBid, bid);
      System.out.println("[BID INFO] Bid placed succesfully.");
    } catch (Exception e) {
      System.out.println("[ERROR] Connecting to the server. Bid was not placed");
    }
    return;
  }

  /*
   * Place a bid on a double auction
   */
  public void placeBidDoubleAuction(API server, Scanner input) {
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
      System.out.println("[ERROR]: Connecting to the server. Try again...");
      return;
    }
  }

  /*
   * Closes an auction created, if any.
   * (user's personal auctions)
r  *
   * Not valid for double auction items.
   */
  public void closeAuction(API server, Scanner input) {
    listPersonalAuctions();
    if (this.userAuctions.isEmpty()) return;

    System.out.print("\nSelect auction ID to close (\"exit\" to go back): ");
    Integer idToClose = null;
    while (true) {
      idToClose = this.inputManager.getIntegerFromClient(input);
      if (this.userAuctions.containsKey(idToClose)) {
        break;
      } else if (idToClose.equals(-1)) return;
      System.out.print("\nID not amongst your auctioned items, please try again: ");
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
          "[SERVER ERROR]: Retrieving auction listing from server.\n");
    }
  }
  /*
   * Lists current auctioned items, if any.
   * (user's personal auctions)
   */
  public void listPersonalAuctions() {
    if (this.userAuctions.isEmpty()) {
      System.out.println("[PERSONAL AUCTION FAILURE] You have no auctioned items, going back...");
      return;
    }
    System.out.println(ClientInputManager.printUserPersonalAuctions(this.userName, this.userAuctions));
  }

  /*
   * Two-way digital signature handshake to verify server's identity.
   * Hybrid encripyion -> symmetric AES key + asymmetric RSA signature.
   */
  private static Boolean verifyServerSignature(API stub, String userName,
        KeyPair userKeyPair, PublicKey serverPubKey, Integer userId) {

    System.out.println("[SECURITY] Verifying server identity...");
    String verificationMessage = userName;
    Signature signature;
    CryptoManager cryptoManager = new CryptoManager();
    Cipher cipher;
    byte[] digitalSignature;
    byte[] encryptedSignature;
    List<byte[]> serverSignatureReturn;
    byte[] encryptedAES;
    byte[] serverSignature;
    SecretKey aesKey;

    // Generate AES key (one-time use, only valid for this handshake)
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(256);
      aesKey = keyGenerator.generateKey();
      System.out.println("[SECURITY LOG]: Succesfully generated AES session key");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[SECURITY ERROR]: Generating AES key for encryption");
      return false;
    }
    // Sign message with user's private RSA key
    try {
      signature = Signature.getInstance("SHA256WithRSA");
      signature.initSign(userKeyPair.getPrivate());
      signature.update(verificationMessage.getBytes(StandardCharsets.UTF_8));
      digitalSignature = signature.sign();
      System.out.println("[SECURITY LOG]: Succesfully generated Signature with your RSA private key");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[SECURITY ERROR]: Problem " +
        "generating digital signature with user's private RSA");
      return false;
    }
    // Encrypt signature (hybrid encryption)
    try {
      // Encrypt signed message with AES Key
      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);
      encryptedSignature = cipher.doFinal(digitalSignature);
      System.out.println("[SECURITY LOG]: Succesfully encrypted your RSA signature with the AES session key");

      // Encrypt AES key with server's public RSA
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, serverPubKey);
      encryptedAES = cipher.doFinal(aesKey.getEncoded());
      System.out.println("[SECURITY LOG]: Succesfully encrypted AES session key with the Server's public RSA key");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[SECURITY ERROR]: Problem encrypting signature or AES key");
      return false;
    }
    // Get verification + ack signature from server
    try {
      // Hash digest of the signature (signature pre-AES encryption)
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(digitalSignature);
      String signatureHashDigest = cryptoManager.byteArrayToHex(md.digest());

      System.out.println("[SECURITY LOG]: Sending verification to the server (Your signature + Signature's RSA hash digest + Encrypted AES session key)");
      serverSignatureReturn = stub.verifyClientSignature(
        encryptedAES,
        encryptedSignature,
        verificationMessage,
        userId,
        signatureHashDigest);
      System.out.println("[SECURITY LOG]: Verifying server's identity...");

      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      serverSignature = cipher.doFinal(serverSignatureReturn.get(0));
      System.out.println("[SECURITY LOG]: Decrypting server's signature with AES session key");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[SECURITY ERROR]: Retrieving server signature");
      return false;
    }
    // Verify validity of the signature from server
    try {
      signature = Signature.getInstance("SHA256WithRSA");
      signature.initVerify(serverPubKey);
      signature.update(verificationMessage.getBytes(StandardCharsets.UTF_8));
      if (!signature.verify(serverSignature)) return false;

      // Show verification on stdout
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(serverSignature);
      String returnHashDigest = cryptoManager.byteArrayToHex(md.digest());
      System.out.println("[SERVER SIGNATURE VERIFICATION SUCCESS] Server signature verification complete");
      System.out.println("+ Decrypted server's signature's hash digest: " + returnHashDigest);
      System.out.println("+ Original server's signature's hash digest: " + cryptoManager.byteArrayToHex(serverSignatureReturn.get(1)) + "\n");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[SECURITY ERROR]: Verifying server signature");
      return false;
    }
    return true;
  }
}
