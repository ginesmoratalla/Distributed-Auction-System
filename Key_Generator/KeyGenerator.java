import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGenerator {

  public static void generateRSAKeys() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      // private key
      try (ObjectOutputStream privateKeyStream =
               new ObjectOutputStream(new FileOutputStream("server_auction_rsa"))) {

        privateKeyStream.writeObject(privateKey);
      }
      // public key
      try (ObjectOutputStream publicKeyStream = new ObjectOutputStream(
               new FileOutputStream("server_auction_rsa_pub"))) {

        publicKeyStream.writeObject(publicKey);
      }
      System.out.println("Keys have been saved successfully.");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "[ERROR]: Problem generating the digital signature");
    }
  }

  public static void main(String[] args) {
    generateRSAKeys();
  }
}
