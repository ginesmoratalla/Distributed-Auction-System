import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuctionSystem extends Remote {
  public AuctionItem getSpec(int itemId, String clientId)
      throws RemoteException;

  public Integer openAuction(String userName, String itName, String itDesc, Integer itCond,
      Float resPrice, Float startPrice)
      throws RemoteException;

  public AuctionListing closeAuction(Integer userId, Integer listingId) throws RemoteException;
  public Integer addUser() throws RemoteException;
}
