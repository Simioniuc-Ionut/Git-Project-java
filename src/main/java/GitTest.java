import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

public class GitTest {

    @Test
    public void testWriteTree() throws Exception {
        // Set up a temporary directory structure
        Path tempDir = Files.createTempDirectory("testRepo");
        File dir1 = new File(tempDir.toFile(), "dir1");
        File dir2 = new File(tempDir.toFile(), "dir2");
        File file1 = new File(dir1, "file1.txt");
        File file2 = new File(dir2, "file2.txt");

        dir1.mkdir();
        dir2.mkdir();
        Files.writeString(file1.toPath(), "content of file1");
        Files.writeString(file2.toPath(), "content of file2");

        // Call the itereateDirectory method
        String treeSha = Git.itereateDirectory(tempDir.toFile());

        // Verify the output
        assertNotNull(treeSha);
        assertFalse(treeSha.isEmpty());

        // Clean up
        deleteDirectory(tempDir.toFile());
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}