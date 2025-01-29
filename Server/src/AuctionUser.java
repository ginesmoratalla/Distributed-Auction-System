public class AuctionUser {
  private String userName;
  private Integer userId;
  public AuctionUser(String name, Integer id) {
    this.userName = name;
    this.userId = id;
  }
  public String getUserName() {
    return userName;
  }
  public void setUserName(String userName) {
    this.userName = userName;
  }
  public Integer getUserId() {
    return userId;
  }
  public void setUserId(Integer userId) {
    this.userId = userId;
  }
}
