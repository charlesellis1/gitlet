package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Repository class.
 *
 * @author charlesellis
 *
 * */
public class Repo implements Serializable {

    /** String HEAD branch. */
    private String _head;
     /** String current directory. */
    private String currentDir;
    /** File current working directory. */
    private File cwd;
    /** File commit file. */
    private File commitFile;
    /** File staging file. */
    private File stagingFile;
    /** File blob file. */
    private File blobFile;
    /** File repo file. */
    private File repoFile;
    /** File .gitlet file. */
    private File gitFile;
    /** Staging Area object to this repo. */
    private StagingArea stagingArea;
    /** Untracked files in this repo. */
    private HashMap<String, String> _untrackedFiles;


    /**
     * Initialize Repo, Init Command.
     */
    public void init() {
        currentDir = System.getProperty("user.dir");
        cwd = Utils.join(currentDir);
        gitFile = Utils.join(cwd, ".gitlet");
        repoFile = Utils.join(gitFile, "/repo");
        if (!gitFile.exists()) {
            gitFile.mkdir();
        } else {
            throw new GitletException("A Gitlet "
                    + "version-control system already "
                    + "exists in the current directory.");
        }
        commitFile = Utils.join(gitFile, "commits");
        commitFile.mkdir();
        Commit initCommit = new Commit("initial commit",
                null, null, false);
        String initialSha1 = initCommit.sha();
        File sha1File = Utils.join(commitFile, initialSha1);
        try {
            sha1File.createNewFile();
        } catch (java.io.IOException e) {
            System.out.println("cannot create new file");
        }
        Utils.writeObject(sha1File, initCommit);
        _head = "master";
        _branches = new HashMap<>();
        _branches.put("master", initialSha1);
        stagingFile = Utils.join(gitFile, "staging");
        stagingFile.mkdir();
        blobFile = Utils.join(gitFile, "blobs");
        blobFile.mkdir();
        stagingArea = new StagingArea();
        _untrackedFiles = new HashMap<>();
    }

    /**
     * -----THE ADD COMMAND-----
     * Adds file.
     * @param name fname
     */
    public void addCommand(String name) {
        File thisFile = Utils.join(cwd, name);
        if (!thisFile.exists()) {
            throw new GitletException("File does not exist.");
        }
        String blob = "B" + Utils.sha1(
                Utils.readContentsAsString(thisFile));
        Commit mostRecent = getCommit(getHeadCommitSha());
        HashMap mrFiles = mostRecent.getFileMap();
        blobFile.mkdir();
        stagingFile.mkdir();
        if (mrFiles == null || mrFiles.isEmpty()
                || !mrFiles.containsKey(name)
                || !mrFiles.get(name).equals(blob)) {
            stagingArea.toBeAdded.put(name, blob);
            String contents = Utils.readContentsAsString(thisFile);
            File contentFile = Utils.join(stagingFile, name);
            Utils.writeContents(contentFile, contents);
        } else if (mrFiles.containsKey(name)
                && stagingArea.toBeAdded.containsKey(name)) {
            if (mrFiles.get(name).equals(blob)) {
                stagingArea.delete(name);
            }
        }
        _untrackedFiles.remove(name);
        stagingArea.removedFiles.remove(name);
    }


