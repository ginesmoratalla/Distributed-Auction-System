import java.security.PublicKey;

public class AuctionUser {
  private Integer userId;
  private String userName;
  private String password;
  private PublicKey publicKey;

  /*
   * Server's representation of an auction user
   *
   * Handle's clients private information in the server
   */
  public AuctionUser(Integer userId, String userName, String password,
                     PublicKey publicKey) {

    this.userId = userId;
    this.userName = userName;
    this.password = password;
    this.publicKey = publicKey;
  }

  public Integer getUserId() { return this.userId; }

  public String getUserName() { return this.userName; }

  public void setUserName(String userName) { this.userName = userName; }

  public String getPassword() { return password; }

  public void setPassword(String password) { this.password = password; }

  public void setUserId(Integer userId) { this.userId = userId; }

  public PublicKey getPublicKey() { return this.publicKey; }
}
