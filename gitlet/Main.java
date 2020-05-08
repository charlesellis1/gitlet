package gitlet;


import java.io.File;
import java.util.Arrays;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author charlesellis
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            }
            if (!isValidCommand(args[0])) {
                throw new GitletException("No command with that name exists.");
            }
            incorrectOps(args);
            if (!args[0].equals("init") && myRepo == null) {
                myRepo = getMyRepo();
            }
            if (args[0].equals("init")) {
                myRepo = new Repo();
                myRepo.init();
            } else if (args[0].equals("add")) {
                myRepo.addCommand(args[1]);
            } else if (args[0].equals("commit")) {
                myRepo.commitCommand(args[1]);
            } else if (args[0].equals("log")) {
                myRepo.log();
            } else if (args[0].equals("global-log")) {
                myRepo.globalLog();
            } else if (args[0].equals("checkout")) {
                String[] operands = Arrays.copyOfRange(args, 1, args.length);
                if (operands.length == 1) {
                    myRepo.checkout(operands[0]);
                } else {
                    myRepo.checkout(operands);
                }
            } else if (args[0].equals("branch")) {
                myRepo.branchCommand(args[1]);
            } else if (args[0].equals("rm-branch")) {
                myRepo.removeBranch(args[1]);
            } else if (args[0].equals("status")) {
                myRepo.status();
            } else if (args[0].equals("find")) {
                myRepo.findCommand(args[1]);
            } else if (args[0].equals("reset")) {
                myRepo.resetCommand(args[1]);
            } else if (args[0].equals("rm")) {
                myRepo.removeCommand(args[1]);
            } else if (args[0].equals("merge")) {
                myRepo.mergeCommand(args[1]);
            }
            Utils.writeObject(repoFile, myRepo);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * Returns true of theres a repo initialized
     * / if we have run the init command already.
     */
    public static boolean repoInitialized() {
        String f = System.getProperty("user.dir");
        File tmpDir = new File(f + "/.gitlet");
        if (tmpDir.exists()) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if command is a valid command.
     * @param arg arg
     */
    public static boolean isValidCommand(String arg) {
        boolean bool = false;
        for (String command : commands) {
            if (arg.equals(command)) {
                bool = true;
            }
        }
        return bool;
    }

    /**
     * String array of valid commands, all the first argument.
     */
    private static String[] commands = new String[]{ "init", "add",
            "commit", "rm", "log", "global-log", "find", "status",
            "checkout", "branch", "rm-branch", "reset", "merge"};


    /**
     * Refers to current repository.
     */
    private static Repo myRepo;

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
    private static File repoFile = Utils.join(gitfile, "/repo");
    /**
     * Returns Repo, reading bytes in the repo file.
     */
    public static Repo getMyRepo() {
        if (!repoInitialized()) {
            throw new GitletException("No gitlet directory initialized.");
        }
        String currentDir = System.getProperty("user.dir");
        File cwd = Utils.join(currentDir);
        File gitFile = Utils.join(cwd, "/.gitlet");
        File repoFile = Utils.join(gitFile, "/repo");
        return Utils.readObject(repoFile, Repo.class);
    }

    /** Checks if operands are incorrect.
     * @param args the args
     **/
    public static void incorrectOps(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        if (!isValidCommand(args[0])) {
            throw new GitletException("No command with that name exists.");
        }
        if (args[0].equals("init")) {
            if (args.length > 1) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("add")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("commit")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("log")) {
            if (args.length > 1) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("global-log")) {
            if (args.length > 1) {
                throw new GitletException("Incorrect operands.");
            }

        } else if (args[0].equals("branch")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("rm-branch")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("status")) {
            if (args.length > 1) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("find")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("reset")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("rm")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (args[0].equals("merge")) {
            if (args.length > 2) {
                throw new GitletException("Incorrect operands");
            }
        }
    }
}
