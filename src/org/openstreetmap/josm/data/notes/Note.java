// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * A map note. It always has at least one comment since a comment is required
 * to create a note on osm.org
 */
public class Note {

    public enum State { open, closed }

    private long id;
    private LatLon latLon;
    private Date createdAt;
    private State state;
    private String noteUrl;
    private List<NoteComment> comments = new ArrayList<NoteComment>();

    public Note(LatLon latLon) {
        this.latLon = latLon;
    }

    /** @return The unique OSM ID of this note */
    public long getId() {
    	return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /** @return The geographic location of the note */
    public LatLon getLatLon() {
        return latLon;
    }

    /** @return Date that this note was submitted */
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /** @return The open or closed state of this note */
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /** @return An ordered list of comments associated with this note */
    public List<NoteComment> getComments() {
        return comments;
    }

    public String getNoteUrl() {
    	return noteUrl;
    }

    public void setNoteUrl(String url) {
        Main.debug("note URL: " + url);
    	this.noteUrl = url;
    }

    public void addComment(NoteComment comment) {
        this.comments.add(comment);
    }

    /**
     * Returns the comment that was submitted by the user when creating the note
     * @return First comment object
     */
    public NoteComment getFirstComment() {
        return this.comments.get(0);
    }

    /**
     * Copies values from a new note into an existing one. Used after a note
     * has been updated on the server and the local copy needs refreshing.
     * @param note
     */
    public void updateWith(Note note) {
        this.comments = note.comments;
        this.createdAt = note.createdAt;
        this.id = note.id;
        this.state = note.state;
        this.latLon = note.latLon;
        this.noteUrl = note.noteUrl;
    }

}
