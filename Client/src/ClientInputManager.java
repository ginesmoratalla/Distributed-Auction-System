import java.util.Scanner;

public class ClientInputManager {

  public String getStringFromClient(Scanner input) {
    String result = null;
    while(true) {
      try {
        result = input.nextLine();
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }
    return result;
  }

  public Float getFloatFromClient(Scanner input) {
    Float result = null;
    while(true) {
      try {
        result = Float.parseFloat(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
      }
    }
    return result;
  }

  public Integer getIntegerFromClient(Scanner input) {
    Integer result = null;
    while(true) {
      try {
        result = Integer.parseInt(input.nextLine());
        break;
      } catch (Exception e) {
        System.out.print("ERROR: Not a valid input type, please try again: ");
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
