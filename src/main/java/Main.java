import javax.xml.crypto.dsig.DigestMethod;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.*;
//
//public class Main {
//  public static void main(String[] args){
//    // You can use print statements as follows for debugging, they'll be visible when running tests.
//    // System.out.println("Logs from your program will appear here!");
//
//    // Uncomment this block to pass the first stage
//
//     final String command = args[0];
//
//     switch (command) {
//       case "init" -> {
//         final File root = new File(".git");
//         new File(root, "objects").mkdirs();
//         new File(root, "refs").mkdirs();
//         final File head = new File(root, "HEAD");
//
//         try {
//           head.createNewFile();
//           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
//           System.out.print("Initialized git directory");
//         } catch (IOException e) {
//           throw new RuntimeException(e);
//         }
//       }
//       case "cat-file" -> Git.catFile(args[2],args[1],"blob");
//       case "hash-object" -> {
//         byte[] sha = Git.hashObjectCreate(args);
//         Git.printShaInHexaMode(sha);
//       }
//       case "ls-tree" ->  {
//         if(args[1].equals("--name-only")){
//           Git.catFile(args[2],args[1],"tree");
//         }else{
//           Git.catFile(args[1],"","tree");
//         }
//       }
//       case "write-tree" ->{
//         // Ensure the current directory is the root of the repository
//         Path path = Paths.get("").toAbsolutePath();
//         File gitDir = new File(path.toString(), ".git");
//         if (!gitDir.exists()) {
//           System.out.println("Error: .git directory not found. Please run 'init' command first.");
//           return;
//         }
//
//         try {
//           byte[] shaTree = Git.itereateDirectory(new File(path.toString()));
//           Git.printShaInHexaMode(shaTree);
//         } catch (Exception e) {
//           throw new RuntimeException(e);
//         }
//       }
//       default -> System.out.println("Unknown command: " + command);
//     }
//  }
//
//}
public class Main {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Expected command:- init, cat-file, hash-object, ls-tree, write-tree");
      return;
    }
    final String command = args[0];
    switch (command) {
      case "init":
        git_init();
        break;
      case "cat-file":
        git_read(args);
        break;
      case "hash-object":
        git_write(args);
        break;
      case "ls-tree":
        git_tree(args);
        break;
      case "write-tree":
        git_write_tree();
        break;
      default:
        System.out.println("Unknown command: " + command);
        break;
    }
  }

  private static void git_init() {
    File root = new File(".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
  }

  private static void git_read(String[] args) {
    if (args.length < 3) return;
    try {
      Git.catFile(args[2], args[1], "");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void git_write(String[] args) {
    if (args.length < 3) return;
    try {
      Git.hashObjectCreate(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void git_tree(String[] args) {
    if (args.length < 3) return;
    try {
      new TreeObject(".", args[2]).readObject();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void git_write_tree() {
    try {
      new TreeObject(".", "").writeObject(".");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
