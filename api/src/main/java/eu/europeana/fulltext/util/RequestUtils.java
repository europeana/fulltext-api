package eu.europeana.fulltext.util;

import eu.europeana.fulltext.exception.InvalidRequestParamException;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestUtils {

    public static final String PROFILE_TEXT           = "text";
    public static final String PROFILE_DEBUG          = "debug";

    public RequestUtils() {
    }

    public static List<String> extractProfiles(String profileParam) throws InvalidRequestParamException {
        // Now profile can be profile=text OR
        // profile=text,debug OR profile=debug (for error stack trace purpose)
        // validate profiles
        List<String> profiles = new ArrayList<>();
        if (StringUtils.isNotEmpty(profileParam)) {
            profiles = Arrays.asList(StringUtils.split(profileParam, ","));
            for (String val : profiles) {
                if (!StringUtils.equals(val, PROFILE_TEXT) && !StringUtils.equals(val, PROFILE_DEBUG)) {
                    throw new InvalidRequestParamException("profile", val);
                }
            }
        }
        return profiles;
    }

    /** Method is similar to ClientUtils.escapeQueryChars
     * Method does not escape the '"' character
     * See EA-3787
     * @param
     * @return
     */
    public static String escapeQueryChars(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c) || Set.of('+', '-', '!', '(', ')', ':',
                '^', '[', ']', '{', '}', '~', '*', '?', '|', '&', ';', '/').contains(c)) {
                stringBuilder.append('\\');
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

}
