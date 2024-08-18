import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
