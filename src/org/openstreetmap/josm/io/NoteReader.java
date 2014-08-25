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
import org.openstreetmap.josm.data.notes.NoteComment.Action;
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
    private NoteParseMode parseMode;

    /**
     * Notes can be represented in two XML formats. One is returned by the API
     * while the other is used to generate the notes dump file. The parser
     * needs to know which one it is handling.
     */
    public enum NoteParseMode {API, DUMP}

    /**
     * Parser for the notes dump file format.
     * It is completely different from the API XML format.
     */
    private class DumpParser extends DefaultHandler {
        private StringBuffer buffer = new StringBuffer();
        private final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH);

        private List<Note> notes = new ArrayList<Note>(100000);
        private Note thisNote;

        private Date commentCreateDate;
        private String commentUsername;
        private long commentUid;
        private Action noteAction;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            buffer.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case "note":
                    notes.add(thisNote);
                    break;
                case "comment":
                    User commentUser = User.createOsmUser(commentUid, commentUsername);
                    thisNote.addComment(new NoteComment(commentCreateDate, commentUser, buffer.toString(), noteAction));
                    commentUid = 0;
                    commentUsername = null;
                    commentCreateDate = null;
                    break;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            buffer.setLength(0);
            switch(qName) {
            case "note":
                double lat = Double.parseDouble(attrs.getValue("lat"));
                double lon = Double.parseDouble(attrs.getValue("lon"));
                LatLon noteLatLon = new LatLon(lat, lon);
                thisNote = new Note(noteLatLon);
                thisNote.setId(Long.parseLong(attrs.getValue("id")));
                String closedTimeStr = attrs.getValue("closed_at");
                if(closedTimeStr == null) {
                    thisNote.setState(Note.State.open);
                } else {
                    thisNote.setClosedAt(parseIso8601Date(closedTimeStr));
                }
                thisNote.setCreatedAt(parseIso8601Date(attrs.getValue("created_at")));
                break;
            case "comment":
                commentUid = Long.parseLong(attrs.getValue("uid"));
                commentUsername = attrs.getValue("user");
                noteAction = Action.valueOf(attrs.getValue("action"));
                commentCreateDate = parseIso8601Date(attrs.getValue("timestamp"));
                break;
            }
        }

        /**
         * Convenience method to handle the date parsing try/catch. If there is ever an exception
         * parsing the dates, it indicates an error in the notes dump file.
         * @param dateStr - String to parse
         * @return Parsed date
         */
        private Date parseIso8601Date(String dateStr) {
            try {
                return ISO8601_FORMAT.parse(dateStr);
            } catch(ParseException e) {
                Main.error("error parsing date in note dump");
                return null;
            }
        }

    }

    private class ApiParser extends DefaultHandler {

        private StringBuffer accumulator = new StringBuffer();
        private final SimpleDateFormat NOTE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

        private List<Note> notes = new ArrayList<Note>();
        private Note thisNote;

        private Date commentCreateDate;
        private String commentUsername;
        private long commentUid;
        private String commentText;
        private Action commentAction;

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
                thisNote.setCreatedAt(parseDate(accumulator.toString()));
                break;
            case "note":
                notes.add(thisNote);
                break;
            case "date":
                commentCreateDate = parseDate(accumulator.toString());
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
            case "comment":
                User commentUser = User.createOsmUser(commentUid, commentUsername);
                thisNote.addComment(new NoteComment(commentCreateDate, commentUser, commentText, commentAction));
                commentUid = 0;
                commentUsername = null;
                commentCreateDate = null;
                commentText = null;
                break;
            case "action":
                commentAction = Action.valueOf(accumulator.toString());
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

        /**
         * Convenience method to handle the date parsing try/catch. If there is ever an exception
         * parsing the dates, it indicates an error in the notes API.
         * @param dateStr - String to parse
         * @return Parsed date
         */
        private Date parseDate(String dateStr) {
            try {
                return NOTE_DATE_FORMAT.parse(dateStr);
            } catch(ParseException e) {
                Main.error("error parsing date in API parser");
                return null;
            }
        }
    }

    /**
     * Initializes the reader with a given InputStream
     * @param source - InputStream containing Notes XML
     * @param parseMode - Indicate if we are parsing API or dump file style XML
     * @throws IOException
     */
    public NoteReader(InputStream source, NoteParseMode parseMode) throws IOException {
        this.inputSource = new InputSource(source);
        this.parseMode = parseMode;
    }

    /**
     * Parses the InputStream given to the constructor and returns
     * the resulting Note objects
     * @return List of Notes parsed from the input data
     * @throws SAXException
     * @throws IOException
     */
    public List<Note> parse() throws SAXException, IOException {
        DefaultHandler parser;
        if(parseMode == NoteParseMode.DUMP) {
            parser = new DumpParser();
        } else {
            parser = new ApiParser();
        }
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
