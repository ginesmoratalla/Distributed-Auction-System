import org.jgroups.JChannel;
import org.jgroups.util.RspList;

import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RequestOptions;

import java.util.Collection;
import java.util.HashMap;

public class GroupUtils {

  /**
   * Returns a JGroup Channel in which a connection has already been established.
   * The channel name is taken from the "GROUP" env var, or a default is used if
   * no var present. note: this channel will discard self messages.
   * 
   * @return the connected jgroups channel or null if an error occurred.
   */
  public static JChannel connect() {
    String channelName = System.getenv("GROUP") == null ? "DEFAULT_GROUP" : System.getenv("GROUP");
    try {
      JChannel channel = new JChannel();
      channel.connect(channelName);
      System.out.printf("✅ connected to jgroups channel: %s\n", channelName);
      channel.setDiscardOwnMessages(true);
      return channel;
    } catch (Exception e) {
      System.err.printf("🆘 could not connect to jgroups channel: %s\n", channelName);
    }
    return null;
  }

  /**
   * Check whether backend replicas return the same response
   * @return if consistent across replicas, returns the T object
   */
  public static <T> T matchAllReplicaResponses(RspList<T> responses) {
    if (responses.isEmpty()) return null; T firstResponse = responses.getFirst();
    for (T response : responses.getResults()) {
      if (!firstResponse.equals(response)) { return null; }
    }
    return firstResponse;
  }

  public static <T> T executeBackendReplicaCall(
                                          String nodeType,
                                          String backendMethodName,
                                          T valueType,
                                          Object[] params,
                                          Class<?>[] paramTypes,
                                          RpcDispatcher dispatcher,
                                          int dispTimeout,
                                          Boolean isNotificationForSubs
  ) {
    System.out.println("📩 " + nodeType + " " + backendMethodName + "() function request via rmi\n");
    try {
      RspList<T> responses = dispatcher.callRemoteMethods(null, backendMethodName,
        params, paramTypes,
        new RequestOptions(ResponseMode.GET_ALL, dispTimeout));

      if (isNotificationForSubs) {
        return GroupUtils.matchDoubleAuctionResponses(responses);
      }
      return GroupUtils.matchAllReplicaResponses(responses);
    } catch (Exception e) {
      System.err.println("🆘 " + nodeType + " " + backendMethodName + "() dispatcher exception:");
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Method specific for double auction return notifications 
   * for pub-sub system
   */
  public static <T> T matchDoubleAuctionResponses(RspList<T> responses) {
    if (responses.isEmpty()) return null;
    T firstResponse = responses.getFirst();
    for (T response : responses.getResults()) {
      if (firstResponse instanceof HashMap<?,?> && response instanceof HashMap<?,?>) {
        HashMap<?,?> firstResponseMap = (HashMap<?,?>) firstResponse;
        HashMap<?,?> responseMap = (HashMap<?,?>) response;
        if (firstResponseMap.values() instanceof HashMap<?,?> && responseMap.values() instanceof HashMap<?,?>) {
          HashMap<?,?> nestedFirstResponseMap = (HashMap<?,?>) firstResponseMap;
          HashMap<?,?> nestedResponseMap = (HashMap<?,?>) responseMap;
          if (!nestedFirstResponseMap.values().equals(nestedResponseMap.values())) {
            return null; 
          }
        }
      }
    }
    return firstResponse;
  }
}
