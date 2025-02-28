import org.jgroups.JChannel;
import org.jgroups.util.RspList;

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
    if (responses.isEmpty()) return null;
    T firstResponse = responses.getFirst();
    for (T response : responses.getResults()) {
      if (!firstResponse.equals(response)) { return null; }
    }
    return firstResponse;
  }
}
