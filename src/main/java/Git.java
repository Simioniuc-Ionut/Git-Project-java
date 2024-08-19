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



                type = result.substring(0,result.indexOf(" "));
                size = result.substring(result.indexOf(" ") + 1 ,result.indexOf("\0"));
                content = result.substring(result.indexOf("\0")+1);

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
                    StringBuilder eachLine = new StringBuilder();
                    eachLine.append(content, charactersReaded, content.indexOf(" "))
                            .append(content, charactersReaded + content.indexOf(" ") + 1, charactersReaded + content.indexOf("\0"))
                            .append(content, charactersReaded+ content.indexOf("\0") + 1, charactersReaded+ content.indexOf("\0") + 21);
                    //add each line
                    allResult.add(eachLine.toString());
                    nameResult.add(content.substring(charactersReaded + content.indexOf(" ") + 1, charactersReaded + content.indexOf("\0")));

                    charactersReaded+=eachLine.length();//without \n
                }

                String[] sortedNames = nameResult.stream().sorted().toArray(String[]::new);
                //print section

                if(option.equals("--name-only")){
                        System.out.print(sortedNames);
                }else{

                    //print all
                    for(String sortedName : Arrays.stream(sortedNames).toList()){
                        for(String unsortedLine : allResult){
                            if(unsortedLine.contains(sortedName)){
                                System.out.print(unsortedLine);
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
