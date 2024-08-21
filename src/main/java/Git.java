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

                String treeContent= Git.processTree(content, !option.equals("--name-only"));
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

    public static byte[] itereateDirectory(File dir) throws Exception{

        File[] files = dir.listFiles();

        StringBuilder contentLine =  new StringBuilder();
       if(files!=null) {
           for (File file : files) {
               if (file.isDirectory()) {
                   byte[] shaTree = itereateDirectory(file);
                   //byte[] shaTreeBinary = hexToBytes(shaTree);
                   contentLine.append("040000 ")
                           .append(file.getName())
                           .append("\0")
                           .append(Arrays.toString(shaTree));
                 //  System.out.println(contentLine);
               } else {
                   //is file
                   //aplicam hash object si returnam sha ul
                   String[] args = new String[3];
                   args[0] = "hash-object";
                   args[1] = "-w";
                   args[2] = file.toString();
                   byte[] blobShaFileBinary = hashObjectCreate(args);
                   //byte[] blobShaBinary = hexToBytes(blobShaFileInHexa);
                   //returnez un blob obj
                   contentLine.append("100644 ")
                           .append(file.getName())
                           .append("\0")
                           .append(Arrays.toString(blobShaFileBinary));

                  // System.out.println(contentLine);
               }
           }
           //am parcurs toate fisierele din direct. creez tree sha ul directorului si l returnez;
           //return sha-1 tree in hex
           return calculateTreeStructure(contentLine.toString());
       }else{
           System.out.println("error files is null " + dir);
           return new byte[0];
       }
    }

    private static byte[] calculateTreeStructure(String content) throws NoSuchAlgorithmException, IOException {
    /**
     * tree <size>\0
     * 100644 file.txt\0<binary_sha1_abcd1234...>
     */

    String fullTreeContent = "tree" + " " +
            content.length() +
            "\0" + Git.processTree(content,true);

    // Calcularea SHA-1
    MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
    byte[] treeSha1 = sha1Digest.digest(fullTreeContent.getBytes(StandardCharsets.UTF_8));
    String hashHexa = bytesToHex(treeSha1);
    try {
        //add in .git/objects/
        String path = addDirAndFileToObjects(hashHexa);
        //compriming data
        comprimeToZlib(path, fullTreeContent);
    }catch (Exception e){
        e.printStackTrace();
        System.out.println("in calculate Tree Structure");
    }
    return treeSha1;

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
            compreserFile.write(resultObject.getBytes(StandardCharsets.UTF_8));
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
    public static String processTree(String content, boolean returnFullContent) {
        int charactersRead = 0;
        List<String> allResult = new LinkedList<>();
        List<String> nameResult = new LinkedList<>();

        while (charactersRead < content.length()) {
            int newlineIndex = content.indexOf('\n', charactersRead);
            if (newlineIndex == -1) newlineIndex = content.length();

            String line = content.substring(charactersRead, newlineIndex);
            int firstSpaceIndex = line.indexOf(' ');
            if (firstSpaceIndex == -1) break;

            int secondSpaceIndex = line.indexOf(' ', firstSpaceIndex + 1);
            if (secondSpaceIndex == -1) break;

            int thirdSpaceIndex = line.indexOf(' ', secondSpaceIndex + 1);
            if (thirdSpaceIndex == -1) break;

            String mode = line.substring(0, firstSpaceIndex);
            String type = line.substring(firstSpaceIndex + 1, secondSpaceIndex);
            String sha = line.substring(secondSpaceIndex + 1, thirdSpaceIndex);
            String name = line.substring(thirdSpaceIndex + 1);

            if (returnFullContent) {
                StringBuilder eachLine = new StringBuilder();
                eachLine.append(mode).append(' ')
                        .append(type).append(' ')
                        .append(sha).append(' ')
                        .append(name);

                allResult.add(eachLine.toString());
            }

            nameResult.add(name);

            charactersRead = newlineIndex + 1;
        }

        String[] sortedNames = nameResult.stream().sorted().toArray(String[]::new);
        StringBuilder result = new StringBuilder();

        if (returnFullContent) {
            for (String sortedName : sortedNames) {
                for (String unsortedLine : allResult) {
                    if (unsortedLine.contains(sortedName)) {
                        result.append(unsortedLine);
                        break;
                    }
                }
            }
        } else {
            for (String sortedName : sortedNames) {
                result.append(sortedName);
            }
        }

        return result.toString();
    }


}
