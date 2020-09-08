package eu.europeana.fulltext;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.fulltext.api.config.FTDefinitions.*;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_JSONLD;

public class RequestUtils {
    public static final Pattern ACCEPT_PROFILE_PATTERN = Pattern.compile("profile=\"(.*?)\"");
    public static final String  ACCEPT                 = "Accept";
    public static final String  ACCEPT_JSON            = "Accept=" + MEDIA_TYPE_JSON;
    public static final String  ACCEPT_JSONLD          = "Accept=" + MEDIA_TYPE_JSONLD;
    public static final String  ACCEPT_VERSION_INVALID = "Unknown profile or format version";
    public static final String  CONTENT_TYPE           = "Content-Type";

    public static final String  PROFILE_TEXT           = "text";

    public static final String REQUEST_VERSION_2 = "2";
    public static final String REQUEST_VERSION_3 = "3";


    /**
     * Retrieve the requested version from the accept header, or if not present from the format parameter. If nothing
     * is specified then 2 is returned as default
     * @return either version 2, 3 or ACCEPT_INVALID
     */
    public static String getRequestVersion(HttpServletRequest request, String format) {
        String result = null;
        String accept = request.getHeader(ACCEPT);
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = ACCEPT_PROFILE_PATTERN.matcher(accept);
            if (m.find()) { // found a Profile parameter in the Accept header
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V3)) {
                    result = REQUEST_VERSION_3;
                } else if (profiles.toLowerCase(Locale.getDefault()).contains(MEDIA_TYPE_IIIF_V2)) {
                    result = REQUEST_VERSION_2;
                } else {
                    result = ACCEPT_VERSION_INVALID; // in case a Profile is found that matches neither version => HTTP 406
                }
            }
        }
        if (result == null) {
            // Request header is empty, or does not contain a Profile parameter
            if (StringUtils.isBlank(format)){
                result = REQUEST_VERSION_2;    // if format not given, fall back to default REQUEST_VERSION_2
            } else if (REQUEST_VERSION_2.equals(format) || REQUEST_VERSION_3.equals(format)) {
                result = format; // else use the format parameter
            } else {
                result = ACCEPT_VERSION_INVALID;
            }
        }
        return result;
    }

}
