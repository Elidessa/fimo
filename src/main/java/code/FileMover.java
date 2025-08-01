package code;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class FileMover implements Runnable {
    private final File sourcePath;
    private final File destinationPath;
    private final ArrayList<File> destinationDirectories;
    private final DateFormat df = new SimpleDateFormat("yyyy");
    private int fileCounter, duplicateFileCounter;

    public FileMover(String sourcePath, String destinationPath){
        this.sourcePath = new File(sourcePath);
        this.destinationPath = new File(destinationPath);
        destinationDirectories = new ArrayList<>();

        destinationDirectories.addAll(Arrays.asList(Objects.requireNonNull(this.destinationPath.listFiles())));
        run();
    }
    public void fileMoveIterator(File source, File destination) throws IOException {
        if (source.isFile()){
            move(source,destination);
        }else{
            File[] listOfFiles = source.listFiles();
            assert listOfFiles != null;
            for(File file : listOfFiles){
                fileMoveIterator(file, destination);
            }
        }
    }
    public void move(File source, File destination) throws IOException {

        File datedDestinationPath = new File(destination.getPath() + "\\" + df.format(getTime(source)));

        if (!subdirectoryExists(datedDestinationPath) && datedDestinationPath.mkdir()){
            destinationDirectories.add(datedDestinationPath);
        }

        String pathname = datedDestinationPath.toString() + '\\' + source.getName();
        try{
            Files.move(source.toPath(),new File(pathname).toPath());
            fileCounter++;

        }catch (FileAlreadyExistsException e){

            Files.move(source.toPath(),new File(datedDestinationPath.toString() + '\\' + System.currentTimeMillis()+ '_' + source.getName() ).toPath());
            duplicateFileCounter++;
        }

        System.out.println("Transferring File: " + (fileCounter+duplicateFileCounter)+ ", " + source.getName());
    }
    public boolean subdirectoryExists(File dirName){
        for (File file : destinationDirectories){
            if(file.equals(dirName)) return true;
        }
        return false;
    }
    public long getTime(File file){
        try{
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            // obtain the Exif SubIFD directory
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            return directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime();
        }catch (ImageProcessingException | IOException | NullPointerException e){
            System.out.println("Can not find creation date, reverting to last modified");
            return file.lastModified();
        }
    }

    @Override
    public void run() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            System.out.println("Image Transfer Started");
            fileMoveIterator(sourcePath,destinationPath);
            System.out.println("Image Transfer Complete");
            System.out.println(fileCounter + " Unique Files Transferred | " + duplicateFileCounter + " Duplicate Files | " + (fileCounter+duplicateFileCounter) + " Total Files Transferred " );
        } catch (IOException  e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        watch.stop();
        System.out.println("Time: " + watch);
    }
}
