import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
 * Input manager for client's stdin commands
 */
public class ClientInputManager {

  public static final String INPUT_ERROR = "[INPUT ERROR]: Not a valid input type, please try again: ";
  public static final String MAIN_MENU_OPERATIONS = 
      "\n----- Available Operations -----"
      + "\n--------------------------------"
      + "\n1. Create auction for an item"
      + "\n2. Close your auction"
      + "\n3. Select forward auction mode"
      + "\n4. Select reverse auction mode"
      + "\n5. Select double auction mode"
      + "\n0. Exit"
      + "\n--------------------------------"
      + "\n--------------------------------"
      + "\n";

  public static final String DOUBLE_AUCTION_OPERATIONS = 
      "\n--- Double auction operations ---"
      + "\n---------------------------------"
      + "\n1. Sell an item"
      + "\n2. Place a bid"
      + "\n0. Return to home"
      + "\n---------------------------------"
      + "\n---------------------------------"
      + "\n";

  public static final String REGULAR_AUCTION_OPERATIONS = 
      "\n--- Item operations ---"
      + "\n-----------------------"
      + "\n1. Get item specs"
      + "\n2. Place a bid on the item"
      + "\n3. Select a different item"
      + "\n0. Return to home"
      + "\n-----------------------"
      + "\n-----------------------"
      + "\n";

  public static final String ITEM_USAGE_SCALE = "\nIs your item new or used?"
      + "\nRate its usage in a scale:\n"
      + "1. New\n"
      + "2. Barely used\n"
      + "3. Used\n"
      + "4. Moderately used\n"
      + "5. Heavily used\n"
      + "PSA: Anything outside of this scale will default to \"Used\": ";

  public static String printUserPersonalAuctions(String userName, HashMap<Integer, AuctionListing> userAuctions) {

    String inputMessage = "\n--- " + userName + "'s auctioned items ---\n";
    String barrier = "-".repeat(inputMessage.length() - 2) + "\n";
    String returnString = inputMessage + barrier;

    for (Map.Entry<Integer, AuctionListing> entry : userAuctions.entrySet()) {
      returnString += "ID: " + entry.getKey()
                      + " | item: " + entry.getValue().getItem().getItemTitle()
                      + "\n";
    }
    returnString += barrier + barrier;
    return returnString;
  }

  public String getStringFromClient(Scanner input) {
    String result = null;
    while (true) {
      try {
        result = input.nextLine();
        break;
      } catch (Exception e) {
        System.out.print(INPUT_ERROR);
      }
    }
    return result;
  }

  public Float getFloatFromClient(Scanner input) {
    Float result = null;
    while (true) {
      try {
        result = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print(INPUT_ERROR);
      }
    }
    return result;
  }

  public Integer getIntegerFromClient(Scanner input) {
    Integer result = null;
    while (true) {
      try {
        String userInput = input.nextLine();
        if (userInput.equals("exit")) return -1;
        result = Integer.parseInt(userInput);
        break;
      } catch (Exception e) {
        System.out.print(INPUT_ERROR);
      }
    }
    return result;
  }

  public Integer getOperation(String operation) {
    try {
      Integer opInteger = Integer.parseInt(operation);
      return opInteger;
    } catch (Exception e) {
      return -1;
    }
  }
}
