import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

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
       case "cat-file" ->{
         String hash = args[2];
         String directory = hash.substring(0,2);
         String filename = hash.substring(2);
         String path = ".git/objects/" + directory + "/"+ filename;
         File object = new File(path);
         String result ,type,size,content;

         //decompresam bytes din object in format citibil ,folosind inflater.

         try(InflaterInputStream decompressor = new InflaterInputStream(Files.newInputStream(object.toPath()))){
           //an object is stored in the format <type>" "<size>\0<content>

           //prepare the object section
         byte[] data = decompressor.readAllBytes();
         result = new String(data);

         type = result.substring(0,result.indexOf(" "));
         size = result.substring(result.indexOf(" ") + 1 ,result.indexOf("\0"));
         content = result.substring(result.indexOf("\0")+1);

         //print section
         if(args[1] != null) {
           if (args[1].equals("-t"))
             System.out.print(type);
           else if (args[1].equals("-s"))
             System.out.print(size);
           else if (args[1].equals("-p"))
             System.out.print(content);
         }else {
             System.out.print(content);
         }

         }catch(IOException e){
           throw new RuntimeException(e);
         }
       }
       case "hash-object" ->{
         int argumentsLength = args.length;
         File fileReaded = new File(args[argumentsLength-1]);

        try {
          //declaration zone
          String content = Files.readString(fileReaded.toPath());
          byte[] size = content.getBytes();
          String contentSize = bytesToHex(size);
          String resultObject,type="";
          StringBuilder path= new StringBuilder();// path where to write file
          MessageDigest  instance = MessageDigest.getInstance("SHA-1"); //make an instance to get SHA-1 hash for file name and directory
          byte[] hash;
          int index=1;


          //options
          while(index<argumentsLength - 2){
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
          resultObject=type + " " + contentSize +"\0" + content;
          //compute SHA-1
          hash = instance.digest(resultObject.getBytes());
          //find directory and filename
          String hashHexa = bytesToHex(hash);
          String directory = hashHexa.substring(0,2);
          String filename = hashHexa.substring(2);
          //compute path
          path.append(directory).append("/").append(filename);
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