    /**
     * -------------------------------------REMOVE--rm--------------
     * Unstage the file if it is currently staged. If the file is
     * tracked in the current commit,
     * mark it to indicate that it is not to be included in the next
     * commit (presumably you would store this mark somewhere in the
     * .gitlet directory), and remove the file from the working
     * directory if the user has not already done so (do not remove it
     * unless it is tracked in thecurrent commit).
     *
     * @param fileName fname
     */
    public void removeCommand(String fileName) {
        boolean changed = false;
        File thisFile = Utils.join(cwd, fileName);
        Commit mostRecent = getCommit(getHeadCommitSha());
        HashMap<String, String> mrFiles = mostRecent.getFileMap();
        if (!thisFile.exists() && !mrFiles.containsKey(fileName)) {
            throw new GitletException("File does not exist.");
        }
        if (mrFiles != null) {
            if (mrFiles.containsKey(fileName)) {
                thisFile.delete();
                changed = true;
            }
        }
        List<String> stagingFiles = Utils.plainFilenamesIn(stagingFile);
        if (stagingFiles != null) {
            for (String file : stagingFiles) {
                if (fileName.equals(file)) {
                    File deleteMe = Utils.join(stagingFile, file);
                    deleteMe.delete();
                    stagingArea.toBeAdded.remove(fileName);
                    changed = true;
                }
            }
        }
        if (!changed) {
            throw new GitletException("No reason to remove the file.");
        } else {
            if (mostRecent.getFileMap().containsKey(fileName)) {
                stagingArea.removedFiles.add(fileName);
            }
        }
    }


