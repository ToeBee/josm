// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import java.util.Date;

import org.openstreetmap.josm.data.osm.User;

/**
 * Represents a comment made on a note. All notes have at least on comment
 * which is the comment the note was opened with. Comments are immutable.
 */
public class NoteComment {

    private String text;
    private String htmlText;
    private User user;
    private Date commentTimestamp;

    public NoteComment(Date createDate, User user, String comment, String htmlText) {
        this.text = comment;
        this.htmlText = htmlText;
        this.user = user;
        this.commentTimestamp = createDate;
    }

    /** @return Plain text of user's comment */
    public String getText() {
        return text;
    }

    /** @return HTML formatted comment text */
    public String getHtmlText() {
        return htmlText;
    }

    /** @return JOSM's User object for the user who made this comment */
    public User getUser() {
        return user;
    }

    /** @return The time at which this comment was created */
    public Date getCommentTimestamp() {
        return commentTimestamp;
    }
}
