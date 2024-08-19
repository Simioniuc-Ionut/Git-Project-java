import javax.xml.crypto.dsig.DigestMethod;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.*;

public class Main {
  public static void main(String[] args){
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    // System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage

     final String command = args[0];

     switch (command) {
       case "init" -> {
         final File root = new File(".git");
         new File(root, "objects").mkdirs();
         new File(root, "refs").mkdirs();
         final File head = new File(root, "HEAD");

         try {
           head.createNewFile();
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           System.out.print("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
       }
       case "cat-file" -> Git.catFile(args[2],args[1],"blob");
       case "hash-object" ->{
         int argumentsLength = args.length;
         File fileReaded = new File(args[argumentsLength-1]);

        try {
          //declaration zone
          String content = Files.readString(fileReaded.toPath());
          byte[] size = content.getBytes();

          String resultObject,type="";
          StringBuilder path= new StringBuilder();// path where to write file
          MessageDigest  instance = MessageDigest.getInstance("SHA-1"); //make an instance to get SHA-1 hash for file name and directory
          byte[] hash;
          int index=1;


          //options
          while(index<argumentsLength - 1){

            if(args[index].contains("-t")){
              String typeRead = args[index].substring(2);
              if(typeRead.equals("tag") ||typeRead.equals("blob") || typeRead.equals("commit") || typeRead.equals("tree")){
                type = typeRead.strip();
              }
            }else if(args[index].contains("-w")){

              path.append(".git/objects/");
            }
            index++;
          }
          if(type.isEmpty()){
            type="blob";
          }

          //result obj
          resultObject=type + " " +content.length()+ "\0" + content;

          //compute SHA-1
          hash = instance.digest(resultObject.getBytes());
          //find directory and filename
          String hashHexa = bytesToHex(hash);
          String directoryName = hashHexa.substring(0,2);

          String filename = hashHexa.substring(2);
          //compute path to directory
          path.append(directoryName);

          //cream directorul
          File directory = new File(path.toString());
          boolean isCreated = directory.mkdir();
          if(!isCreated){
            return ;
          }
          //compute path to file
           path.append("/").append(filename);

          //compriming content of file using zlib
          try(FileOutputStream fileOutputStream = new FileOutputStream(path.toString());
                  DeflaterOutputStream compreserFile = new DeflaterOutputStream(fileOutputStream)) {
                  compreserFile.write(resultObject.getBytes());
                  compreserFile.finish();
          }

          System.out.print(hashHexa);
        }catch (IOException | NoSuchAlgorithmException e){
          throw new RuntimeException(e);
        }

       }
       case "ls-tree" ->  {
         if(args[1].equals("--name-only")){
           Git.catFile(args[2],args[1],"tree");
         }else{
           Git.catFile(args[1],"","tree");
         }
        }
       default -> System.out.println("Unknown command: " + command);
     }
  }

  // Helper method to convert byte array to hexadecimal string
  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
