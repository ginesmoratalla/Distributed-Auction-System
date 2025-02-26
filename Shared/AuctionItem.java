import java.io.Serializable;

public class AuctionItem implements Serializable {

  private String itemCondition;
  private int itemId;
  private String itemTitle;
  private String itemDescription;
  private String itemType;


  /*
   * Class representing an auctioned item
   */
  public AuctionItem(Integer itemId, String itemTitle, String itemType, String itemDescription,
                     Integer conditionScale)
  {
    this.itemId = itemId;
    this.itemTitle = itemTitle;
    this.itemDescription = itemDescription;
    this.itemType = itemType;
    setItemCondition(conditionScale);
  }

  public Integer getItemId() { return itemId; }
  public void setItemId(Integer itemId) { this.itemId = itemId; }

  public String getItemTitle() { return itemTitle; }
  public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }

  public String getItemDescription() { return itemDescription; }
  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }
  public String getItemType() {
    return this.itemType;
  }

  public void setItemCondition(Integer conditionScale) {
    switch (conditionScale) {
      case 5:
        this.itemCondition = "Heavily used.";
        break;
      case 4:
        this.itemCondition = "Moderately used.";
        break;
      case 3:
        this.itemCondition = "Used.";
        break;
      case 2:
        this.itemCondition = "Barely used.";
        break;
      case 1:
        this.itemCondition = "New.";
        break;
      default:
        System.out.println("Chosen scale indicator not in the scale bounds." +
          " Defaulting to \"Used.\"");
        this.itemCondition = "Used.";
    }
  }

  public String getItemCondition() {
    return this.itemCondition;
  }
}
