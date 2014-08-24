// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.User;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to read Note objects from their XML representation
 */
public class NoteReader {

    private InputSource inputSource;
    private List<Note> parsedNotes;

    private class ApiParser extends DefaultHandler {

        private StringBuffer accumulator = new StringBuffer();
        private final SimpleDateFormat NOTE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

        private List<Note> notes = new ArrayList<Note>();
        private Note thisNote;

        private Date commentCreateDate;
        private String commentUsername;
        private long commentUid;
        private String commentText;
        private String commentHtmlText;

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            accumulator.setLength(0);
            if ("note".equals(qName)) {
                double lat = Double.parseDouble(atts.getValue("lat"));
                double lon = Double.parseDouble(atts.getValue("lon"));
                LatLon noteLatLon = new LatLon(lat, lon);
                thisNote = new Note(noteLatLon);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            accumulator.append(ch, start, length);
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            switch (qName) {
            case "id":
                thisNote.setId(Long.parseLong(accumulator.toString()));
                break;
            case "status":
                thisNote.setState(Note.State.valueOf(accumulator.toString()));
                break;
            case "url":
                thisNote.setNoteUrl(accumulator.toString());
                break;
            case "date_created":
                try {
                    thisNote.setCreatedAt(NOTE_DATE_FORMAT.parse(accumulator.toString()));
                } catch (ParseException e) {
                    Main.error("Could not parse note date from API: \"" + accumulator.toString() + "\":");
                    e.printStackTrace();
                }
                break;
            case "note":
                notes.add(thisNote);
                break;
            case "date":
                try {
                    commentCreateDate = NOTE_DATE_FORMAT.parse(accumulator.toString());
                } catch (ParseException e) {
                    Main.error("Could not parse comment date from API: \"" + accumulator.toString() + "\":");
                    e.printStackTrace();
                }
                break;
            case "user":
                commentUsername = accumulator.toString();
                break;
            case "uid":
                commentUid = Long.parseLong(accumulator.toString());
                break;
            case "text":
                commentText = accumulator.toString();
                break;
            case "html":
                commentHtmlText = accumulator.toString();
                break;
            case "comment":
                User commentUser = User.createOsmUser(commentUid, commentUsername);
                thisNote.addComment(new NoteComment(commentCreateDate, commentUser, commentText, commentHtmlText));
                commentUid = 0;
                commentUsername = null;
                commentCreateDate = null;
                commentText = null;
                break;
            default:
                Main.debug("ignoring XML element: " + qName);
                break;
            }
        }

        @Override
        public void endDocument() throws SAXException  {
            Main.debug("parsed notes: " + notes.size());
            parsedNotes = notes;
        }

    }

    /**
     * Initializes the reader with a given InputStream
     * @param source - InputStream containing Notes XML
     * @throws IOException
     */
    public NoteReader(InputStream source) throws IOException {
        this.inputSource = new InputSource(source);
    }

    /**
     * Parses the InputStream given to the constructor and returns
     * the resulting Note objects
     * @return List of Notes parsed from the input data
     * @throws SAXException
     * @throws IOException
     */
    public List<Note> parse() throws SAXException, IOException {
        ApiParser parser = new ApiParser();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newSAXParser().parse(inputSource, parser);
        } catch (ParserConfigurationException e) {
            Main.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
        return parsedNotes;
    }
}
