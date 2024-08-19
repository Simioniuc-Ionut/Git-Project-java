import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
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
            result = new String(data);

            System.out.println(result);
            
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
                    // Print name only
                    int charactersReaded = 0;
                    List<String> nameResult = new LinkedList<>();
                    while (charactersReaded < content.length()) {
                        int spaceIndex = content.indexOf(' ', charactersReaded);
                        if (spaceIndex == -1) break;

                        int nullCharIndex = content.indexOf('\0', spaceIndex);
                        if (nullCharIndex == -1) break;

                        String name = content.substring(spaceIndex + 1, nullCharIndex);
                        nameResult.add(name);

                        charactersReaded = nullCharIndex + 21; // Move past the name and 20-byte SHA-1
                    }
                    String[] sortedNames = nameResult.stream().sorted().toArray(String[]::new);

                    for (String sortedName : sortedNames) {
                        System.out.println(sortedName);
                    }
                }else {
                    GitFunctions.processTreeContent(content);
                }
            }
        }catch (IOException e){
            throw  new RuntimeException(e);
        }

    }



}
