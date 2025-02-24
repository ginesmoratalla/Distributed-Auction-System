import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuctionSubscriber extends Remote {
  public void getMessage(Integer userId, String message) throws RemoteException;
}
