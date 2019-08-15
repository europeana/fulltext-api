package eu.europeana.fulltext.util;

import java.text.ParseException;

/**
 * Created by luthien on 2019-06-13, based on gdata-java-client
 * Time specification object which tries to conform to section 3.6
 * of RFC 2326 (Normal Play Time).
 * <p>
 * It only supports a millisecond precision. Any time more precise than
 * that will be lost when parsing.
 */
public class NormalPlayTime {

    private long    ms;

    /**
     * Creates a NormalPlayTime object.
     * @param ms time offset, in milliseconds
     */

    private NormalPlayTime(long ms) {
        this.ms = ms;
    }

    /**
     * Returns the offset in milliseconds
     */
    public long getTimeOffsetMs() {
        return ms;
    }

    /**
     * Parses a NormalPlayTime object and return it.
     *
     * @param stringRep string representation
     * @return a NormalPlayTime, null if and only if stringRep == null
     * @throws ParseException if the string representation could not
     *                        be parsed
     */
    public static NormalPlayTime parse(String stringRep) throws ParseException {
        if (stringRep == null) {
            return null;
        }

        NPTParser parser = new NPTParser(stringRep);
        return new NormalPlayTime(parser.parse());
    }

    /**
     * Gets a valid string representation (seconds "." fraction).
     */
    @Override
    public String toString() {
        return getNptSecondsRepresentation();
    }

    /**
     * Gets the standard {@code seconds.fraction } representation
     * for this object.
     *
     * @return {@code seconds.fraction}
     */
    private String getNptSecondsRepresentation() {

        long seconds  = ms / 1000L;
        long fraction = ms % 1000L;
        if (fraction == 0) {
            return Long.toString(seconds);
        }
        return String.format("%1$d.%2$03d", seconds, fraction);
    }

    /**
     * Gets the standard {@code  hh:mm:ss.fraction } representation
     * for this object.
     *
     * @return {@code hh:mm:ss.fraction}
     */
    public String getNptHhmmssRepresentation() {
        return msToHHmmss(ms);
    }

    public static String msToHHmmss(long millis){
        long fraction     = millis % 1000L;
        long totalseconds = millis / 1000L;
        long seconds      = totalseconds % 60L;
        long totalminutes = totalseconds / 60L;
        long minutes      = totalminutes % 60L;
        long hours        = totalminutes / 60L;
        if (fraction > 0) {
            return String.format("%1$02d:%2$02d:%3$02d.%4$03d", hours, minutes, seconds, fraction);
        } else {
            return String.format("%1$02d:%2$02d:%3$02d.000", hours, minutes, seconds);
        }
    }

    /**
     * Parser class for a NormalPlayTime that supports both time representations.
     */
    private static class NPTParser {
        private final String text;
        private final int    length;
        private       int    currentIndex;
        /**
         * Current character, 0 when the end is reached.
         */
        private       char   current;

        private static final char EOF = '\0';

        NPTParser(String text) {
            this.text = text;
            this.length = text.length();
            currentIndex = -1;
            next();
        }

        private long parse() throws ParseException {
            long ms;
            int  first = parseNumber();

            if (current == ':') {
                next();
                long minutes = parseNumber();
                assertCurrentIs(':');
                next();
                long seconds = parseNumber();
                ms = ((((first * 60L) + minutes) * 60L) + seconds) * 1000L;
            } else {
                ms = first * 1000L;
            }
            if (current == '.') {
                next();
                int exp = 100;
                for (int i = 0; i <= 3 && isDigit(); next(), i++, exp /= 10) {
                    ms += exp * digitValue();
                }
                // Ignore extra fraction which can't be stored
                parseNumber();
            }
            assertCurrentIs(EOF);
            return ms;
        }

        private int parseNumber() {
            int retval;
            for (retval = 0; isDigit(); next()) {
                retval *= 10;
                retval += digitValue();
            }
            return retval;
        }

        private int digitValue() {
            return current - '0';
        }

        private boolean isDigit() {
            return current >= '0' && current <= '9';
        }

        private void assertCurrentIs(char c) throws ParseException {
            if (c != current) {
                throw new ParseException("Unexpected character", currentIndex);
            }
        }

        private void next() {
            currentIndex++;
            if (currentIndex >= length) {
                current = EOF;
            } else {
                current = text.charAt(currentIndex);
            }
        }
    }

}