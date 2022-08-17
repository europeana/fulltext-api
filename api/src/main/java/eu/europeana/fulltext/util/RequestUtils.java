package eu.europeana.fulltext.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;

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
    public static final String  PROFILE_DEBUG          = "debug";

    public static final String REQUEST_VERSION_2 = "2";
    public static final String REQUEST_VERSION_3 = "3";


    /**
     * Retrieve the requested version from accept header if present
     * OR if not present, check the format parameter.
     * If nothing is specified then 2 is returned as default
     *
     * @return either version 2, 3 or null (if invalid)
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
                    result = null; // in case profile is found & it's invalid.
                }
                return result; // if accept header present and contains profile; return the value processed
            }
        }

        // Check if format is present in the request param
        if (StringUtils.isBlank(format)) {
            result = REQUEST_VERSION_2;    // if format not given, fall back to default REQUEST_VERSION_2
        } else if (REQUEST_VERSION_2.equals(format) || REQUEST_VERSION_3.equals(format)) {
            result = format; // else use the format parameter
        }
        return result;
    }

    public static void addContentTypeToResponseHeader(HttpHeaders headers, String version, boolean isJson) {
        if ("3".equalsIgnoreCase(version)) {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V3);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V3);
            }
        } else {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V2);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V2);
            }
        }
    }



}
