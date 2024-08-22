import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

//    public static  byte[] itereateDirectory(File dir) throws Exception{
//
//        File[] files = dir.listFiles();
//
//        //StringBuilder contentLine =  new StringBuilder();
//        ByteArrayOutputStream contentLine2 = new ByteArrayOutputStream();
//
//       if(files!=null) {
//           for (File file : files) {
//               if (file.isDirectory()) {
//                   byte[] shaTree = itereateDirectory(file);
//
//                   //String shaTreeHex = bytesToHex(shaTree); // Convertim sha-ul în hexazecimal
////                   contentLine.append("040000 ")
////                           .append(file.getName())
////                           .append('\0')
////                           .append(shaTree); // Append binary
//                     contentLine2.write("040000 ".getBytes(StandardCharsets.UTF_8));
//                        contentLine2.write(file.getName().getBytes(StandardCharsets.UTF_8));
//                        contentLine2.write(0); // null terminator
//                        contentLine2.write(shaTree); // SHA binar
//                   //  System.out.println(contentLine);
//               } else {
//                   //is file
//                   //aplicam hash object si returnam sha ul
//                   String[] args = new String[3];
//                   args[0] = "hash-object";
//                   args[1] = "-w";
//                   args[2] = file.toString();
//                   byte[] blobShaFileBinary = hashObjectCreate(args);
//                   //String blobShaFileHex = bytesToHex(blobShaFileBinary); // Convertim sha-ul în hexazecimal
//                   //returnez un blob obj
//
////                   contentLine.append("100644 ")
////                           .append(file.getName())
////                           .append('\0')
////                           .append(blobShaFileBinary); // Append binary
//                   contentLine2.write("100644 ".getBytes(StandardCharsets.UTF_8));
//                     contentLine2.write(file.getName().getBytes(StandardCharsets.UTF_8));
//                        contentLine2.write(0); // null terminator
//                        contentLine2.write(blobShaFileBinary); // SHA binar
//
//                  // System.out.println(contentLine);
//               }
//           }
//           //am parcurs toate fisierele din direct. creez tree sha ul directorului si l returnez;
//           //return sha-1 tree in hex
//           return calculateTreeStructure(contentLine2.toString());
//       }else{
//           System.out.println("error files is null " + dir);
//           return new byte[0];
//       }
//        File[] files = dir.listFiles();
//        ByteArrayOutputStream contentLine = new ByteArrayOutputStream();
//
  //  }

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
        contentLine.write("040000 ".getBytes(StandardCharsets.UTF_8));
        contentLine.write(dir.getName().getBytes(StandardCharsets.UTF_8));
        contentLine.write(0); // Null terminator
        contentLine.write(shaTree); // SHA binar
    }

    private static void processFile(ByteArrayOutputStream contentLine, File file) throws Exception {
        String[] args = new String[]{"hash-object", "-w", file.toString()};
        byte[] blobShaFileBinary = hashObjectCreate(args);
        contentLine.write("100644 ".getBytes(StandardCharsets.UTF_8));
        contentLine.write(file.getName().getBytes(StandardCharsets.UTF_8));
        contentLine.write(0); // Null terminator
        contentLine.write(blobShaFileBinary); // SHA binar
    }
    private static byte[] calculateTreeStructure(byte[] content) throws NoSuchAlgorithmException, IOException {
        byte[] sortedContent = Git.processTree(new String(content, StandardCharsets.ISO_8859_1), true);

        try (ByteArrayOutputStream fullTreeContent = new ByteArrayOutputStream()) {
            fullTreeContent.write("tree".getBytes(StandardCharsets.UTF_8));
            fullTreeContent.write(' ');
            fullTreeContent.write(String.valueOf(sortedContent.length).getBytes(StandardCharsets.UTF_8));
            fullTreeContent.write(0); // Null terminator
            fullTreeContent.write(sortedContent);

            // Debugging: Afișează conținutul complet înainte de calculul SHA-1
            System.out.println("Full Tree Content (hex): " + bytesToHex(fullTreeContent.toByteArray()));

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

//    private static byte[] calculateTreeStructure(String content) throws NoSuchAlgorithmException, IOException {
//    /**
//     * tree <size>\0
//     * 100644 file.txt\0<binary_sha1_abcd1234...>
//     */
//    byte[] sortedContent = Git.processTree(content,true);
//    // System.out.println("unsorted content " + content + "\n" + "sorted content " + sortedContent);
//    // System.out.println("sorted content " + sortedContent);
////    String fullTreeContent = "tree" + ' ' +
////            sortedContent.length() +
////            '\0' + sortedContent;
//        ByteArrayOutputStream fullTreeContent = new ByteArrayOutputStream();
//        fullTreeContent.write("tree".getBytes(StandardCharsets.UTF_8));
//        fullTreeContent.write(' ');
//        fullTreeContent.write(String.valueOf(sortedContent.length).getBytes(StandardCharsets.UTF_8));
//        fullTreeContent.write(0);
//        fullTreeContent.write(sortedContent);
//    // Calcularea SHA-1
//    MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
//    byte[] treeSha1 = sha1Digest.digest(fullTreeContent.toByteArray());
//    String hashHexa = bytesToHex(treeSha1);
//    try {
//        //add in .git/objects/
//        String path = addDirAndFileToObjects(hashHexa);
//        //compriming data
//       // comprimeToZlib(path, fullTreeContent);
//        comprimeToZlibInBytes(path, fullTreeContent.toByteArray());
//    }catch (Exception e){
//        e.printStackTrace();
//        System.out.println("in calculate Tree Structure");
//    }
//    return treeSha1;
//
//    }

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
        List<String> allResult = new ArrayList<>();
        List<String> nameResult = new ArrayList<>();

        Map<String, byte[]> nameToSha = parseContent(content, returnFullContent);

        Collections.sort(nameResult);

        //StringBuilder sortedResult = new StringBuilder();
        ByteArrayOutputStream sortedResult = new ByteArrayOutputStream();
        for (String name : nameResult) {
            for (Map.Entry<String,byte[]> entry : nameToSha.entrySet()) {
                if (entry.getKey().contains(name)) {
                    sortedResult.write(entry.getKey().getBytes(StandardCharsets.ISO_8859_1));
                    sortedResult.write(entry.getValue());

                    break;
                }
            }
        }
//        if (returnFullContent) {
//            for (String name : nameResult) {
//                for (String line : allResult) {
//                    if (line.contains(name)) {
//                        sortedResult.append(line);
//                        break;
//                    }
//                }
//            }
//        } else {
//            for (String name : nameResult) {
//                sortedResult.append(name);
//            }
//        }

        return sortedResult.toByteArray();
    }
    private static Map<String, byte[]> parseContent(String content, boolean returnFullContent) {
        Map<String, byte[]> nameToSha = new LinkedHashMap<>();
        int pos = 0;

        while (pos < content.length()) {
            int modeEndIndex = content.indexOf(' ', pos);
            int nameEndIndex = content.indexOf('\0', modeEndIndex + 1);

            if (modeEndIndex == -1 || nameEndIndex == -1) break;

            String mode = content.substring(pos, modeEndIndex);
            String name = content.substring(modeEndIndex + 1, nameEndIndex);
            byte[] shaBinary = content.substring(nameEndIndex + 1, nameEndIndex + 21).getBytes(StandardCharsets.ISO_8859_1);

            String key = returnFullContent ? mode + ' ' + name + '\0' : name;
            nameToSha.put(key, shaBinary);

            pos = nameEndIndex + 21; // Move past the SHA
        }

        return nameToSha;
    }


    private static int indexOf(byte[] buffer, char delimiter, int start, int end) {
            for (int i = start; i < end; i++) {
                if (buffer[i] == delimiter) {
                    return i;
                }
            }
            return -1;
        }


//    public static String processTree(String content, boolean returnFullContent) {
//        int charactersRead = 0;
//        List<String> allResult = new LinkedList<>();
//        List<String> nameResult = new LinkedList<>();
//
//        while (charactersRead < content.length()) {
//            int modeEndIndex = content.indexOf(" ", charactersRead);
//            if (modeEndIndex == -1) break;
//
//            int nameEndIndex = content.indexOf("\0", modeEndIndex + 1);
//            if (nameEndIndex == -1) break;
//
//            // Extract mode, name, and binary SHA
//            String mode = content.substring(charactersRead, modeEndIndex);
//            String name = content.substring(modeEndIndex + 1, nameEndIndex);
//
//            // Extract the 20-byte binary SHA
//            byte[] shaBinary = new byte[20];
//            System.arraycopy(content.getBytes(StandardCharsets.ISO_8859_1), nameEndIndex + 1, shaBinary, 0, 20);
//
//            if (returnFullContent) {
//                StringBuilder eachLine = new StringBuilder();
//                eachLine.append(mode).append(" ")
//                        .append(name).append("\0")
//                        .append(bytesToHex(shaBinary)); // Append binary SHA
//
//                allResult.add(eachLine.toString());
//            }
//
//            nameResult.add(name);
//            charactersRead = nameEndIndex + 21; // Move past \0 and 20-byte SHA
//        }
//
//        String[] sortedNames = nameResult.stream().sorted().toArray(String[]::new);
//        StringBuilder result = new StringBuilder();
//        //print all content
//        System.out.println("All content");
//        for(String s : allResult){
//            System.out.println(s);
//        }
//        if (returnFullContent) {
//            for (String sortedName : sortedNames) {
//                for (String unsortedLine : allResult) {
//                    if (unsortedLine.contains(sortedName)) {
//                        result.append(unsortedLine);
//                        break;
//                    }
//                }
//            }
//        } else {
//            for (String sortedName : sortedNames) {
//                result.append(sortedName);
//            }
//        }
//
//        return result.toString();
//    }

}
