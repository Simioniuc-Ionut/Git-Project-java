import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Git {
    //Create a clone of a repository from GitHub
    public static void cloneRepository(String gitURL) throws Exception{
        /**
         * 1. Ref Discovery
         * - **Purpose**: The client discovers the references (branches, tags, etc.) available on the server.
         * - **Process**:
         *     - The client sends an HTTP GET request to the server to obtain a list of available refs.
         */
        Map<String,String> refs = handleRefDirectory(gitURL);
        /**
         * ### 2. **Constructing the Request**
         *
         * - **Purpose**: The client specifies the objects it needs (`want`) and the objects it already has (`have`).
         * - **Process**:
         *     - The client sends a POST request to the server with the list of wanted objects.
         */
        ConstructingTheRequest(refs);


    }
    //Constructing the Request
    private static void ConstructingTheRequest(Map<String,String> refs) throws Exception {
        String gitUrl = "https://github.com/user/repo.git";
        URL url = new URL(gitUrl + "/git-upload-pack");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-git-upload-pack-request");

        //generally if i clone,i want all objects , bcs i dont have any object at the moment
        String requestBody = buildRequestBody(refs);
        //debug
        System.out.println(requestBody);
        // Write the request body to the server
        try (OutputStream os = connection.getOutputStream()){
            os.write(requestBody.getBytes());
            os.flush();
        }

        System.out.println("Response code: " + connection.getResponseCode());

    }
    private static String buildRequestBody(Map<String,String> refs) {
        StringBuilder requestBody = new StringBuilder();
        StringBuilder packet = new StringBuilder();
        //i will want hust unic sha1 from refs.
        Set<String> setUniqueSHA1 = new HashSet<>(refs.values());

        for(String sha1 : setUniqueSHA1){
            requestBody.append("0032want ").append(sha1).append("\n");
        }
        requestBody.append("0000");
        String length = Integer.toHexString(requestBody.length());
        packet.append(length).append("\0").append(requestBody);
        return packet.toString();
    }
    //GitRefsDirectory
    private static Map<String,String> handleRefDirectory(String gitURL)throws Exception{
        String refsContent = GetRefsDirectory(gitURL);
        //debug
        //System.out.println(refsContent);

        // Parse the refsContent to find the SHA-1 hash of the master branch
        Map<String,String> refs = parseMasterBranch(refsContent);
        //debug
        //for(Map.Entry<String,String> entry : refs.entrySet()){
        //      System.out.println(entry.getKey() + " : " + entry.getValue());
        //  }
        return refs;
    }
    private static String GetRefsDirectory(String gitURL) throws Exception{
        URL url = new URL(gitURL + "/info/refs?service=git-upload-pack");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Read the response from the server
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder RefsContent = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            RefsContent.append(inputLine);
        }
        in.close();

        return RefsContent.toString();

    }
    private static Map<String,String> parseMasterBranch(String refsContent) {
        Map<String,String> refs = new HashMap<>();
        for (int i = 0; i<refsContent.length(); i++) {
            //HEAD
            if(refsContent.charAt(i) == 'H' && refsContent.charAt(i+1) == 'E' && refsContent.charAt(i+2) == 'A' && refsContent.charAt(i+3) == 'D' && refsContent.charAt(i+4) != ':'){
                String HEADSHA1 = takeSHA1fromRefsContent(refsContent, i);
                //the HEAD commit sha1 ,40bytes
                refs.put("HEAD", HEADSHA1);
            }
            //refs
            if(refsContent.charAt(i) == 'r' && refsContent.charAt(i+1) == 'e' && refsContent.charAt(i+2) == 'f' && refsContent.charAt(i+3) == 's' && refsContent.charAt(i-1) != ':'){//take the master branch
               //heads
                if(refsContent.charAt(i+5) == 'h' && refsContent.charAt(i+6) == 'e' && refsContent.charAt(i+7) == 'a' && refsContent.charAt(i+8) == 'd' && refsContent.charAt(i+9) == 's'){
                   //master or another branch
                    //finish read the name from refs/heads/__name__ and take the sha1
                    String branchSHA1 = takeSHA1fromRefsContent(refsContent, i);
                    String nameBranch = takeNameFromRefs(refsContent, i);
                    refs.put("refs/heads/"+ nameBranch, branchSHA1);  //take the name of the branch ,and branch SHA1
                }
               //tags
                if(refsContent.charAt(i+5) == 't' && refsContent.charAt(i+6) == 'a' && refsContent.charAt(i+7) == 'g' && refsContent.charAt(i+8) == 's'){
                    //finish read the name from refs/tags/__name__ and take the sha1
                    String tagSHA1 = takeSHA1fromRefsContent(refsContent, i);
                    String nameTag = takeNameFromRefs(refsContent, i-1); //bcs refs/heads have 11 and refs/tags have 10
                    refs.put("refs/tags/"+ nameTag, tagSHA1);  //take the name of the tag ,and tag SHA1
                }
                //another folders...
            }
            //the HEAD commit sha1 is the same with the master branch sha1

        }
        return refs;

    }
    private static String takeSHA1fromRefsContent(String refsContent, int i){
        StringBuilder sha1 = new StringBuilder();
        for(int j = i - 2; j > i - 42; j--){
            sha1.append(refsContent.charAt(j));
        }
        return sha1.reverse().toString();
    }
    private static String takeNameFromRefs(String refsContent, int i) {
        StringBuilder name = new StringBuilder();
        for (int j = i + 11; j < refsContent.length(); j++) {
            if (refsContent.charAt(j) == ' ' || ( refsContent.charAt(j) == '0' && refsContent.charAt(j + 1) == '0' && refsContent.charAt(j + 2) == '0' && refsContent.charAt(j + 3) == '0')) {
                break;
            }
            name.append(refsContent.charAt(j));
        }
        return name.toString();
    }

    //Crate a new commit-tree object
    public static void createCommit(String[] args){
        /**
         * tree <sha1-of-tree>
         * parent <sha1-of-parent-commit> (opțional, poate fi mai multe linii pentru fiecare părinte)
         * author <name> <email> <timestamp> <timezone>
         * committer <name> <email> <timestamp> <timezone>
         *
         * <commit-message>
         **/

        try{
            int length = args.length;
            //take sha-tree
            String shaTree = args[1] , shaParentCommit = "", message="";
            StringBuilder commitContent = new StringBuilder();
            boolean hasParent=false,hasMessage=false;

            //verify if we have option -p
            if(args[2].equals("-p") && length>2){
                hasParent=true;
                shaParentCommit=args[3];
            }
            //verify -m option
            if(length>4 && args[4].equals("-m")){
                hasMessage=true;
                message=args[5];
            }

            // Build commit content
            commitContent.append("tree ").append(shaTree).append("\n");
            if (hasParent) {
                commitContent.append("parent ").append(shaParentCommit).append("\n");
            }
            commitContent.append("author test author <test@example.com> ").append(LocalDateTime.now().toString()).append(" +0000\n");
            commitContent.append("committer test author <test@example.com> ").append(LocalDateTime.now().toString()).append(" +0000\n");
            if (hasMessage) {
                commitContent.append("\n").append(message).append("\n");
            } else {
                commitContent.append("\n");
            }

            // Convert commit content to byte array
            byte[] contentBytes = commitContent.toString().getBytes(StandardCharsets.UTF_8);
            int contentLength = contentBytes.length;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write("commit ".getBytes());
            baos.write(Integer.toString(contentLength).getBytes());
            baos.write(0); // append null byte
            baos.write(contentBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error creating blob bytes", e);
        }

        byte[] blob_bytes = baos.toByteArray();

            //debug
            //System.out.println(commitObject);

            //create sha1Commit.
            byte[] sha1Commit = computeSHA1CompressAndStore(blob_bytes);

            //print sha1Commit
            printShaInHexaMode(sha1Commit);


        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Main method to display the content of a Git object
    public static void displayGitObject(String hashInput, String option) {

        String objectPath = getObjectPath(hashInput);
        File objectFile = new File(objectPath);

        try (InflaterInputStream decompressor = new InflaterInputStream(Files.newInputStream(objectFile.toPath()))) {
            byte[] data = decompressor.readAllBytes();
            String objectContent = new String(data, StandardCharsets.ISO_8859_1);

            // Extract type, size, and content from the object data
            ObjectData objectData = extractObjectData(objectContent);

            // Handle different types of Git objects
            if ("blob".equals(objectData.type)) {
                handleBlobObject(option, objectData);
            } else if ("tree".equals(objectData.type)) {
                handleTreeObject(option, objectData);
            } else if("commit".equals(objectData.type) || option.equals("commit")) {

                handleCommitObject(objectData);
            }else {
                throw new IllegalArgumentException("Unsupported object type: " + objectData.type);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading or processing Git object", e);
        }
    }

    // Helper method to get the path of the Git object
    private static String getObjectPath(String hash) {
        String directory = hash.substring(0, 2);
        String filename = hash.substring(2);
        return ".git/objects/" + directory + "/" + filename;
    }

    // Helper method to extract type, size, and content from object data
    private static ObjectData extractObjectData(String content) {
        int typeEndIndex = content.indexOf(' ');
        int sizeEndIndex = content.indexOf('\0');

        if (typeEndIndex == -1 || sizeEndIndex == -1 || sizeEndIndex <= typeEndIndex) {
            throw new IllegalArgumentException("Invalid object format");
        }

        String type = content.substring(0, typeEndIndex);
        String size = content.substring(typeEndIndex + 1, sizeEndIndex);
        String objectContent = content.substring(sizeEndIndex + 1);

        return new ObjectData(type, size, objectContent);
    }

    // Handles and prints blob objects
    private static void handleBlobObject(String option, ObjectData objectData) {
        switch (option) {
            case "-t":
                System.out.print(objectData.type);
                break;
            case "-s":
                System.out.print(objectData.size);
                break;
            case "-p":
                System.out.print(objectData.content);
                break;
            default:
                System.out.print(objectData.content);
        }
    }

    // Handles and prints tree objects
    private static void handleTreeObject(String option, ObjectData objectData) throws IOException {
        byte[] processedTree = processTreeContent(objectData.content, !option.equals("--name-only"));
        System.out.print(new String(processedTree, StandardCharsets.ISO_8859_1));
    }

    // Handles and prints commit objects
    private static void handleCommitObject(ObjectData data) {


        boolean isMessage = false;
        StringBuilder message = new StringBuilder();

        for (String line : data.content.split("\n")) {
            if (line.startsWith("tree") || line.startsWith("parent") || line.startsWith("author") || line.startsWith("committer")) {
                // Start collecting the message after all metadata
                isMessage = true;
                continue;
            }
            if (isMessage) {
                message.append(line).append("\n");
            }
        }
        System.out.println(message.toString().trim());
    }
    // Creates a Git object (blob) from a file
    public static byte[] createGitBlob(String[] args) {
        File file = new File(args[args.length - 1]);
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String content = new String(fileContent, StandardCharsets.UTF_8);
            String type = parseObjectType(args);
            String objectContent = type + " " + content.length() + "\0" + content;
            byte[] sha1Hash = computeSHA1Hash(objectContent);

            if (shouldWriteToGitObjects(args)) {
                String objectPath = getObjectPathForHash(sha1Hash);
                compressToZlib(objectPath, objectContent);
            }
            return sha1Hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating Git blob", e);
        }
    }

    // Parses the object type from command-line arguments
    private static String parseObjectType(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-t")) {
                String type = arg.substring(2).strip();
                if (type.equals("tag") || type.equals("blob") || type.equals("commit") || type.equals("tree")) {
                    return type;
                }
            }
        }
        return "blob";
    }

    // Determines whether to write the object to .git/objects
    private static boolean shouldWriteToGitObjects(String[] args) {
        for (String arg : args) {
            if (arg.equals("-w")) {
                return true;
            }
        }
        return false;
    }

    // Gets the object path for the SHA-1 hash
    private static String getObjectPathForHash(byte[] sha1Hash) throws IOException {
        String hashHex = bytesToHex(sha1Hash);
        String directoryName = hashHex.substring(0, 2);
        String filename = hashHex.substring(2);
        String path = ".git/objects/" + directoryName;
        File directory = new File(path);

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory);
        }
        return path + "/" + filename;
    }

    // Compresses data to a zlib file
    private static void compressToZlib(String path, String content) throws IOException {
        //System.out.println("Compressing to: " + path + " content " + content);
        try (FileOutputStream fileOutputStream = new FileOutputStream(path);
             DeflaterOutputStream compressor = new DeflaterOutputStream(fileOutputStream)) {
            compressor.write(content.getBytes(StandardCharsets.ISO_8859_1));
            compressor.finish();
        }
    }
    private static void compressToZlib(String path , byte[] content) {
        // BE aware - not closing the output streams properly would cause incorrect content
        // written to file (should close deflaterOutputStream first, then FileOutputStream)
        try (OutputStream outputStream = new FileOutputStream(path);
             DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(content);
            deflaterOutputStream.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error writing blob file", e);
        }
    }

    // Converts byte array to hexadecimal string
    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    // Prints the SHA-1 hash in hexadecimal format
    public static void printShaInHexaMode(byte[] sha) {
        System.out.println(bytesToHex(sha));
    }
    // Iterates through a directory and processes files and subdirectories
    public static byte[] processDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Directory is empty or cannot be read: " + dir);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processSubdirectory(outputStream, file);
                } else {
                    processFile(outputStream, file);
                }
            }
            return generateTreeStructure(outputStream.toByteArray());
        }
    }

    // Processes a subdirectory and adds its content to the output stream
    private static void processSubdirectory(ByteArrayOutputStream outputStream, File dir) throws Exception {
        byte[] treeSha = processDirectory(dir);
        outputStream.write("40000 ".getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(dir.getName().getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write('\0'); // Null terminator
        outputStream.write(treeSha);
    }

    // Processes a file and adds its content to the output stream
    private static void processFile(ByteArrayOutputStream outputStream, File file) throws Exception {
        String[] args = {"hash-object", "-w", file.toString()};
        byte[] blobSha = createGitBlob(args);
        outputStream.write("100644 ".getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write(file.getName().getBytes(StandardCharsets.ISO_8859_1));
        outputStream.write('\0'); // Null terminator
        outputStream.write(blobSha);
    }

    // Generates the tree structure and returns the SHA-1 hash of the tree
    private static byte[] generateTreeStructure(byte[] content) throws NoSuchAlgorithmException, IOException {
        byte[] sortedContent = processTreeContent(new String(content, StandardCharsets.ISO_8859_1), true);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write("tree ".getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write(String.valueOf(sortedContent.length).getBytes(StandardCharsets.UTF_8));
            outputStream.write('\0'); // Null terminator
            outputStream.write(sortedContent);

            return computeSHA1CompressAndStore(outputStream.toByteArray());
        }
    }

    // Computes SHA-1 hash and stores the data
    private static byte[] computeSHA1CompressAndStore(byte[] data) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1Digest.digest(data);


        String path = getObjectPathForHash(sha1Hash);

        compressToZlib(path, data);

        return sha1Hash;
    }

    // Computes the SHA-1 hash of the object content
    private static byte[] computeSHA1Hash(String content) throws NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        return sha1Digest.digest(content.getBytes(StandardCharsets.UTF_8));
    }

    // Processes and returns tree content
    public static byte[] processTreeContent(String content, boolean returnFullContent) throws IOException {
        List<String> names = new ArrayList<>();
        Map<String, byte[]> nameToShaMap = new LinkedHashMap<>();
        int pos = 0;

        while (pos < content.length()) {
            int modeEndIndex = content.indexOf(' ', pos);
            int nameEndIndex = content.indexOf('\0', modeEndIndex + 1);

            if (modeEndIndex == -1 || nameEndIndex == -1) break;

            String mode = content.substring(pos, modeEndIndex);
            String name = content.substring(modeEndIndex + 1, nameEndIndex);

            if (".git".equals(name)) {
                pos = nameEndIndex + 21;
                continue;
            }

            if (nameEndIndex + 21 > content.length()) {
                throw new IOException("Insufficient data to extract SHA for " + name);
            }

            byte[] shaBinary = content.substring(nameEndIndex + 1, nameEndIndex + 21).getBytes(StandardCharsets.ISO_8859_1);

            if (returnFullContent) {
                String entry = mode + ' ' + name + '\0';
                nameToShaMap.put(entry, shaBinary);
            }
            names.add(name);

            pos = nameEndIndex + 21;
        }

        names.sort(Collator.getInstance(Locale.ROOT));

        if (returnFullContent) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (String name : names) {
                for (Map.Entry<String, byte[]> entry : nameToShaMap.entrySet()) {
                    if (entry.getKey().contains(name)) {
                        outputStream.write(entry.getKey().getBytes(StandardCharsets.ISO_8859_1));
                        outputStream.write(entry.getValue());
                        break;
                    }
                }
            }
            return outputStream.toByteArray();
        } else {
            StringBuilder sortedNames = new StringBuilder();
            for (String name : names) {
                sortedNames.append(name).append('\n');
            }
            return sortedNames.toString().getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    // Helper class to hold object data
    private static class ObjectData {
        String type;
        String size;
        String content;

        ObjectData(String type, String size, String content) {
            this.type = type;
            this.size = size;
            this.content = content;
        }
    }
}