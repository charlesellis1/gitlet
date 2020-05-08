package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Commit class.
 *
 * @author charlesellis */
public class Commit implements Serializable {

    /**
     * String of commit message.
     */
    private String _message;

    /**
     * String of timestamp.
     */
    protected String _timestamp;

    /**
     * Parent commit of this commit.
     */
    protected Commit _parent;

    /**
     * Hashmap fileMap representing the files and blobs of this commit.
     */
    protected HashMap<String, String> _fileMap;

    /**
     * String of sha1 ID of this commit.
     */
    protected String _sha;

    /**
     * String of the sha1 ID of this commit's parent.
     */
    protected String _parentSha;

    /**
     * Returns true if this commit is a merge commit.
     */
    protected boolean _merge;


    /** Commit.
     *
     * @param message msg
     * @param parent p
     * @param fileMap fm
     * @param merge mrge  */
    public Commit(String message, Commit parent,
                  HashMap<String, String> fileMap, boolean merge) {
        this._message = message;
        this._parent = parent;
        this._fileMap = fileMap;
        this._merge = merge;
        Date date;
        if (!message.equals("initial commit")) {
            date = new Date();
            _timestamp = getDate(date);
        } else {
            _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
            _fileMap = new HashMap<>();
        }
        List<Object> toSha = new ArrayList<Object>();
        if (_parent == null) {
            _parentSha = "imaGDI";
        } else {
            _parentSha = _parent.sha();
        }
        byte[] bytes = Utils.serialize(this);
        toSha.add(bytes);
        toSha.add(getMessage());
        toSha.add(_parentSha);
        toSha.add(timestamp());
        _sha = Utils.sha1(toSha);
    }


    /**
     * Return timestamp for this commit.
     */
    String timestamp() {
        return _timestamp;
    }

    /**
     * Return parent commit for this commit.
     */
    Commit parent() {
        return _parent;
    }

    /**
     * Return timestamp for this commit.
     */
    HashMap<String, String> getFileMap() {
        return _fileMap;
    }

    /**
     * Return timestamp for this commit.
     */
    String getMessage() {
        return _message;
    }

    /**
     * Return sha1 id for this commit.
     */
    String sha() {
        return _sha;
    }


    /**
     * The date format.
     *
     * @param d date
     *
     * @return String
     */
    protected String getDate(Date d) {
        return DATE_FORMAT.format(d);
    }

    /**
     * Returns simple date format with right form.
     */
    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("E MMM d HH:mm:ss y Z");
}
