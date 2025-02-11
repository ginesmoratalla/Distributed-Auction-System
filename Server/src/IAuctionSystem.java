import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuctionSystem extends Remote {

  public AuctionListing openAuction(Integer userId, String itName, String itType, String itDesc,
      Integer itCond, Float resPrice, Float startPrice) throws RemoteException;
  public AuctionListing closeAuction(Integer listingId, String itemType, Integer userId) throws RemoteException;

  public AuctionItem getSpec(Integer itemId, String clientId) throws RemoteException;
  public Integer addUser(String userName) throws RemoteException;
  public void placeBid(Integer userId, Integer auctionListingId, Float bid) throws RemoteException;

  public Boolean isBidPriceAcceptable(Integer id, Float price) throws RemoteException;
  public Boolean idMatchesExistingItem(Integer id) throws RemoteException;
  public Boolean userNameExists(String userName) throws RemoteException;
  public Boolean itemTypeExists(String typeStr) throws RemoteException;

  public String retrieveItemsByType(String type) throws RemoteException;
  public String retrieveItemTypes() throws RemoteException;
  public String getAuctionedItems() throws RemoteException;
}
