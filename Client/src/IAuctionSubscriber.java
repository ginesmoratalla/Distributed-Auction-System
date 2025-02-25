import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface acting as remote RMI object
 *
 * Pub-Sub subscriber recieving double auction messages
 * from the auction server
 */
public interface IAuctionSubscriber extends Remote {
  public void getMessage(Integer userId, String message) throws RemoteException;
}
