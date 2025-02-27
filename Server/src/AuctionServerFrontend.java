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
import java.security.PublicKey;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// JGroups
import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;

// Misc imports
import java.util.Random;

public class AuctionServerFrontend implements API {

  private HashMap<Integer, IAuctionSubscriber> subscriberList;
  private HashSet<String> userNames;
  private Random random;

  private PrivateKey privateKey;
  private PublicKey publicKey;

  private final int DISPATCHER_TIMEOUT = 1000;
  public final String SERVER_NAME = "LZSCC.311 auction server";
  private static Registry registry;
  private JChannel groupChannel;
  private RpcDispatcher dispatcher;

  public AuctionServerFrontend() {
    super();
    this.userNames = new HashSet<String>();
    this.subscriberList = new HashMap<Integer, IAuctionSubscriber>();

    this.publicKey = CryptoManager.loadPublicKey("../keys/server_auction_rsa.pub");
    this.privateKey = CryptoManager.loadPrivateKey("../keys/server_auction_rsa");
    this.random = new Random();
    this.groupChannel = GroupUtils.connect();

    if (this.groupChannel == null) {
      System.exit(1);
    }
    this.bind(this.SERVER_NAME);
    this.dispatcher = new RpcDispatcher(this.groupChannel, this);
    this.dispatcher.setMembershipListener(new MembershipListener());
  }

