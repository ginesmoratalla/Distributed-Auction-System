import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuctionSystem extends Remote {
  public AuctionItem getSpec(int itemId, String clientId)
      throws RemoteException;

  public Integer openAuction(String userName, String itName, String itDesc, Integer itCond,
      Float resPrice, Float startPrice)
      throws RemoteException;

  public Float closeAuction(String userName, Integer listingId) throws RemoteException;
}
