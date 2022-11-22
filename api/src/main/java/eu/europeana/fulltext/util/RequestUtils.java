package eu.europeana.fulltext.util;

import eu.europeana.fulltext.exception.InvalidRequestParamException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestUtils {

    public static final String PROFILE_TEXT           = "text";
    public static final String PROFILE_DEBUG          = "debug";

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

}
