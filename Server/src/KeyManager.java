import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyManager {

  public static KeyPair generateRSAKeys() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      // PrivateKey privateKey = keyPair.getPrivate();
      // PublicKey publicKey = keyPair.getPublic();

      // private key
      // try (ObjectOutputStream privateKeyStream =
      //          new ObjectOutputStream(new FileOutputStream("auction_rsa"))) {

      //   privateKeyStream.writeObject(privateKey);
      // }

      // public key
      // try (ObjectOutputStream publicKeyStream = new ObjectOutputStream(
      //          new FileOutputStream("auction_rsa.pub"))) {

      //   publicKeyStream.writeObject(publicKey);
      // }

      System.out.println("Keys have been generated successfully.");
      return keyPair;

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "[ERROR]: Problem generating the rsa key-pair");
    }
    return null;
  }

  public static PrivateKey loadPrivateKey(String dir) {
    PrivateKey privK = null;
    try (ObjectInputStream privateKeyStream =
             new ObjectInputStream(new FileInputStream(dir))) {
      privK = (PrivateKey) privateKeyStream.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("Private Key Loaded Succesfully");
    return privK;
  }

  public static PublicKey loadPublicKey(String dir) {
    PublicKey pubK = null;
    try (ObjectInputStream publicKeyStream =
             new ObjectInputStream(new FileInputStream(dir))) {
      pubK = (PublicKey) publicKeyStream.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Public Key Loaded Succesfully");
    return pubK;
  }
}
