package gitlet;

import ucb.junit.textui;
import org.junit.Test;


import java.io.File;

/** The suite of all JUnit tests for the gitlet package.
 *  @author charlesellis
 */
public class UnitTest {
    private void makeDirectory() {
        System.setProperty("user.dir",
                new File("/Users/charlesellis/Desktop/gits").getAbsolutePath());
    }

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void initTest() {
        makeDirectory();
        Main.main("init");
    }

    @Test
    public void addTest() {
        makeDirectory();
        Main.main("add", "Hello.txt");
        Main.main("add", "m.txt");
        Main.main("add", "cheers.txt");
    }

    @Test
    public void commitTest1() {
        makeDirectory();
        Main.main("commit", "Two files");
    }
    @Test
    public void commitTest2() {
        makeDirectory();
        Main.main("commit", "Added k, removed cheers");
    }
    @Test
    public void commitTest3() {
        makeDirectory();
        Main.main("commit", "soup commit");
    }

    @Test
    public void basicLogTest() {
        makeDirectory();
        Main.main("log");
    }

    @Test
    public void basicStatusTest() {
        makeDirectory();
        Main.main("status");
    }

    @Test
    public void basicCheckoutTest() {
        makeDirectory();
        Main.main("checkout", "master");
    }

    @Test
    public void basicBranchTest() {
        makeDirectory();
        Main.main("branch", "other");
    }

    @Test
    public void removeBranchTest() {
        makeDirectory();
        Main.main("rm-branch", "");
    }
    @Test
    public void rmTest() {
        makeDirectory();
        Main.main("rm", "cheers.txt");
    }

    @Test
    public void basicMergeTest() {
        makeDirectory();
        Main.main("merge", "mybranch");
    }

    @Test
    public void findTest() {
        makeDirectory();
        Main.main("find", "Commit #1");
    }
}



