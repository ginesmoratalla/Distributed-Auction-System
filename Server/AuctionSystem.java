import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuctionSystem extends Remote {
  public AuctionItem getSpec(int itemId, int clientId) throws RemoteException;
}