    /**
     * -----THE COMMIT COMMAND-----
     * Takes a snapshot of files.
     * @param msg msg
     */
    public void commitCommand(String msg) {
        if (msg.trim().equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        Commit mostRecent = getCommit(getHeadCommitSha());
        HashMap<String, String> trackedFiles = mostRecent.getFileMap();
        if (trackedFiles == null) {
            trackedFiles = new HashMap<>();
        }
        if (stagingArea.toBeAdded.size() > 0 || _untrackedFiles.size() > 0) {
            for (String fileName : stagingArea.toBeAdded.keySet()) {
                trackedFiles.put(fileName,
                        stagingArea.toBeAdded.get(fileName));
                File thisFile = Utils.join(stagingFile, fileName);
                String contents = Utils.readContentsAsString(thisFile);
                String blob = stagingArea.toBeAdded.get(fileName);
                File newBlob = Utils.join(blobFile, blob);
                Utils.writeObject(newBlob, contents);
            }
            for (String fileName : _untrackedFiles.keySet()) {
                _untrackedFiles.remove(fileName);
            }
        }
        if (stagingArea.removedFiles.size() > 0) {
            for (String fileName : stagingArea.removedFiles) {
                trackedFiles.remove(fileName);
                Utils.restrictedDelete(Utils.join(cwd, fileName));
            }
        }
        if (stagingArea.toBeAdded.size() == 0
                && stagingArea.removedFiles.size() == 0) {
            throw new GitletException("No changes added to the commit.");
        }
        Commit newCommit = new Commit(msg, mostRecent, trackedFiles,
                false);
        String newCommID = newCommit.sha();
        File newCommFile = Utils.join(commitFile, newCommID);
        Utils.writeObject(newCommFile, newCommit);
        stagingArea.reset();
        _untrackedFiles = new HashMap<>();
        _branches.put(_head, newCommID);
    }


    /**
     * Same thing as commit but for merge.
     * @param msg m
     * @param p1 p1
     * @param p2 p2
     */
    public void mergeCommit(String msg, String p1, String p2) {
        if (msg.trim().equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        Commit mostRecent = getCommit(p2);
        HashMap<String, String> trackedFiles = mostRecent.getFileMap();
        if (trackedFiles == null) {
            trackedFiles = new HashMap<>();
        }
        if (stagingArea.toBeAdded.size() > 0 || _untrackedFiles.size() > 0) {
            for (String fileName : stagingArea.toBeAdded.keySet()) {
                trackedFiles.put(fileName,
                        stagingArea.toBeAdded.get(fileName));
                File thisFile = Utils.join(stagingFile, fileName);
                String contents = Utils.readContentsAsString(thisFile);
                String blob = stagingArea.toBeAdded.get(fileName);
                File newBlob = Utils.join(blobFile, blob);
                Utils.writeObject(newBlob, contents);
            }
            for (String fileName : _untrackedFiles.keySet()) {
                _untrackedFiles.remove(fileName);
            }
        }
        if (stagingArea.removedFiles.size() > 0) {
            for (String fileName : stagingArea.removedFiles) {
                trackedFiles.remove(fileName);
                Utils.restrictedDelete(Utils.join(cwd, fileName));
            }
        }
        if (stagingArea.toBeAdded.size() == 0
                && stagingArea.removedFiles.size() == 0) {
            throw new GitletException("No changes added to the commit.");
        }
        Commit newCommit = new MergeCommit(msg, mostRecent, trackedFiles,
                true, new String[]{p1, p2});
        String newCommID = newCommit.sha();
        File newCommFile = Utils.join(commitFile, newCommID);
        Utils.writeObject(newCommFile, newCommit);

        stagingArea.reset();
        _untrackedFiles = new HashMap<>();
        _branches.put(_head, newCommID);
    }

    /**
     * Log and global log.
     */
    public void log() {
        String head = getHeadCommitSha();
        Commit first = getCommit(head);
        if (first._merge) {
            first = getMergeCommit(head);
        }
        while (first != null) {
            printACommit(head);
            if (first.parent() != null) {
                head = first.parent().sha();
            }
            first = first.parent();
        }
    }

    /**
     * Prints log of ALL commits.
     */
    public void globalLog() {
        List<String> files = Utils.plainFilenamesIn(commitFile);
        for (String commitID : files) {
            printACommit(commitID);
        }
    }

    /**
     * Print commit given sha1 id of commit.
     * @param uid uid
     */
    public void printACommit(String uid) {
        Commit comm = getCommit(uid);
        if (comm._merge) {
            printMergeCommit(uid);
        } else {
            if (comm.parent() != null
                    && comm.parent().sha().length() > 1) {
                System.out.println("===");
                System.out.println("commit " + uid);
                System.out.println("Date: " + comm.timestamp());
                System.out.println(comm.getMessage());
                System.out.println();
            } else {
                System.out.println("===");
                System.out.println("commit " + uid);
                System.out.println("Date: " + comm.timestamp());
                System.out.println(comm.getMessage());
                System.out.println();
            }
        }
    }

    /**
     * Print a commit, but in merge format.
     * @param uid uid
     */
    public void printMergeCommit(String uid) {
        MergeCommit comm = getMergeCommit(uid);
        assert comm._merge;
        if (comm.parent() != null && comm.parent().sha().length() > 1) {
            System.out.println("===");
            System.out.println("commit " + comm.sha());
            String[] split = comm.getMessage().split(" ");
            String shortb1 = split[1];
            String shortb2 = split[3].replace(".", "");
            String shortP1 = _branches.get(shortb1).substring(0, 7);;
            String shortP2 = _branches.get(shortb2).substring(0, 7);;
            System.out.println("Merge: " + shortP1 + " " + shortP2);
            System.out.println("Date: " + comm.timestamp());
            System.out.println(comm.getMessage());
            System.out.println();
        } else {
            System.out.println("===");
            System.out.println("commit " + uid);
            System.out.println("Date: " + comm.timestamp());
            System.out.println(comm.getMessage());
            System.out.println();
        }
    }

    /**
     * ----------------------------------FIND---------------------
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the
     * ids out on separate lines. The commit message is a single operand;
     * to indicate a multiword message, put the operand
     * in quotation marks, as for the commit command below.
     *
     * @param msg msg
     */
    public void findCommand(String msg) {
        boolean found = false;
        List<String> shasOfCommits = Utils.plainFilenamesIn(commitFile);
        for (String sha : shasOfCommits) {
            Commit thisCommit = getCommit(sha);
            if (thisCommit.getMessage().equals(msg)) {
                System.out.println(sha);
                found = true;
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /**
     * -------------STATUS------------
     * Displays what branches currently exist, and marks the current
     * branch with a *. Also displays what files have been
     * staged or marked for untracking.
     */
    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : _branches.keySet()) {
            if (branch.equals(_head)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> stagingFiles = Utils.plainFilenamesIn(stagingFile);
        if (stagingFiles != null) {
            for (String fileName : stagingFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> toDelete = stagingArea.removedFiles;
        if (toDelete != null) {
            for (String fileName : toDelete) {
                if (!Utils.join(cwd, fileName).exists()) {
                    System.out.println(fileName);
                }
            }
        }
        System.out.println();
        updateUntrackedFiles();
        printUntrackedFiles();
    }

    /**
     * Checkout command.
     * @param args args
     */
    public void checkout(String[] args) {
        String commID;
        String fileName;
        if (args.length == 2 && args[0].equals("--")) {
            fileName = args[1];
            commID = getHeadCommitSha();
        } else if (args.length == 3 && args[1].equals("--")) {
            commID = args[0];
            fileName = args[2];
        } else {
            throw new GitletException("Incorrect operands");
        }
        commID = convertShortenedID(commID);
        Commit comm = getCommit(commID);
        HashMap<String, String> trackedFiles = comm.getFileMap();

        if (trackedFiles.containsKey(fileName)) {
            File f = Utils.join(cwd, fileName);
            String blobFileName = trackedFiles.get(fileName);
            File g = Utils.join(blobFile, blobFileName);
            String blah = Utils.readObject(g, String.class);
            String blobContents = Utils.readContentsAsString(g);
            Utils.writeContents(f, blah);
        } else {
            throw new GitletException("File does not exist in that commit.");
        }

        _branches.put(_head, commID);
    }

    /**
     * Takes in a short commit ID, and returns the longer version, which
     * we can read to get commit.
     * @param id id
     */
    public String convertShortenedID(String id) {
        if (id.length() == Utils.UID_LENGTH) {
            return id;
        }
        File[] commitFiles = commitFile.listFiles();
        assert commitFiles != null;
        for (File file : commitFiles) {
            if (file.getName().contains(id)) {
                return file.getName();
            }
        }
        throw new GitletException("No commit with that id exists.");
    }

    /**
     * Checkout, given a branchName
     * If a working file is untracked in the current branch and would be
     * overwritten by the checkout.
     * @param branchName bname
     */
    public void checkout(String branchName) {
        if (!_branches.containsKey(branchName)) {
            throw new GitletException("No such branch exists.");
        }
        if (_head.equals(branchName)) {
            throw new GitletException(
                    "No need to checkout the current branch.");
        }
        String newHeadCommID = _branches.get(branchName);
        Commit newHeadComm = getCommit(newHeadCommID);
        Commit oldHead = getCommit(getHeadCommitSha());
        HashMap<String, String> newHeadCommFileMap = newHeadComm.getFileMap();
        checkForUntracked(newHeadCommID);
        for (String fileName : Utils.plainFilenamesIn(cwd)) {
            if (newHeadCommFileMap == null
                    || (!newHeadCommFileMap.containsKey(fileName)
                    && !fileName.equals(".gitlet"))) {
                stagingArea.toBeAdded.remove(fileName);
            } else {
                boolean b = !newHeadCommFileMap.containsKey(fileName);
                if (b && !fileName.equals(".gitlet")) {
                    Utils.restrictedDelete(Utils.join(cwd, fileName));
                }
            }
            if (oldHead.getFileMap().containsKey(fileName)
                    && !newHeadCommFileMap.containsKey(fileName)) {
                Utils.restrictedDelete(Utils.join(cwd, fileName));
            }
        }
        if (newHeadCommFileMap != null
                && !_branches.get(_head)
                .equals(_branches.get(branchName))) {
            for (String file : newHeadCommFileMap.keySet()) {
                File f = Utils.join(blobFile,
                        newHeadCommFileMap.get(file));
                if (f.exists()) {
                    String contents = Utils.readObject(f, String.class);
                    File editMe = Utils.join(cwd, file);
                    Utils.writeContents(editMe, contents);
                }
            }
        }
        stagingArea.reset();
        _head = branchName;
    }

    /**
     * Checks for untracked files in the current working directory.
     * When we run checkout branch, we need to make sure there aren't
     * and untracked files, meaning any files that 1)
     * <p>
     * <p>
     * If a working file is untracked in the current branch and would
     * be overwritten by the checkout, print "There is an untracked file
     * in the way; delete it or add it first"
     *
     * @param givenCommID givC-ID
     */
    private void checkForUntracked(String givenCommID) {
        String msg = "There is an untracked file in the way; ";
        msg += "delete it or add it first.";
        updateUntrackedFiles();
        Commit newComm = getCommit(givenCommID);
        if (_untrackedFiles.size() > 0) {
            for (String fileName : _untrackedFiles.keySet()) {
                if (newComm.getFileMap().containsKey(fileName)) {
                    File myFile = Utils.join(cwd, fileName);
                    String mycontents = Utils.sha1("B" + myFile);
                    if (!newComm.getFileMap().get(fileName)
                            .equals(mycontents)) {
                        throw new GitletException(msg);
                    }
                }
            }
        }
    }

    /**
     * Adds a branch to this repo, doesnt check it out.
     * @param branchName bname
     */
    public void branchCommand(String branchName) {
        if (_branches.containsKey(branchName)) {
            throw new GitletException("A branch with that "
                    + "name already exists.");
        } else {
            _branches.put(branchName, getHeadCommitSha());
        }
    }

    /**
     * removes branch.
     * @param branchName bname
     */
    public void removeBranch(String branchName) {
        if (!_branches.containsKey(branchName)) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        } else if (branchName.equals(_head)) {
            throw new GitletException("Cannot remove the current branch.");
        } else {
            _branches.remove(branchName);
        }
    }


    /**
     * ----------------------------RESET-------------------------------
     * Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in that commit. Also moves the current
     * branch's head to that commit node. The [commit id] may be abbreviated
     * for checkout. The staging area is cleared. The command is essentially
     * checkout of an arbitrary commit that also changes the current branch
     * head.
     * @param commID cID
     */
    public void resetCommand(String commID) {
        commID = convertShortenedID(commID);
        Commit thisCommit = getCommit(commID);
        HashMap<String, String> commitFiles = thisCommit.getFileMap();
        checkForUntracked(commID);
        for (String fileName : commitFiles.keySet()) {
            String[] args = {commID, "--", fileName};
            checkout(args);
        }
        _branches.put(_head, commID);
        stagingArea.reset();
    }


    /**
     * get sha1 id of head commit.
     *
     * @return String
     */
    public String getHeadCommitSha() {
        return _branches.get(_head);
    }


    /**
     * Return commit of given a sha1 id.
     * @param sha1 sha
     */
    public Commit getCommit(String sha1) {
        File f = Utils.join(commitFile, sha1);
        if (f.exists()) {
            Commit c = Utils.readObject(f, Commit.class);
            if (c._merge) {
                return getMergeCommit(sha1);
            } else {
                return c;
            }
        } else {
            throw new GitletException("No commit with that id exists.");
        }
    }

    /**
     * Return the MergeCommit given the sha1 id of the commit.
     * @param sha1 sha
     */
    public MergeCommit getMergeCommit(String sha1) {
        File f = Utils.join(commitFile, sha1);
        if (f.exists()) {
            MergeCommit mc = Utils.readObject(f, MergeCommit.class);
            assert mc._merge;
            return mc;
        } else {
            throw new GitletException("No commit with that id exists.");
        }
    }


    /**
     * For files in the current working directory, or in SA, if they are
     * untracked / modified / deleted, add them
     * Hashmap _untrackedFiles.
     * "Untracked files" = Files present in CWD but neither staged for
     * addition or tracked.
     * <p>
     * in modifications:
     * 1. Tracked in the current commit, changed in the working directory,
     * but not staged.
     * 2. Staged for addition, but with different contents than in the
     * working directory.
     * 3. Staged for addition, but deleted in the working directory.
     * 4. Not staged for removal, but tracked in the current commit and
     * deleted from the working directory.
     */
    private void updateUntrackedFiles() {
        _untrackedFiles = new HashMap<>();
        String headCommID = getHeadCommitSha();
        Commit headComm = getCommit(headCommID);
        for (String fileName : Utils.plainFilenamesIn(cwd)) {
            File f = Utils.join(cwd, fileName);
            if (!headComm.getFileMap().containsKey(fileName)
                    && !fileName.equals(".gitlet")
                    && !stagingArea.toBeAdded.containsKey(fileName)
                    && !fileName.equals(".DS_Store")) {
                String uTblob = "B" + Utils.sha1(Utils.readContentsAsString(f));
                if (headComm._merge) {
                    String[] split = headComm.getMessage().split(" ");
                    String sha1 = _branches.get(split[1]);
                    sha1 = convertShortenedID(sha1);
                    String sha2 = _branches.get(split[3].replace(".", ""));
                    sha2 = convertShortenedID(sha2);
                    Commit c;
                    if (headCommID.equals(sha1)) {
                        c = getCommit(sha2);
                    } else {
                        c = getCommit(sha1);
                    }
                    if (!c.getFileMap().containsKey(fileName)) {
                        _untrackedFiles.put(fileName, uTblob);
                    }
                } else {
                    _untrackedFiles.put(fileName, uTblob);
                }
            } else if (headComm.getFileMap().containsKey(fileName)) {
                String uTblob = "B" + Utils.sha1(Utils.readContentsAsString(f));
                String oBlob = headComm.getFileMap().get(fileName);
                if (!uTblob.equals(oBlob) && !uTblob.equals(
                        stagingArea.toBeAdded.get(fileName))) {
                    if (!stagingArea.removedFiles.contains(fileName)) {
                        _untrackedFiles.put(fileName, uTblob);
                    }
                } else if (!stagingArea.removedFiles.contains(fileName)
                        && !f.exists()) {
                    _untrackedFiles.put(fileName, uTblob);
                }
            } else if (stagingArea.toBeAdded.containsKey(fileName)) {
                String uTblob = "B"
                        + Utils.sha1(Utils.readContentsAsString(f));
                if (!stagingArea.toBeAdded.get(fileName).equals(uTblob)) {
                    _untrackedFiles.put(fileName, uTblob);
                }
            }
        }
        for (String fileName : stagingArea.toBeAdded.keySet()) {
            if (!Utils.join(cwd, fileName).exists()) {
                String uTblob = "B" + Utils.sha1(Utils.readContentsAsString(
                        Utils.join(stagingFile, fileName)));
                _untrackedFiles.put(fileName, uTblob);
            }
        }
    }

    /**
     * For file in untrackedFiles, print them according to the specific class
     * they are in --> modified, deleted, untracked.
     */
    private void printUntrackedFiles() {
        boolean printed = false;
        String headCommID = getHeadCommitSha();
        Commit headComm = getCommit(headCommID);
        HashSet<String> printlater = new HashSet<>();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fileName : _untrackedFiles.keySet()) {
            File thisFile = Utils.join(cwd, fileName);
            if (headComm.getFileMap().containsKey(fileName)) {
                if (!thisFile.exists()) {
                    if (!stagingArea.removedFiles.contains(fileName)) {
                        System.out.println((fileName + " (deleted)"));
                        printed = true;
                    }
                } else {
                    String uTblob = "B" + Utils.sha1(
                            Utils.readContentsAsString(thisFile));
                    String oBlob = headComm.getFileMap().get(fileName);
                    if (!uTblob.equals(oBlob)
                            && !uTblob.equals(
                                    stagingArea.toBeAdded.get(fileName))) {
                        System.out.println(fileName + " (modified)");
                        printed = true;
                    }
                }
            } else if (stagingArea.toBeAdded.containsKey(fileName)) {
                File f = Utils.join(cwd, fileName);
                if (!f.exists()) {
                    System.out.println(fileName + " (deleted)");
                    printed = true;
                } else {
                    String uTblob = "B"
                            + Utils.sha1(Utils.readContentsAsString(f));
                    if (!stagingArea.toBeAdded.get(fileName).equals(uTblob)) {
                        System.out.println(fileName + " (modified)");
                        printed = true;
                    }
                }
            }
            if (!printed) {
                printlater.add(fileName);
            } else {
                printed = false;
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String f : printlater) {
            System.out.println(f);
        }
        System.out.println();
    }
    /**
     * The way we keep track of commits; Hashmap of ...
     * a String of the sha1 of this commit, and a String
     * og the sha1 of this parent's commit.
     */
    private HashMap<String, String> _branches;


    /**
     * ------------------------------M----E----R----G----E-----------
     * ------------------------------------E----R----G----E----------
     * -----------------------------------------R----G----E----------
     * ----------------------------------------------G----E----------
     * ---------------------------------------------------E----------
     * --------------------------------------------------------------
     * Merge.
     * @param branchName bname
     */
    public void mergeCommand(String branchName) {
        if (stagingArea.toBeAdded.size() != 0 || _untrackedFiles.size() != 0) {
            throw new GitletException("You have uncommitted changes.");
        }
        if (!_branches.containsKey(branchName)) {
            throw new GitletException("A branch "
                    + "with that name does not exist.");
        }
        if (branchName.equals(_head)) {
            throw new GitletException("Cannot merge a "
                    + "branch with itself.");
        }
        Commit lCA = getSplitPoint(branchName, _head);
        if (lCA.sha().equals(_branches.get(branchName))) {
            Utils.message("Given branch is an ancestor "
                    + "of the current branch.");
        }
        if (lCA.sha().equals(_branches.get(_head))) {
            String bNHead = _branches.get(branchName);
            _branches.put(_head, bNHead);
            Utils.message("Current branch fast-forwarded.");
        }
        HashMap<String, String> lCAfiles = lCA.getFileMap();
        middleMerge(branchName);
        Commit currComm = getCommit(getHeadCommitSha());
        HashMap<String, String> current = currComm.getFileMap();
        Commit givenComm = getCommit(_branches.get(branchName));
        HashMap<String, String> given = givenComm.getFileMap();
        for (String fileName : given.keySet()) {
            if (!lCAfiles.containsKey(fileName)) {
                if (!current.containsKey(fileName)) {
                    String b = _branches.get(branchName);
                    checkout(new String[]{b, "--", fileName});
                    stagingArea.toBeAdded.put(fileName, given.get(fileName));
                    File mergeFile = Utils.join(stagingFile, fileName);
                    File mContents = Utils.join(blobFile, given.get(fileName));
                    Utils.writeContents(mergeFile,
                            Utils.readObject(mContents, String.class));
                } else if (!given.containsKey(fileName)) {
                    continue;
                } else if (mo(fileName, given, current)) {
                    File c = Utils.join(blobFile, current.get(fileName));
                    File g = Utils.join(blobFile, given.get(fileName));
                    String contents = "<<<<<<< HEAD\n";
                    contents += Utils.readContentsAsString(c);
                    contents += "=======\n";
                    contents += Utils.readContentsAsString(g) + ">>>>>>>";
                    Utils.writeContents(new File(fileName), contents);
                    addCommand(fileName);
                    Utils.message("Encountered a merge conflict.");
                }
            }
        }
        String msg = "Merged " + branchName + " into " + _head + ".";
        mergeCommit(msg, _branches.get(branchName), getHeadCommitSha());

    }

    /**
     * Provides a middle merge.
     * @param branchName bname
     */
    private void middleMerge(String branchName) {
        Commit splitCommit = getSplitPoint(branchName, _head);
        HashMap<String, String> splitFiles = splitCommit.getFileMap();
        Commit currComm = getCommit(getHeadCommitSha());
        HashMap<String, String> current = currComm.getFileMap();
        String givID = _branches.get(branchName);
        Commit givenComm = getCommit(givID);
        HashMap<String, String> given = givenComm.getFileMap();
        checkForUntracked(givID);
        for (String fileName : splitFiles.keySet()) {
            boolean presentInGiven = given.containsKey(fileName);
            boolean modifiedInCurrent = mo(fileName, splitFiles, current);
            boolean modifiedInGiven = mo(fileName, splitFiles, given);
            if (!modifiedInCurrent) {
                if (!presentInGiven) {
                    Utils.restrictedDelete(new File(fileName));
                    removeCommand(fileName);
                    continue;
                }
                if (modifiedInGiven) {
                    String b = _branches.get(branchName);
                    checkout(new String[]{b, "--", fileName});
                    addCommand(fileName);
                }
            }
            if (modifiedInCurrent && modifiedInGiven) {
                if (mo(fileName, given, current)) {
                    mergeConflict(branchName, fileName);
                }
            }
        }
    }


    /**
     * Checks for merge conflicts.
     * @param branchName bname
     * @param fileName fname
     */
    private void mergeConflict(String branchName, String fileName) {
        Commit splitCommit = getSplitPoint(branchName, _head);
        HashMap<String, String> splitFiles = splitCommit.getFileMap();
        Commit currComm = getCommit(getHeadCommitSha());
        HashMap<String, String> current = currComm.getFileMap();
        Commit givenComm = getCommit(_branches.get(branchName));
        HashMap<String, String> given = givenComm.getFileMap();
        File c;
        String cContents;
        if (current.containsKey(fileName)) {
            c = Utils.join(blobFile, current.get(fileName));
            cContents = Utils.readObject(c, String.class);
        } else {
            c = null;
            cContents = "";
        }
        File g;
        String gContents;
        if (given.containsKey(fileName)) {
            g = Utils.join(blobFile, given.get(fileName));
            gContents = Utils.readObject(g, String.class);
        } else {
            g = null;
            gContents = "";
        }
        String contents = "<<<<<<< HEAD\n";
        contents += cContents;
        contents += "=======\n" + gContents;
        contents += ">>>>>>>\n";
        Utils.writeContents(Utils.join(cwd, fileName), contents);
        addCommand(fileName);
        Utils.message("Encountered a merge conflict.");
    }

    /**
     * Return Commit of split point of branches.
     * @param b1 b1
     * @param b2 b2
     */
    public Commit getSplitPoint(String b1, String b2) {
        Commit splitpoint = null;
        Commit b1Commit = getCommit(_branches.get(b1));
        ArrayList<String> b1CommitList = new ArrayList<>();
        while (b1Commit != null) {
            b1CommitList.add(b1Commit.sha());
            b1Commit = b1Commit.parent();
        }
        Commit b2Commit = getCommit(_branches.get(b2));
        ArrayList<String> b2CommitList = new ArrayList<>();
        while (b2Commit != null) {
            b2CommitList.add(b2Commit.sha());
            b2Commit = b2Commit.parent();
        }
        for (int i = b1CommitList.size() - 1; i > 0; i--) {
            for (int j = b2CommitList.size() - 1; j > 0; j--) {
                if (b1CommitList.get(i).equals(b2CommitList.get(j))) {
                    splitpoint = getCommit(b1CommitList.get(i));
                    break;
                }
            }
        }
        if (splitpoint == null) {
            throw new GitletException();
        } else {
            return splitpoint;
        }
    }


    /**
     * Returns a boolean if the file with name fileName has been
     * modified from branch A to branch B.
     *
     * @param fileName fname
     * @param a a
     * @param b b
     */
    boolean mo(String fileName, HashMap<String, String> a,
               HashMap<String, String> b) {
        if (a.containsKey(fileName) && b.containsKey(fileName)) {
            String blobA = a.get(fileName);
            String blobB = b.get(fileName);
            return !blobA.equals(blobB);
        } else {
            return a.containsKey(fileName) || b.containsKey(fileName);
        }
    }
}
