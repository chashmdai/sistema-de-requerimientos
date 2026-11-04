import java.security.*;
import java.util.Base64;
class IdentitySmr {
 public static void main(String[] args)throws Exception{
  var pair=KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
  System.out.println(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
  System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
 }
}
