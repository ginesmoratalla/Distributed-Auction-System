import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuctionSubscriber extends Remote {
  public void getMessage(String message) throws RemoteException;
}
