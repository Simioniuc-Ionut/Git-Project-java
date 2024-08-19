import com.sun.source.tree.InstanceOfTree;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MainTest {

    private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
    }

    public static void test1(){
        try {
            MessageDigest dig = MessageDigest.getInstance("SHA-1");
            String nume = "ceva mai lung";
            byte[] hash = dig.digest(nume.getBytes());
            String hashHexa =bytesToHex(hash);
            System.out.println("input: "+ nume + " hash " + hashHexa);

            Deflater def = new Deflater();
            def.setInput(nume.getBytes());
            def.finish();
            System.out.println("Compresed: " + def.toString());

            byte[] compresedData = new byte[1024];
            int length = def.deflate(compresedData);
            def.end();

            Inflater inf = new Inflater();
            inf.setInput(compresedData, 0, length);


            byte[] output = new byte[1024];
            int outlength = inf.inflate(output);
            inf.end();

            String decompresedData = new String(output,0,outlength);
            System.out.println("Decompresed : " + decompresedData + " hash " + hashHexa);

        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
    public  static  void test2(){
        // Example content of a Git tree object
        String content =
        "040000 tree c43da8ed93e41af84dc08cffbce3a907e09159b8\0.codecrafters";

        // Example usage
        GitFunctions.processTreeContent(content);
    }
    public static void main(String[] args) {
        //compriming and decompriming an string. and create an sha-1 hash
        MainTest.test1();

        // ls-tree command:
        MainTest.test2();

    }

}
