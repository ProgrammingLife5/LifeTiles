package nl.tudelft.lifetiles.core.util;

/**
 * A message, used as shout identifier.
 *
 * @author joren
 * @author Rutger van den Berg
 *
 */
public final class Message {

    /**
     * A shout message indicating data has been filtered.
     */
    public static final Message FILTERED = create("filtered");
    /**
     * A shout message indicating data has been opened.
     */
    public static final Message OPENED = create("opened");
    /**
     * A shout message indicating data has been loaded.
     */
    public static final Message LOADED = create("loaded");
    /**
     * A shout message indicating that the filters have been reset.
     */
    public static final Message RESET = create("reset");
    /**
     * A shout message indicating there is a zoom action.
     */
    public static final Message ZOOM = create("zoom");
    /**
     * A shout message indicating the bookmark sidebar needs to appear.
     */
    public static final Message BOOKMARKS = create("bookmarks");
    /**
     * A shout message indicating we want to move to a location in the genome.
     */
    public static final Message GOTO = create("goto");
    /**
     * A shout message indicating we want to create a new bookmark.
     */
    public static final Message CREATE_BOOKMARK = create("new bookmark");

    /**
     * The value.
     */
    private final String value;

    /**
     * Create a new message.
     *
     * @param message
     *            the value of the message
     */
    private Message(final String message) {
        value = message;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Factory method.
     *
     * @param message
     *            the message string
     * @return the Message.
     */
    public static Message create(final String message) {
        return new Message(message);
    }

}