  /*
   * Frontend server initiation.
   */
  public static void main(String[] args) {
    try {
      new AuctionServerFrontend();
      System.out.println("✅ Frontend server ready");
    } catch (Exception e) {
      System.err.println("🆘 remote exception:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /*
   * Bind this frontend with RMI Registry
   * */
  private void bind(String serverName) {
    try {
      API remoteObject = (API) UnicastRemoteObject.exportObject(this, 0);
      registry = LocateRegistry.getRegistry();
      registry.rebind(serverName, remoteObject);
      System.out.println("✅ rmi server running...");
    } catch (Exception e) {
      System.err.println("🆘 remote exception:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /*
   * Method for RMI
   *
   * Checks whether a username is used.
   */
  public Boolean userNameExists(String userName) throws RemoteException {
    System.out.printf("📩 rmi request for userNameExists()\n");
    Boolean userNameExists = this.userNames.contains(userName);
    if (!userNameExists) this.userNames.add(userName);
    return userNameExists;
  }


  /*
   * Sends result message from double auction to client (Pub-Sub)
   * One channel, userId filters who recieves message
   */
  public void notifyDoubleAuctionUser(String notification, Integer userId) throws RemoteException {
    IAuctionSubscriber subscriber = this.subscriberList.get(userId);
    sendMessage(userId, subscriber, notification);
  }

  /*
   * Sends double auction results to all participants
   */
  private void notifyAllDoubleAuctionUsers(HashMap<Integer, HashMap<Integer, String>> doubleAuctionResults) throws RemoteException {
    for (HashMap<Integer, String> innerMap : doubleAuctionResults.values()) {
      Map.Entry<Integer, String> entry = innerMap.entrySet().iterator().next();
      notifyDoubleAuctionUser(entry.getValue(), entry.getKey());
    }
  }

  /*
   * Method for RMI
   *
   * Send client incoming notification
   */
  public void sendMessage(Integer userId, IAuctionSubscriber subscriber, String message) throws RemoteException {
    subscriber.getMessage(userId, message);
  }

  /*
   * Method for RMI
   *
   * Add subscriber to a list to get server notifications (Pub-Sub)
   *
   */
  public void registerSubscriber(Integer userId, IAuctionSubscriber subscriber) throws RemoteException {
    this.subscriberList.put(userId, subscriber);
    System.out.println("✅ User with ID " + userId + " registered as subscriber");
  }

  /*
   * Method for RMI
   *
   * Checks for an existing entry among types of items
   */
  public Boolean itemTypeExists(String typeStr) throws RemoteException {
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
    System.out.println("📩 retrieveItemTypes() function request via rmi\n");
    String formattedString = "\n--- Item Types ---\n------------------\n";
    for (AuctionItemTypeEnum type : AuctionItemTypeEnum.values()) {
      formattedString += "> " + type.getValue() + "\n";
    }
    formattedString += "\nRESPECT SPACES WHEN TYPING ITEM TYPE\n";
    return formattedString;
  }

  /*
   * Method for RMI
   *
   * Creates and adds user ID to server's user list.
   * User random ID is created ensuring mutex.
   */
  public Integer addUser(String userName, byte[] userPublicKeyEncoded) throws RemoteException {
    System.out.println("📩 addUser() function request via rmi\n");
    Integer userId = random.nextInt(1000);
    while (true) {
      userId = random.nextInt(1000);
      try {
        RspList<Boolean> responses = this.dispatcher.callRemoteMethods(null, "proposedIdExistsBackend",
          new Object[] { userId }, new Class[] { Integer.class },
          new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
        if (!matchAllReplicaResponses(responses)) { break; }
      } catch (Exception e) {
        System.err.println("🆘 addUser() -> checking for ID. dispatcher exception:");
        e.printStackTrace();
      }
    }
    try {
      System.out.printf("✅ ID validated, proceding to add user (%d, %s) to the database\n", userId, userName);
      RspList<Integer> responses = this.dispatcher.callRemoteMethods(null, "addUserBackend",
        new Object[] { userId, userName, userPublicKeyEncoded }, new Class[] { Integer.class, String.class, byte[].class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 addUser() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Adds an auction listing to a list of double auctions by item type.
   */
  public void addBuyerForDoubleAuction(Integer userId, String itemType, Float bid) throws RemoteException {
    System.out.println("📩 addBuyerForDoulbeAuction() function request via rmi\n");
    try {
      RspList<HashMap<Integer, HashMap<Integer, String>>> responses = this.dispatcher.callRemoteMethods(null, "addBuyerForDoubleAuctionBackend",
        new Object[] { userId, itemType, bid },
        new Class[] { Integer.class, String.class, Float.class},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      HashMap<Integer, HashMap<Integer, String>> response = matchAllReplicaResponses(responses);
      if (response != null && !response.isEmpty()) {
        notifyAllDoubleAuctionUsers(response);
      }
    } catch (Exception e) {
      System.err.println("🆘 addBuyerForDoubleAuction() dispatcher exception:");
      e.printStackTrace();
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
                                        Float startPrice) throws RemoteException {

    System.out.println("📩 addSellerForDoulbeAuction() function request via rmi\n");
    try {
      RspList<HashMap<Integer, HashMap<Integer, String>>> responses = this.dispatcher.callRemoteMethods(null, "addSellerForDoubleAuctionBackend",
        new Object[] { userId, itemName, itemType, itemDesc, itemCond, resPrice, startPrice },
        new Class[] { Integer.class, String.class,
                      String.class, String.class,
                      Integer.class, Float.class, Float.class},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      HashMap<Integer, HashMap<Integer, String>> response = matchAllReplicaResponses(responses);
      if (response != null && !response.isEmpty()) {
        notifyAllDoubleAuctionUsers(response);
      }
    } catch (Exception e) {
      System.err.println("🆘 addSellerForDoubleAuction() dispatcher exception:");
      e.printStackTrace();
    }
  }

  /*
   * Method for RMI
   *
   * Removes auction listing from server's global list
   */
  public AuctionListing closeAuction(Integer listingId, String itemType,
                                     Integer userId) throws RemoteException {
    System.out.println("📩 closeAuction() function request via rmi\n");
    try {
      RspList<AuctionListing> responses = this.dispatcher.callRemoteMethods(null, "closeAuctionBackend",
        new Object[] { listingId, itemType, userId }, new Class[] { Integer.class, String.class, Integer.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 closeAuction() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Create an auction for a given item's details.
   */
  public AuctionListing openAuction(
      Integer userId, String itName, String itType, String itDesc,
      Integer itCond, Float resPrice, Float startPrice
  ) throws RemoteException {
    System.out.println("📩 openAuction() function request via rmi\n");
    try {
      RspList<AuctionListing> responses = this.dispatcher.callRemoteMethods(null, "openAuctionBackend",
        new Object[] { userId, itName, itType, itDesc, itCond, resPrice, startPrice }, new Class[]
        { Integer.class, String.class,
          String.class, String.class,
          Integer.class, Float.class,
          Float.class
        },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 openAuction() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Generate a formatted list containing all auctioned items
   * of a specific type.
   */
  public String retrieveItemsByType(String type) throws RemoteException {
    System.out.println("📩 retrieveItemsByType() function request via rmi\n");
    try {
      RspList<String> responses = this.dispatcher.callRemoteMethods(null, "retrieveItemsByTypeBackend",
        new Object[] { type }, new Class[] { String.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 getSpec() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Return details from a requested item by ID.
   */
  public AuctionItem getSpec(Integer itemId, String clientId) throws RemoteException {
    System.out.println("📩 getSpec() function request via rmi\n");
    try {
      RspList<AuctionItem> responses = this.dispatcher.callRemoteMethods(null, "getSpecBackend",
        new Object[] { itemId, clientId }, new Class[] { Integer.class, String.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 getSpec() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Place a bid.
   * @return Boolean: whether the bid was placed succesfully (auction ID exists)
   */
  public Boolean placeBid(Integer userId, Integer auctionListingId, Float bid) throws RemoteException {
    System.out.println("📩 placeBid() function request via rmi\n");
    try {
      RspList<Boolean> responses = this.dispatcher.callRemoteMethods(null, "placeBidBackend",
        new Object[] { userId, auctionListingId, bid }, new Class[] { Integer.class, Integer.class, Float.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 placeBid() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Checks whether an id is amongst existing auctions.
   */
  public Boolean idMatchesExistingItem(Integer id) throws RemoteException {
    System.out.println("📩 idMatchesExistingItem() function request via rmi\n");
    try {
      RspList<Boolean> responses = this.dispatcher.callRemoteMethods(null, "idMatchesExistingItemBackend",
        new Object[] { id }, new Class[] { Integer.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 idMatchesExistingItem() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Checks if the price prompted by a buyer exceeds the starting/current price for item
   */
  public Boolean isBidPriceAcceptable(Integer listingId, Float price)
      throws RemoteException {
    System.out.println("📩 isBidPriceAcceptable() function request via rmi\n");
    try {
      RspList<Boolean> responses = this.dispatcher.callRemoteMethods(null, "isBidPriceAcceptableBackend",
        new Object[] { listingId, price }, new Class[] { Integer.class, Float.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 isBidPriceAcceptable() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Method for RMI
   *
   * Sends complete list of auctioned items (forward auction)
   */
  public String getAuctionedItems() throws RemoteException {
    System.out.println("📩 getAuctionedItems() function request via rmi\n");
    try {
      RspList<String> responses = this.dispatcher.callRemoteMethods(null, "getAuctionedItemsBackend",
        new Object[] {}, new Class[] {},
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 getAuctionedItems() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  private AuctionUser getUser(Integer userId) {
    try {
      RspList<AuctionUser> responses = this.dispatcher.callRemoteMethods(null, "getUserByInt",
        new Object[] { userId }, new Class[] { Integer.class },
        new RequestOptions(ResponseMode.GET_ALL, this.DISPATCHER_TIMEOUT));
      return matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 getUser() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /*
   * Check whether replicas return the same response
   */
  private <T> T matchAllReplicaResponses(RspList<T> responses) {
    if (responses.isEmpty()) {
      return null;
    }
    T firstResponse = responses.getFirst();
    for (T response : responses.getResults()) {
      if (!firstResponse.equals(response)) {
        return null;
      }
    }
    return firstResponse;
  }

  /*
   * Method for RMI
   *
   * Two-way digital signature handshake to verify user's identity
   * and send an acknowledgement signature back to the user.
   *
   * Hybrid encripyion -> symmetric AES key + asymmetric RSA signature.
   */
  public List<byte[]> verifyClientSignature(byte[] encryptedAES, byte[] encryptedSignature,
    String originalMessage, Integer userId, String originalSignatureHashDigest) throws RemoteException {
    System.out.println("📩 verifyClientSignature() function request via rmi\n");
    CryptoManager cryptoManager = new CryptoManager();
    Cipher cipher;
    Signature signature;
    AuctionUser user = getUser(userId);
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
}
