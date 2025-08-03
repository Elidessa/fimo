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
import java.nio.file.Path;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class FileMover implements Runnable {
    private final File sourcePath;
    private final File destinationPath;
    private final ArrayList<File> destinationDirectories;
    private final File duplicateDirectory;
    private final DateFormat df = new SimpleDateFormat("yyyy");
    private int fileCounter, duplicateFileCounter;


    public FileMover(String sourcePath, String destinationPath){
        this.sourcePath = new File(sourcePath);
        this.destinationPath = new File(destinationPath);
        destinationDirectories = new ArrayList<>();
        duplicateDirectory = new File(destinationPath + '\\' + "duplicates");

        destinationDirectories.addAll(Arrays.asList(Objects.requireNonNull(this.destinationPath.listFiles())));
        run();
    }
    public void fileMoveIterator(File source, File destination) throws IOException {
        if (source.isFile()){
            moveFullSort(source,destination);
        }else{
            File[] listOfFiles = source.listFiles();
            assert listOfFiles != null;
            for(File file : listOfFiles){
                fileMoveIterator(file, destination);
            }
        }
    }
    public void noSortMove(File source, File destination){

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


    public void move(File source, File destination) throws IOException {

        File datedDestinationDirectory = new File(destination.getPath() + "\\" + df.format(getTime(source)));

        if (subdirectoryDoNotExists(datedDestinationDirectory) && datedDestinationDirectory.mkdir()){
            destinationDirectories.add(datedDestinationDirectory);
        }


        Path finalFilePath = Path.of(datedDestinationDirectory.toString() + '\\' + source.getName());

        try{
            Files.move(source.toPath(), finalFilePath);
            fileCounter++;

        }catch (FileAlreadyExistsException e){
            if( Files.mismatch(source.toPath(), finalFilePath) == -1){

                if (subdirectoryDoNotExists(duplicateDirectory) && duplicateDirectory.mkdir()){
                    Files.move(source.toPath(),Path.of(duplicateDirectory.toString() + '\\' + System.currentTimeMillis()+ '_' + source.getName()));
                }

                duplicateFileCounter++;
            }else{
                Files.move(source.toPath(),new File(datedDestinationDirectory.toString() + '\\' + System.currentTimeMillis()+ '_' + source.getName() ).toPath());
                fileCounter++;
            }

        }

        System.out.println("Transferring File: " + (fileCounter+duplicateFileCounter)+ ", " + source.getName());

    }
    public boolean matchFileInDirectory(File file, File directory) throws IOException {
        File[] files = directory.listFiles();
        assert files != null;
        for (File fileInDirectory : files){
            if( Files.mismatch(file.toPath(), fileInDirectory.toPath()) == -1) return true;
        }
        return false;
    }


    public boolean subdirectoryDoNotExists(File dirName){

        for (File file : destinationDirectories){
            if(file.equals(dirName)) return false;
        }
        return true;
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

    public void moveFullSort(File source, File destination) throws IOException {

        File datedDestinationDirectory = new File(destination.getPath() + "\\" + df.format(getTime(source)));

        if (subdirectoryDoNotExists(datedDestinationDirectory) && datedDestinationDirectory.mkdir()){
            destinationDirectories.add(datedDestinationDirectory);
        }

        String pathname = datedDestinationDirectory.toString() + '\\' + source.getName();
        Path finalFilePath = Path.of(pathname);

        if(matchFileInDirectory(source,datedDestinationDirectory)){
            try{
                if (subdirectoryDoNotExists(duplicateDirectory) && duplicateDirectory.mkdir()){
                    Files.move(source.toPath(),Path.of(duplicateDirectory.toString() + '\\' + source.getName()) );
                    System.out.println("Identical file, unique name");
                }

            }catch (FileAlreadyExistsException e){
                Files.move(source.toPath(),Path.of(duplicateDirectory.toString() + '\\' + System.currentTimeMillis()+ '_' + source.getName()));
                System.out.println("Identical file, identical name");
            }
            duplicateFileCounter++;
        }else{
            try{
                Files.move(source.toPath(), finalFilePath);
                System.out.println("Unique file, unique name");

            }catch (FileAlreadyExistsException e){
                Files.move(source.toPath(), Path.of(datedDestinationDirectory.toString() + '\\' + System.currentTimeMillis()+ '_' + source.getName()));
                System.out.println("Unique file, identical name");
            }
            fileCounter++;
        }
    }
}
