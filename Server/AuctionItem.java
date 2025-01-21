import java.io.Serializable;
public class AuctionItem implements Serializable {

  private int itemId;
  private String itemTitle;
  private String itemDescription;

  public AuctionItem(int itemId, String itemTitle, String itemDescription) {
    this.itemId = itemId;
    this.itemTitle = itemTitle;
    this.itemDescription = itemDescription;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public String getItemTitle() {
    return itemTitle;
  }

  public void setItemTitle(String itemTitle) {
    this.itemTitle = itemTitle;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }
}
