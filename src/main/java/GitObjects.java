import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class GitObjects {
    protected Path file;
    protected byte[] fileContent;

    public GitObjects(String repo, String hash) throws FileNotFoundException {
        int length = hash.length();
        if (length > 2) {
            file = Path.of(repo, ".git", "objects", hash.substring(0, 2), hash.substring(2));
            if (!Files.exists(file)) {
                throw new FileNotFoundException();
            }
        }
    }

    public abstract String writeObject(String filename) throws IOException;

    public static byte[] sha1Digest(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(input);
    }

    public static String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    protected String writeObject(Path fileName, byte[] type) throws IOException, NoSuchAlgorithmException {
        fileContent = Files.readAllBytes(fileName);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(type);
        buffer.write(" ".getBytes(StandardCharsets.UTF_8));
        buffer.write(String.valueOf(fileContent.length).getBytes(StandardCharsets.UTF_8));
        buffer.write(0);
        buffer.write(fileContent);
        byte[] obj = buffer.toByteArray();
        byte[] sha = sha1Digest(obj);
        String shaHex = bytesToHex(sha);
        file = Path.of(".git", "objects", shaHex.substring(0, 2), shaHex.substring(2));
        Files.createDirectories(file.getParent());
        try (FileOutputStream fos = new FileOutputStream(file.toFile());
             DeflaterOutputStream dos = new DeflaterOutputStream(fos)) {
            dos.write(obj);
        }
        return shaHex;
    }
}
