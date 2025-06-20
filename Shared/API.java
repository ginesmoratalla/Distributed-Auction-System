import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/*
 * Remote object acting as auction server stub
 */
public interface API extends Remote {

  public AuctionListing openAuction(Integer userId, String itName,
      String itType, String itDesc,
      Integer itCond, Float resPrice,
      Float startPrice) throws RemoteException;

  public AuctionListing closeAuction(Integer listingId, String itemType,
      Integer userId) throws RemoteException;

  public AuctionItem getSpec(Integer itemId, String clientId)
      throws RemoteException;

  public Integer addUser(String userName, byte[] userPublicKey)
      throws RemoteException;

  public Boolean placeBid(Integer userId, Integer auctionListingId, Float bid)
      throws RemoteException;

  public Boolean isBidPriceAcceptable(Integer id, Float price)
      throws RemoteException;

  public Boolean idMatchesExistingItem(Integer id) throws RemoteException;

  public Boolean userNameExists(String userName) throws RemoteException;

  public Boolean itemTypeExists(String typeStr) throws RemoteException;

  public String retrieveItemsByType(String type) throws RemoteException;

  public String retrieveItemTypes() throws RemoteException;

  public String getAuctionedItems() throws RemoteException;

  // Double Auctions
  public void addSellerForDoubleAuction(Integer userId, String itemName,
      String itemType, String itemDesc,
      Integer itemCond, Float resPrice,
      Float startPrice)
      throws RemoteException;

  public void addBuyerForDoubleAuction(Integer userId, String itemType,
      Float bid) throws RemoteException;

  public List<byte[]> verifyClientSignature(byte[] encryptedAES, byte[] encryptedSignature,
      String originalMessage, Integer userId,
      String originalSignatureHashDigest)
      throws RemoteException;

  public void registerSubscriber(Integer userId, IAuctionSubscriber subscriber)
      throws RemoteException;
}
