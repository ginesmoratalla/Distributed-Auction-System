import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuctionSystem extends Remote {

  public AuctionItem getSpec(int itemId, String clientId)
      throws RemoteException;

  public Integer openAuction(Integer userId, String itName, String itDesc,
      Integer itCond, Float resPrice, Float startPrice)
      throws RemoteException;

  public AuctionListing closeAuction(Integer userId, Integer listingId)
      throws RemoteException;

  public Integer addUser(String userName) throws RemoteException;

  public String getAuctionedItems() throws RemoteException;

  public void placeBid(Integer userId, Integer auctionListingId, Float bid)
      throws RemoteException;

  public Boolean idMatchesExistingItem(Integer id) throws RemoteException;
  public Boolean userNameExists(String uName) throws RemoteException;
}
