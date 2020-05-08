package gitlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/** Class of Merge Commit, a type of Commit.
 * @author charlesellis
 * */
public class MergeCommit extends Commit {
    /**
     * ******COMMIT.
     *
     * @param message msg
     * @param parent p
     * @param fileMap fm
     * @param merge mrge
     *
     * Same thing as commit but a MergeCommit.
     * @param parents p's
     * */
    public MergeCommit(String message, Commit parent,
                       HashMap<String, String> fileMap,
                       boolean merge, String[] parents) {
        super(message, parent, fileMap, merge);
        assert merge;
        String[] split = message.split(" ");
        String sha1 = parents[0];
        String sha2 = parents[1];
        _parentSha = sha1 + sha2;
        Date date = new Date();
        _timestamp = getDate(date);
        List<Object> toSha = new ArrayList<Object>();
        byte[] bytes = Utils.serialize(this);
        toSha.add(bytes);
        toSha.add(getMessage());
        toSha.add(_parentSha);
        toSha.add(timestamp());
        _sha = Utils.sha1(toSha);
    }

    /**
     * Return sha1 id for this commit.
     */
    String sha() {
        return _sha;
    }


}
