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
        File testFile1 = new File(tempDir.toFile(), "test_file_1.txt");
        File dir1 = new File(tempDir.toFile(), "test_dir_1");
        File testFile2 = new File(dir1, "test_file_2.txt");
        File dir2 = new File(tempDir.toFile(), "test_dir_2");
        File testFile3 = new File(dir2, "test_file_3.txt");

        // Create directories and files
        Files.writeString(testFile1.toPath(), "hello world");
        dir1.mkdir();
        Files.writeString(testFile2.toPath(), "hello world");
        dir2.mkdir();
        Files.writeString(testFile3.toPath(), "hello world");

        // Call the itereateDirectory method
        String treeSha = Git.itereateDirectory(tempDir.toFile());

        System.out.println(treeSha);

        // Verify the output
        Assertions.assertNotNull(treeSha);
        Assertions.assertFalse(treeSha.isEmpty());

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