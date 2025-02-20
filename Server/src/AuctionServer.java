// RMI
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// Data structs
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// Signature and Security
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.security.PublicKey;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Misc imports
import java.util.Random;

public class AuctionServer implements IAuctionSystem {

  private HashMap<String, HashMap<Integer, AuctionListing>> auctionList;
  private HashMap<String, DoubleAuction> doubleAuctionList;
  private static Integer globalId = 0;
  private static Registry registry;

  private HashMap<Integer, AuctionUser> userList;
  private HashMap<Integer, IAuctionSubscriber> subscriberList;
  private HashSet<String> userNames;

  private PrivateKey privateKey;
  private PublicKey publicKey;

  private Random random;

  public AuctionServer() {
    super();
    this.auctionList = new HashMap<String, HashMap<Integer, AuctionListing>>();
    this.subscriberList = new HashMap<Integer, IAuctionSubscriber>();
    this.doubleAuctionList = new HashMap<String, DoubleAuction>();
    this.userList = new HashMap<Integer, AuctionUser>();
    this.userNames = new HashSet<String>();
    this.random = new Random();
    this.publicKey = CryptoManager.loadPublicKey("../keys/server_auction_rsa.pub");
    this.privateKey = CryptoManager.loadPrivateKey("../keys/server_auction_rsa");
  }

  /*
   * Adds item to server's global list
   *
   * Global ID is updated ensuring mutex.
   */
  private synchronized Integer assignItemId() { return globalId++; }

  /*
   * Adds aucitoned item listing to server's global list
   */
  private AuctionListing addListing(AuctionListing listing) {
    String itemType = listing.getItem().getItemType().toLowerCase();
    this.auctionList
        .computeIfAbsent(itemType, k -> new HashMap<Integer, AuctionListing>())
        .put(listing.getItem().getItemId(), listing);

    return listing;
  }

  public void notifyDoubleAuctionUser(String notification, Integer userId) throws RemoteException {
    IAuctionSubscriber subscriber = this.subscriberList.get(userId);
    sendMessage(subscriber, notification);
  }

  /*
   * Method for RMI
   *
   * Add subscriber to a list to get server notifications
   */
  public void sendMessage(IAuctionSubscriber subscriber, String message) throws RemoteException {
    subscriber.getMessage(message);
  }

  /*
   * Method for RMI
   *
   * Add subscriber to a list to get server notifications
   *
   */
  public void registerSubscriber(Integer userId, IAuctionSubscriber subscriber) throws RemoteException {
    this.subscriberList.put(userId, subscriber);
    System.out.println("> User " + this.userList.get(userId).getUserName() + " registered as subscriber");
  }


  /*
   * Method for RMI
   *
   * Checks whether a username is used.
   */
  public Boolean userNameExists(String userName) throws RemoteException {
    return this.userNames.contains(userName);
  }

  /*
   * Method for RMI
   *
   * Creates and adds user ID to server's user list.
   * User random ID is created ensuring mutex.
   */
  public synchronized Integer addUser(String userName, byte[] userPublicKeyEncoded) throws RemoteException {
    Integer userId = random.nextInt(1000);
    while (this.userList.containsKey(userId)) {
      userId = random.nextInt();
    }
    PublicKey userPublicKey;
    try {
      System.out.println("> User " + userName + " got assigned ID " + userId);
      userPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(userPublicKeyEncoded));
      this.userList.put(userId,
                        new AuctionUser(userId, userName, "NO_PASSWORD_YET", userPublicKey));
      this.userNames.add(userName);
    } catch (Exception e) {
      System.out.println("ERROR: Deserializing user public key.");
    }

