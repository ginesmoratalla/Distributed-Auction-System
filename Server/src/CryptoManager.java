import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class CryptoManager {

  public static KeyPair generateRSAKeys() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      System.out.println("[KEY GENERATION] RSA Key-pair generated successfully.");
      return keyPair;

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "[KEY GENERATION ERROR]: Problem generating the rsa key-pair");
    }
    return null;
  }

  public static PrivateKey loadPrivateKey(String dir) {
    PrivateKey privK = null;
    try (ObjectInputStream privateKeyStream =
             new ObjectInputStream(new FileInputStream(dir))) {

      privK = (PrivateKey) privateKeyStream.readObject();
      System.out.println("[SECURITY] Server Private RSA Key Loaded Succesfully");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "[KEY LOADING ERROR]: Problem loading Server's Private RSA key.");
    }
    return privK;
  }

  public static PublicKey loadPublicKey(String dir) {
    PublicKey pubK = null;
    try (ObjectInputStream publicKeyStream =
             new ObjectInputStream(new FileInputStream(dir))) {

      pubK = (PublicKey) publicKeyStream.readObject();
      System.out.println("[SECURITY] Server Public RSA Key Loaded Succesfully");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "[KEY LOADING ERROR]: Problem loading Server's Public RSA key.");
    }
    return pubK;
  }

  public String byteArrayToHex(byte[] byteArray) {
    StringBuilder hexStringBuilder = new StringBuilder();
    for (byte singularByte : byteArray) {
      hexStringBuilder.append(String.format("%02x", singularByte));
    }
    return hexStringBuilder.toString();
  }
}
