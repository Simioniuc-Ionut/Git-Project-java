import javax.xml.crypto.dsig.DigestMethod;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.zip.*;

public class Main {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Expected command: init, cat-file, hash-object, ls-tree, write-tree");
      return;
    }

    String command = args[0];

    try {
      switch (command) {
        case "init" -> initializeGitRepository();
        case "cat-file" ->  handleCatFileCommand(args);
        case "hash-object" -> handleHashObjectCommand(args);
        case "ls-tree" -> handleLsTreeCommand(args);
        case "write-tree" -> handleWriteTreeCommand();
        case "commit-tree" -> handleCommitTreeCommand(args);
        case "clone" -> handleCloneCommand(args);
        default -> System.out.println("Unknown command: " + command);
      }
    } catch (Exception e) {
      System.err.println("Error executing command: " + e.getMessage());
    }
  }

  private static void initializeGitRepository() {
    File root = new File(".git");
    if (root.exists()) {
      System.out.println(".git directory already exists.");
      return;
    }

    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
    File head = new File(root, "HEAD");

    try {
      head.createNewFile();
      Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
      System.out.println("Initialized git directory");
    } catch (IOException e) {
      throw new RuntimeException("Error initializing git repository", e);
    }
  }

  private static void handleCatFileCommand(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: cat-file <option> <hash>");
      return;
    }

    Git.displayGitObject(args[2], args[1]);
  }

  private static void handleHashObjectCommand(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: hash-object <file>");
      return;
    }
    byte[] sha = Git.createGitBlob(args);
    Git.printShaInHexaMode(sha);
  }

  private static void handleLsTreeCommand(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: ls-tree <tree-hash> [--name-only]");
      return;
    }
    if (args[1].equals("--name-only")) {
      Git.displayGitObject(args[2], args[1]);
    } else {
      Git.displayGitObject(args[1], "");
    }
  }

  private static void handleWriteTreeCommand() {
    Path path = Paths.get("").toAbsolutePath();
    File gitDir = new File(path.toString(), ".git");
    if (!gitDir.exists()) {
      System.out.println("Error: .git directory not found. Please run 'init' command first.");
      return;
    }

    try {
      byte[] shaTree = Git.processDirectory(new File(path.toString()));
      Git.printShaInHexaMode(shaTree);
    } catch (Exception e) {
      throw new RuntimeException("Error processing directory", e);
    }
  }

  private static void handleCommitTreeCommand(String[] args) {
    if (args.length < 4) {
      System.out.println("Usage: commit-tree <tree-hash> -m <message>");
      return;
    }
    //git commit-tree <sha1-tree> | -p <sha1-parent-tree> | -m "message"
    Git.createCommit(args);
  }

  private static void handleCloneCommand(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: clone <repository-url>");
      return;
    }
      try {
          Git.cloneRepository(args[1],args[2]);
      } catch (Exception e) {
          System.out.println("Error cloning repository: " + e.getMessage());
      }
  }


}