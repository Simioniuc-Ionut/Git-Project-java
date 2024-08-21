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
                if (option.equals("--name-only")) {
                    GitFunctions.processTreeNames(content);
                }else{
                    GitFunctions.processTreeContent(content);
                }
            }
        }catch (IOException e){
            throw  new RuntimeException(e);
        }

    }
    public static void hashObjectCreate(String[] args)  {
        //create a blob
        int argumentsLength = args.length;
        File fileReaded = new File(args[argumentsLength-1]);

        try {
            //declaration zone
            String content = Files.readString(fileReaded.toPath(), StandardCharsets.UTF_8);// Ensure UTF-8 encoding
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

            //compriming content of file using zlib
            comprimeToZlib(path.toString(),resultObject);

            System.out.print(hashHexa);
        }catch (MalformedInputException e) {
           System.out.println("Failed to read file as UTF-8: " + fileReaded.getPath()+ " " + e);
        }catch (IOException | NoSuchAlgorithmException e){
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    }

    public static String itereateDirectory(File dir) throws Exception{

        File[] files = dir.listFiles();

        StringBuilder contentLine =  new StringBuilder();
       if(files!=null) {
           for (File file : files) {
               if (file.isDirectory()) {
                   String shaTree = itereateDirectory(file);
                   //returnez un tree object
                   contentLine.append("040000 ")
                           .append(file.getName())
                           .append('\0')
                           .append(shaTree);
                 //  System.out.println(contentLine);
               } else {
                   //is file
                   //aplicam hash object si returnam sha ul
                   String[] args = new String[3];
                   args[0] = "hash-object";
                   args[1] = "-w";
                   args[2] = file.toString();
                   String blobShaFileInHexa = takeShaFromStdout(args);
                 
                   //returnez un blob obj
                   contentLine.append("100644 ")
                           .append(file.getName())
                           .append('\0')
                           .append(blobShaFileInHexa);

                  // System.out.println(contentLine);
               }
           }
           //am parcurs toate fisierele din direct. creez tree sha ul directorului si l returnez;
           //return sha-1 tree in hex
           return calculateTreeStructure(contentLine.toString());
       }else{
           System.out.println("error files is null " + dir);
           return " nothing ";
       }
    }

    private static String calculateTreeStructure(String content) throws NoSuchAlgorithmException, IOException {
    /**
     * tree <size>\0
     * 100644 file.txt\0<binary_sha1_abcd1234...>
     */

    String fullTreeContent = "tree " +
            content.length() +
            "\0" +
            content;

    // Calcularea SHA-1
    MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
    byte[] treeSha1 = sha1Digest.digest(fullTreeContent.getBytes(StandardCharsets.UTF_8));
    String hashHexa = bytesToHex(treeSha1);
    //add in .git/objects/
        String path = addDirAndFileToObjects(hashHexa);
        //compriming data
        comprimeToZlib(path,fullTreeContent);
    try{
        addDirAndFileToObjects(hashHexa);
    }catch (IOException e){
        e.printStackTrace();
    }
    return hashHexa;

    }
    // Helper method to convert byte array to hexadecimal string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    private static String takeShaFromStdout(String[] args){
        // Citim din stodutul  sha ul si l returnam ca string
        // Cream un obiect ByteArrayOutputStream pentru a capta ieșirea
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);


        // Salvăm referința originală la System.out
        PrintStream originalOut = System.out;
        String outputShaData;
        try{
            // Redirecționăm System.out către noul flux (printStream)
            System.setOut(printStream);

            hashObjectCreate(args);

            // Convertim conținutul captat într-un String și eliminăm newline-urile
            outputShaData = outputStream.toString().replace("\n", "").replace("\r", "");
        }
        finally {
            // Restaurăm System.out la starea inițială
            System.setOut(originalOut);
        }

        return outputShaData;
    }
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
}
