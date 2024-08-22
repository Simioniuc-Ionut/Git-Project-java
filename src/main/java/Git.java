import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Git {

    public static void catFile(String hashInput,String option,String typeInput){
        String hash =hashInput;
        String directory = hash.substring(0,2);
        String filename = hash.substring(2);
        String path = ".git/objects/" + directory + "/"+ filename;
        File object = new File(path);
        String result ,type,size,content;

        //decompresam bytes din object in format citibil ,folosind inflater.

        try(InflaterInputStream decompressor = new InflaterInputStream(Files.newInputStream(object.toPath()))){


            //prepare the object section
            byte[] data = decompressor.readAllBytes();
            result = new String(data, StandardCharsets.UTF_8); // Sau specifică codificarea potrivită



            int typeEndIndex = result.indexOf(" ");
            int sizeEndIndex = result.indexOf("\0");

            if (typeEndIndex == -1 || sizeEndIndex == -1 || sizeEndIndex <= typeEndIndex) {
                throw new IllegalArgumentException("Invalid object format");
            }

            type = result.substring(0, typeEndIndex);
            size = result.substring(typeEndIndex + 1, sizeEndIndex);
            content = result.substring(sizeEndIndex + 1);

            if(type.equals("blob")){
                //an blob object is stored in the format <type>" "<size>\0<content>
                //print section
                if(!option.isEmpty()) {
                    if (option.equals("-t"))
                        System.out.print(type);
                    else if (option.equals("-s"))
                        System.out.print(size);
                    else if (option.equals("-p"))
                        System.out.print(content);
                }else {
                    System.out.print(content);
                }
            }else if(type.equals("tree")) {
                // tree <size>\0
                //  <mode> <name>\0<20_byte_sha>
                //  <mode> <name>\0<20_byte_sha>

                String treeContent= Arrays.toString(Git.processTree(content, !option.equals("--name-only")));
                System.out.println(treeContent);

            }
        }catch (IOException e){
            throw  new RuntimeException(e);
        }

    }
    public static byte[] hashObjectCreate(String[] args)  {
        //create a blob
        int argumentsLength = args.length;
        File fileReaded = new File(args[argumentsLength-1]);

        try {
            //declaration zone
            //System.out.println("Fileread  pah  is " + fileReaded.toPath());
            byte[] contentBinary = Files.readAllBytes(fileReaded.toPath()); // Read as bytes
            String content = new String(contentBinary,StandardCharsets.UTF_8);
            String resultObject,type="";
            StringBuilder path= new StringBuilder();// path where to write file
            MessageDigest instance = MessageDigest.getInstance("SHA-1"); //make an instance to get SHA-1 hash for file name and directory
            byte[] hash;
            int index=1;
            boolean writeToObjects= false;

            //options
            while(index<argumentsLength - 1){

                if(args[index].contains("-t")){ //if type is specified
                    String typeRead = args[index].substring(2);
                    if(typeRead.equals("tag") ||typeRead.equals("blob") || typeRead.equals("commit") || typeRead.equals("tree")){
                        type = typeRead.strip();
                    }
                }else if(args[index].contains("-w")){
                    //we write in .git/objects/dirname/filename
                    writeToObjects=true;
                }
                index++;
            }
            if(type.isEmpty()){
                type="blob";
            }

            //result obj
            resultObject=type + " " +content.length()+ "\0" + content;

            //compute SHA-1
            hash = instance.digest(resultObject.getBytes(StandardCharsets.UTF_8));
            String hashHexa = bytesToHex(hash);

            if(writeToObjects) {
                //add file and directory to .git/objects
                path.append(addDirAndFileToObjects(hashHexa));

            }
            //System.out.println("Path is : " + path);
            //compriming content of file using zlib
            comprimeToZlib(path.toString(),resultObject);

            //System.out.print(hashHexa);
            return hash;
        }catch (MalformedInputException e) {
           System.out.println("Failed to read file as UTF-8: " + fileReaded.getPath()+ " " + e + " in hashObjectCreate");
        }catch (IOException | NoSuchAlgorithmException e){
        e.printStackTrace();
        throw new RuntimeException(e);
    }
        return new byte[0];
    }


    public static byte[] itereateDirectory(File dir) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            System.out.println("error files is null " + dir);
            return new byte[0];
        }

        try (ByteArrayOutputStream contentLine = new ByteArrayOutputStream()) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(contentLine, file);
                } else {
                    processFile(contentLine, file);
                }
            }
            return calculateTreeStructure(contentLine.toByteArray());
        }
    }

    private static void processDirectory(ByteArrayOutputStream contentLine, File dir) throws Exception {
        byte[] shaTree = itereateDirectory(dir);
        contentLine.write("40000 ".getBytes(StandardCharsets.ISO_8859_1));
        contentLine.write(dir.getName().getBytes(StandardCharsets.ISO_8859_1));
        contentLine.write(0); // Null terminator
        contentLine.write(shaTree); // SHA binar
    }

    private static void processFile(ByteArrayOutputStream contentLine, File file) throws Exception {
        String[] args = new String[]{"hash-object", "-w", file.toString()};
        byte[] blobShaFileBinary = hashObjectCreate(args);
        contentLine.write("100644 ".getBytes(StandardCharsets.ISO_8859_1));
        contentLine.write(file.getName().getBytes(StandardCharsets.ISO_8859_1));
        contentLine.write("\0".getBytes(StandardCharsets.ISO_8859_1)); // Null terminator
        contentLine.write(blobShaFileBinary); // SHA binar
    }
    private static byte[] calculateTreeStructure(byte[] content) throws NoSuchAlgorithmException, IOException {
        byte[] sortedContent = Git.processTree(new String(content, StandardCharsets.ISO_8859_1), true);

        try (ByteArrayOutputStream fullTreeContent = new ByteArrayOutputStream()) {
            fullTreeContent.write("tree ".getBytes(StandardCharsets.ISO_8859_1));
            fullTreeContent.write(String.valueOf(sortedContent.length).getBytes(StandardCharsets.UTF_8));
            fullTreeContent.write("\0".getBytes(StandardCharsets.ISO_8859_1)); // Null terminator
            fullTreeContent.write(sortedContent);

            return computeSHA1AndStore(fullTreeContent.toByteArray());
        }
    }

    private static byte[] computeSHA1AndStore(byte[] fullTreeContent) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        byte[] treeSha1 = sha1Digest.digest(fullTreeContent);

        String hashHexa = bytesToHex(treeSha1);
        String path = addDirAndFileToObjects(hashHexa);

        comprimeToZlibInBytes(path, fullTreeContent);

        return treeSha1;
    }


    private static void comprimeToZlibInBytes(String path, byte[] fullTreeContent) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(path);
            DeflaterOutputStream compreserFile = new DeflaterOutputStream(fileOutputStream)) {
            compreserFile.write(fullTreeContent);
            compreserFile.finish();
        }
    }
    // Helper method to convert byte array to hexadecimal string

    private static String addDirAndFileToObjects(String hashHexa) throws IOException {
        StringBuilder path= new StringBuilder();// path where to write file

        path.append(".git/objects/");
        //find directory and filename

        String directoryName = hashHexa.substring(0,2);
        String filename = hashHexa.substring(2);
        //compute path to directory
        path.append(directoryName);

        //cream directorul
        File directory = new File(path.toString());

        if (!directory.exists() && !directory.mkdir()) {
            throw new IOException("Failed to create directory: " + directory);
        }
        //compute path to file
        path.append("/").append(filename);

        return path.toString();

    }
    private static void comprimeToZlib(String path,String resultObject) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(path);
            DeflaterOutputStream compreserFile = new DeflaterOutputStream(fileOutputStream)) {
            compreserFile.write(resultObject.getBytes(StandardCharsets.ISO_8859_1));
            compreserFile.finish();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    public static void printShaInHexaMode(byte[] sha){
        System.out.println(bytesToHex(sha));
    }


    public static byte[] processTree(String content, boolean returnFullContent) throws IOException {
        List<String> nameResult = new ArrayList<>();
        Map<String, byte[]> nameToSha = new LinkedHashMap<>();
        int pos = 0;

        while (pos < content.length()) {
            int modeEndIndex = content.indexOf(' ', pos);
            int nameEndIndex = content.indexOf('\0', modeEndIndex + 1);

            if (modeEndIndex == -1 || nameEndIndex == -1) break;

            String mode = content.substring(pos, modeEndIndex);
            String name = content.substring(modeEndIndex + 1, nameEndIndex);

            byte[] shaBinary = content.substring(nameEndIndex + 1, nameEndIndex + 21).getBytes(StandardCharsets.ISO_8859_1);

            if (returnFullContent) {
                StringBuilder line = new StringBuilder();
                line.append(mode).append(' ').append(name).append('\0');
                nameToSha.put(line.toString(), shaBinary);
            }
            nameResult.add(name);

            pos = nameEndIndex + 21; // Move past the SHA
        }

        // Comparator care sortează lexicografic (fără a ține cont de setările locale)
        Collections.sort(nameResult, Collator.getInstance(Locale.ROOT));

        ByteArrayOutputStream sortedResult = new ByteArrayOutputStream();
        for (String name : nameResult) {
            for (Map.Entry<String, byte[]> entry : nameToSha.entrySet()) {
                if (entry.getKey().contains(name)) {
                    sortedResult.write(entry.getKey().getBytes(StandardCharsets.ISO_8859_1));
                    sortedResult.write(entry.getValue());
                    break;
                }
            }
        }

        return sortedResult.toByteArray();
    }



}
