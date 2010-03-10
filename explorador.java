import java.io.*;

public class explorador implements FilenameFilter {
    String extension;

    explorador(String extension){
        this.extension = extension;
    }
    public boolean accept (File dir, String name){
        return name.endsWith(extension);
    }
}