    return userId;
  }

  /*
   * Method for RMI
   *
   * Adds an auction listing to a list of double auctions by item type.
   */
  public void addBuyerForDoubleAuction(Integer userId, String itemType,
                                       Float bid) throws RemoteException {
    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(
        itemType.toLowerCase(), k -> new DoubleAuction(itemType));
    doubleAuciton.addBuyer(this.userList.get(userId), bid);

    if (this.doubleAuctionList.get(itemType.toLowerCase())
            .finalizeDoubleAuction()) {
      doubleAuciton.closeDoubleAuction();
      this.doubleAuctionList.remove(itemType);
    }
  }

  /*
   * Method for RMI
   *
   * Adds a bid to a double auction for a specific type.
   */
  public void addSellerForDoubleAuction(Integer userId, String itemName,
                                        String itemType, String itemDesc,
                                        Integer itemCond, Float resPrice,
                                        Float startPrice)
      throws RemoteException {

    DoubleAuction doubleAuciton = this.doubleAuctionList.computeIfAbsent(
        itemType.toLowerCase(), k -> new DoubleAuction(itemType));

    AuctionItem item = new AuctionItem(assignItemId(), itemName, itemType.toLowerCase(), itemDesc, itemCond);
    AuctionListing listing = new AuctionListing(item, startPrice, resPrice);
    doubleAuciton.addSeller(this.userList.get(userId), listing);

    if (this.doubleAuctionList.get(itemType.toLowerCase())
            .finalizeDoubleAuction()) {
      doubleAuciton.closeDoubleAuction();
    }
  }

  /*
   * Method for RMI
   *
   * Removes auction listing from server's global list
   */
  public AuctionListing closeAuction(Integer listingId, String itemType,
                                     Integer userId) throws RemoteException {
    String itemLowerCase = itemType.toLowerCase();
    if (this.auctionList.containsKey(itemLowerCase) &&
        this.auctionList.get(itemLowerCase).containsKey(listingId)) {
      AuctionListing returnListing =
          this.auctionList.get(itemLowerCase).get(listingId);
      this.auctionList.get(itemLowerCase).remove(listingId);
      System.out.println("> User " + this.userList.get(userId).getUserName() +
                         " closed auction with ID: " + listingId);
      return returnListing;
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Checks for an existing entry among types of items
   */
  public Boolean itemTypeExists(String typeStr) {
    String lowerCaseString = typeStr.toLowerCase();
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      if (type.getValue().toLowerCase().equals(lowerCaseString))
        return true;
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Returs a formatted list of item types (for reverse auctions)
   *
   */
  public String retrieveItemTypes() throws RemoteException {
    String formattedString = "--- Item Types ---\n";
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      formattedString += "> " + type.getValue() + "\n";
    }
    formattedString += "\nRESPECT SPACES WHEN TYPING ITEM TYPE\n";
    return formattedString;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public AuctionListing openAuction(Integer userId, String itName,
                                    String itType, String itDesc,
                                    Integer itCond, Float resPrice,
                                    Float startPrice) throws RemoteException {

    AuctionItem item =
        new AuctionItem(assignItemId(), itName, itType, itDesc, itCond);
    AuctionListing listing =
        addListing(new AuctionListing(item, startPrice, resPrice));
    System.out.println("> User " + this.userList.get(userId).getUserName() +
                       " created auction for \"" + itName +
                       "\", id: " + item.getItemId());
    return listing;
  }

  /*
   * Method for RMI
   *
   * Generate a formatted list containing all auctioned items
   * of a specific type.
   */
  public String retrieveItemsByType(String type) throws RemoteException {
    if (!this.auctionList.containsKey(type.toLowerCase()) ||
        this.auctionList.get(type.toLowerCase()).size() == 0)
      return null;

    String list = "--- All Available " + type + "---\n";
    for (Map.Entry<Integer, AuctionListing> listing :
         this.auctionList.get(type.toLowerCase()).entrySet()) {
      list += "ID: " + listing.getKey() + "\n"
              + "Item condition: " +
              listing.getValue().getItem().getItemCondition() + "\n"
              + "Current price: " +
              ((listing.getValue().getCurrentPrice().compareTo(
                    listing.getValue().getStartingPrice()) < 0)
                   ? listing.getValue().getStartingPrice()
                   : listing.getValue().getCurrentPrice()) +
              " EUR\n\n";
    }
    return list;
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpec(Integer itemId, String clientId)
      throws RemoteException {
    System.out.println("> User " + clientId + " requested item " + itemId);

    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(itemId)) {
        System.out.println("> Succesfully retreived item with ID: " + itemId);
        AuctionItem selectedItem = entry.getValue().get(itemId).getItem();
        return selectedItem;
      }
    }

    System.out.println("> Item with itemId " + itemId + " does not exist");
    return null;
  }

  /*
   * Method for RMI
   *
   * Place a bid.
   *
   * @return Boolean; whether the bid was placed succesfully (auction ID exists)
   */
  public void placeBid(Integer userId, Integer auctionListingId, Float bid)
      throws RemoteException {
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(auctionListingId)) {
        AuctionListing auctionListing = entry.getValue().get(auctionListingId);
        auctionListing.appendAuctionLog(
            "[AUCTION LOG] User " + this.userList.get(userId).getUserName() +
            " placed a bid of " + bid + " EUR.\n");
        if ((auctionListing.getCurrentPrice() < bid) &&
            (bid >= auctionListing.getStartingPrice())) {
          auctionListing.setCurrentPrice(bid);
          auctionListing.setBestBidUser(
              this.userList.get(userId).getUserName());
        }
        break;
      }
    }
  }

  /*
   * Method for RMI
   *
   * Checks whether an id is amongst existing auctions.
   */
  public Boolean idMatchesExistingItem(Integer id) throws RemoteException {
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(id))
        return true;
    }
    return false;
  }

  /*
   * Method for RMI
   *
   * Checks if the price prompted by a buyer exceeds the starting price for item
   */
  public Boolean isBidPriceAcceptable(Integer listingId, Float price)
      throws RemoteException {
    for (Map.Entry<String, HashMap<Integer, AuctionListing>> entry :
         this.auctionList.entrySet()) {
      if (entry.getValue().containsKey(listingId)) {
        if (entry.getValue().get(listingId).getCurrentPrice() > 0.0f) {
          return entry.getValue().get(listingId).getCurrentPrice().compareTo(
                     price) < 0;
        } else {
          return entry.getValue().get(listingId).getStartingPrice().compareTo(
                     price) <= 0;
        }
      }
    }
    return true;
  }

  /*
   * Method for RMI
   *
   */
  public String getAuctionedItems() throws RemoteException {
    if (this.auctionList.isEmpty() ||
        this.auctionList.values().stream().allMatch(Map::isEmpty))
      return null;
    String introStr = "\n===== ALL AVAILABLE AUCTIONED ITEMS ======\n";
    String strToStd = introStr;
    for (HashMap.Entry<String, HashMap<Integer, AuctionListing>> listing :
         this.auctionList.entrySet()) {
      for (HashMap.Entry<Integer, AuctionListing> entry :
           this.auctionList.get(listing.getKey()).entrySet()) {

        strToStd +=
            "ID: " + entry.getValue().getItem().getItemId() +
            " || item: " + entry.getValue().getItem().getItemTitle() +
            " || starting price: " + entry.getValue().getStartingPrice() +
            " EUR"
            + " || current best bid: " +
            ((entry.getValue().getCurrentPrice() == 0.0f)
                 ? "No best bid yet"
                 : entry.getValue().getCurrentPrice()) +
            "\n";
      }
    }
    strToStd += "=".repeat(introStr.length() - 2) + "\n";
    return strToStd;
  }

  /*
   * Method for RMI
   *
   * Two-way digital signature handshake to verify user's identity
   * and send an acknowledgement signature back to the user.
   *
   * Hybrid encripyion -> symmetric AES key + asymmetric RSA signature.
   */
  public synchronized List<byte[]> verifyClientSignature(byte[] encryptedAES, byte[] encryptedSignature,
    String originalMessage, Integer userId, String originalSignatureHashDigest) throws RemoteException {

    CryptoManager cryptoManager = new CryptoManager();
    Cipher cipher;
    Signature signature;
    AuctionUser user = this.userList.get(userId);
    byte[] decryptedSignature;
    byte[] serverReturnSignature;
    byte[] serverSignatureHashDigest;
    SecretKey aesKey;

    // Decrypt encrypted AES key with server's private RSA key
    try {
      cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
      aesKey = new SecretKeySpec(cipher.doFinal(encryptedAES), "AES");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[DECRYPTION ERROR]: Problem decrypting AES key with server's private RSA key");
      return null;
    }
    // Decrypt encrypted signature given by user with the decrypted AES key
    try {
      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      decryptedSignature = cipher.doFinal(encryptedSignature);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("[DECRYPTION ERROR]: Problem decrypting signature with server's private key");
      return null;
    }
    // Check whether message has not been tampered with (verify signature)
    try {
      signature = Signature.getInstance("SHA256WithRSA");
      signature.initVerify(user.getPublicKey());
      signature.update(originalMessage.getBytes(StandardCharsets.UTF_8));

      if (!signature.verify(decryptedSignature))
        throw new Exception("[USER SIGNATURE VERIFICATION ERROR]: Wrong Client Signature");

      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(decryptedSignature);
      String decryptedSignatureHashDigest = cryptoManager.byteArrayToHex(md.digest());

      System.out.println("\n[USER SIGNATURE VERIFICATION SUCCESS] Client verification complete for user: " + user.getUserName());
      System.out.println("+ Decrypted signature's hash digest: " + decryptedSignatureHashDigest);
      System.out.println("+ Original signature's hash digest: " + originalSignatureHashDigest + "\n");

    } catch (Exception e) {
      System.out.println("[SIGNATURE VERIFICATION ERROR]: Error verifying user signature");
      return null;
    }
    // Create server signature
    try {
      signature = Signature.getInstance("SHA256WithRSA");
      signature.initSign(this.privateKey);
      signature.update(originalMessage.getBytes(StandardCharsets.UTF_8));
      serverReturnSignature = signature.sign();
    } catch (Exception e) {
      System.out.println("[SERVER SIGNATURE ERROR]: Problem generating server signature");
      return null;
    }
    // Encrypt server signature with AES Key
    try {
      // Hash digest of server signature (pre-AES encryption)
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(serverReturnSignature);
      serverSignatureHashDigest = md.digest();

      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);
      serverReturnSignature = cipher.doFinal(serverReturnSignature);
    } catch (Exception e) {
      System.out.println("[SERVER SIGNATURE ENCRYPTION ERROR]: Problem encrypting server signature with AES Key");
      return null;
    }
    return List.of(serverReturnSignature, serverSignatureHashDigest);
  }

  /*
   * Server initiation.
   */
  public static void main(String[] args) {
    try {
      AuctionServer s = new AuctionServer();
      String name = "LZSCC.311 auction server";
      IAuctionSystem remoteObject =
          (IAuctionSystem) UnicastRemoteObject.exportObject(s, 0);

      // String subscriberName = "LZSCC.311 auction subscriber";
      // Client c = new Client();
      // IAuctionSubscriber remoteSubscriber = (IAuctionSubscriber) UnicastRemoteObject.exportObject(c, 0);
      // registry.rebind(subscriberName, remoteSubscriber);


      registry = LocateRegistry.getRegistry();
      registry.rebind(name, remoteObject);

      System.out.println("> Server ready");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
