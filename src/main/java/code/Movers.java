package code;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Movers {
    public static void noSortMove(File source, File destination){

        try{
            Files.move(source.toPath(), Path.of(destination.getPath() + '\\' + source.getName()));
        }catch (IOException e){
            try{
                Files.move(source.toPath(),Path.of(destination.getPath() + '\\' + System.currentTimeMillis() + '\\' + source.getName()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
