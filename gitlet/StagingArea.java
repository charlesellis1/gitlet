package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Staging area class.
 *
 * @author charlesellis
 *
 * */
public class StagingArea implements Serializable {

    /** Hashmap of fileName, blob, of files that are staged for addition. */
    HashMap<String, String> toBeAdded;

    /** Arraylist of fileNames of files that are staged for removal. */
    ArrayList<String> removedFiles;


    /**
     * Refers to current directory.
     */
    private static File cwd = Utils.join(System.getProperty("user.dir"));

    /**
     * Refers to gitfile.
     */
    private static File gitfile = Utils.join(cwd, ".gitlet");

    /**
     * Refers to repofile.
     */
    private static File stagingFile = Utils.join(gitfile, "/staging");

    /** Initializes a staging area. */
    StagingArea() {
        toBeAdded = new HashMap<>();
        removedFiles = new ArrayList<>();
    }

    /** Resets staging area.*/
    void reset() {
        toBeAdded = new HashMap<>();
        Repo r = Main.getMyRepo();
        List<String> toBeDeleted = Utils.plainFilenamesIn(stagingFile);
        for (String fileName : toBeDeleted) {
            File temp = new File(stagingFile + "/" + fileName);
            temp.delete();
        }
        removedFiles = new ArrayList<>();
    }
    /** Delete fileName from staging area and in stagingFile.
     * @param fileName fname
     * */
    void delete(String fileName) {
        Repo r = Main.getMyRepo();
        File temp = new File(stagingFile + "/" + fileName);
        temp.delete();
        toBeAdded.remove(fileName);
    }




}
