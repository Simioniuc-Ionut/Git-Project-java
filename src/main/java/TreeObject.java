import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class TreeObject extends GitObjects {
    private static final byte[] TYPE = {'t', 'r', 'e', 'e'};

    public TreeObject(String repo, String hash) throws IOException {
        super(repo, hash);
    }

    @Override
    public String writeObject(String filename) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (Stream<Path> fileStream = Files.list(Path.of(filename))) {
            fileStream.filter(p -> !p.getFileName().toString().equals(".git"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            if (Files.isDirectory(p)) {
                                buffer.write("40000 ".getBytes(StandardCharsets.UTF_8));
                            } else {
                                buffer.write("100644 ".getBytes(StandardCharsets.UTF_8));
                            }
                            buffer.write(p.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                            buffer.write(0);
                            byte[] sha = new BlobObject(".", "").writeObject(p.toString()).getBytes(StandardCharsets.UTF_8);
                            buffer.write(sha);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        fileContent = buffer.toByteArray();
        return writeObject(Path.of(filename), TYPE);
    }
}
