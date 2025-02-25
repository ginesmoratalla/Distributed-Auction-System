
/*
 * Enum representing item categories
 */
public enum AuctionItemTypeEnum {
  COMPUTER("Computer"),
  BOOK("Book"),
  PHONE("Phone"),
  COAT("Coat"),
  SHOES("Shoes"),
  TSHIRT("Tshirt"),
  JEANS("Jeans"),
  HOODIE("Hoodie"),
  SPORTS_JERSEY("Sports jersey"),
  MISC("Miscellaneous");

  private final String typeStr;
  AuctionItemTypeEnum(String typeStr) {
    this.typeStr = typeStr;
  }
  public String getValue() { return this.typeStr; }
}

