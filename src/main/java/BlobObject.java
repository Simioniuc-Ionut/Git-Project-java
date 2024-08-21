import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlobObject extends GitObjects {
    private static final byte[] TYPE = {'b', 'l', 'o', 'b'};

    public BlobObject(String repo, String hash) throws IOException {
        super(repo, hash);
    }

    @Override
    public String writeObject(String filename) throws IOException {
        return writeObject(Path.of(filename), TYPE);
    }
}
