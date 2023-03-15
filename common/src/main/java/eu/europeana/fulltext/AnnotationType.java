package eu.europeana.fulltext;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Enumeration of all Annotation types. The one-letter abbreviation is used to store the annotation type in the
 * fulltext mongo database
 *
 * @author Patrick Ehlert
 * Created 18 Jun 2020
 */
public enum AnnotationType {

    PAGE('P'),
    BLOCK('B'),
    LINE('L'),
    WORD('W'),
    MEDIA('M'),
    CAPTION('C');

    private char abbreviation;

    AnnotationType(char abbreviation) {
        this.abbreviation = abbreviation;
    }

    /**
     * Given the one-character abbreviation, find the corresponding AnnotationType
     * @param abbreviation character, the abbreviation of the annotation type
     * @return the AnnotationType that corresponds with the provided abbreviation, or null if it's unknown
     */
    public static AnnotationType fromAbbreviation(char abbreviation) {
        for (AnnotationType annoType : AnnotationType.values()) {
            if (annoType.getAbbreviation() == abbreviation) {
                return annoType;
            }
        }
        return null;
    }

    public char getAbbreviation() {
        return this.abbreviation;
    }

    /**
     * @return the name of the annotation type in a public friendly format (capitalized first letter)
     */
    public String getDisplayName() {
        return StringUtils.capitalize(this.name().toLowerCase(Locale.GERMAN));
    }

    /**
     * @return the name of the annotation type in a public friendly format (capitalized first letter)
     */
    public String getLowerCaseName() {
        return this.name().toLowerCase(Locale.GERMAN);
    }

    /**
     * Check if the provided string is either a valid abbreviation or name of an existing AnnotationType
     * Note that the provided value can have any capitalization.
     * @return an AnnotationType if we can find a corresponding type, otherwise null
     */
    public static AnnotationType fromAbbreviationOrName(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 1) {
            return AnnotationType.fromAbbreviation(value.toUpperCase(Locale.GERMAN).charAt(0));
        }
        try {
            return AnnotationType.valueOf(value.toUpperCase(Locale.GERMAN));
        } catch (IllegalArgumentException e) {
            // ignore, apparently user provided incorrect value
            return null;
        }
    }

}
