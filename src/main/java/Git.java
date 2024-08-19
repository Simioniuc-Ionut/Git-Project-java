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
            }else if(type.equals("tree")){
                // tree <size>\0
                //  <mode> <name>\0<20_byte_sha>
                //  <mode> <name>\0<20_byte_sha>
                int charactersReaded=0;
                List<String> allResult = new LinkedList<>();
                List<String> nameResult = new LinkedList<>();

                while(charactersReaded<content.length()) {
                    // Search for firest appearance \0
                    int nullCharIndex = content.indexOf('\0', charactersReaded);
                    if (nullCharIndex == -1) break; // If we dont found \0 ,we break,
                    int hashSize= 0;
                    // Split line
                    String modeNamePart = content.substring(charactersReaded, nullCharIndex);
                    int spaceIndex = modeNamePart.indexOf(' ');
                    if (spaceIndex == -1) break; // If not exist,break

                    String name = modeNamePart.substring(spaceIndex + 1); //gain name
                    String sha = content.substring(nullCharIndex + 1, nullCharIndex + 41); // SHA

                    StringBuilder eachLine = new StringBuilder();
                    eachLine.append(modeNamePart).append('\0').append(sha);

                    // Add each row
                    allResult.add(eachLine.toString());
                    nameResult.add(name);
                    // Update index for next iteration
                    charactersReaded = nullCharIndex + 41 ;
                }

                String[] sortedNames = nameResult.stream().sorted().toArray(String[]::new);
                //print section

                if(option.equals("--name-only")){

                    //print name only
                    for (String sortedName : sortedNames) {
                        System.out.println(sortedName);
                    }
                }else{

                    //print all info
                    for(String sortedName : Arrays.stream(sortedNames).toList()){
                        for(String unsortedLine : allResult){
                            if(unsortedLine.contains(sortedName)){
                                System.out.println(unsortedLine);
                                break;
                            }
                        }
                    }
                }
            }

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

}
