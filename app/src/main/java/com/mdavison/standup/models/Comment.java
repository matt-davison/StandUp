package com.mdavison.standup.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * This model represents a comment and is compatible with the Parse database
 */
@ParseClassName("Comment")
public class Comment extends ParseObject {

    public static final String KEY_COMMENT = "comment";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_LIKES = "likes";
    public static final String KEY_LIKERS = "likers";
    public static final String KEY_SUB_COMMENTS = "subComments";

    public Comment() {
        //required empty constructor
    }

    public String getComment() {
        return getString(KEY_COMMENT);
    }

    public void setComment(String comment) {
        put(KEY_COMMENT, comment);
    }

    public ParseUser getAuthor() {
        return getParseUser(KEY_AUTHOR);
    }

    public void setAuthor(ParseUser user) {
        put(KEY_AUTHOR, user);
    }
}