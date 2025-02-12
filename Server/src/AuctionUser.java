public class AuctionUser {
  private Integer userId;
  private String userName;
  private String password;

  public AuctionUser(Integer userId, String userName, String password) {
    this.userId = userId;
    this.userName = userName;
    this.password = password;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public String getUserName() {
    return this.userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
