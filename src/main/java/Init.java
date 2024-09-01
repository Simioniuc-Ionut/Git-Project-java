import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Init {
    private static final byte[] SPACE_BYTES = {' '};
    private static final byte[] NULL_BYTES = {0};
    private final Path root;
    public  Path getDotGit() { return root.resolve(".git"); }
    public  Path getObjectsDirectory() { return getDotGit().resolve("objects"); }
    public  Path getRefsDirectory() { return getDotGit().resolve("refs"); }
    public  Path getHeadFile() { return getDotGit().resolve("HEAD"); }
    public  Path getConfigFile() { return getDotGit().resolve("config"); }
    public Init(Path root) { this.root = root; }

    public static Git init(Path root) throws IOException {
        final var git = new Init(root);
        final var dotGit = git.getDotGit();
        if (Files.exists(dotGit)) {
            throw new FileAlreadyExistsException(dotGit.toString());
        }
        Files.createDirectories(git.getObjectsDirectory());
        Files.createDirectories(git.getRefsDirectory());
        final var head = git.getHeadFile();
        Files.createFile(head);
        Files.write(head, "ref: refs/heads/master\n".getBytes());
        final var config = git.getConfigFile();
        Files.createFile(config);
        Files.write(config, ("[core]\n        autocrlf = false").getBytes());
        return git;
    }
}
