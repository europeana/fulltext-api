package eu.europeana.iiif;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Common functionality for handling accept headers and profile parameters useed by IIIF Manifest and Fulltext API
 *
 */
public final class AcceptUtils {

    public static final String MEDIA_TYPE_JSONLD         = "application/ld+json";
    public static final String MEDIA_TYPE_JSON           = "application/json";

    public static final String CHARSET_UTF_8             = "charset=UTF-8";
    public static final String ACCEPT_DELIMITER          = ";";

    public static final Pattern ACCEPT_PROFILE_PATTERN = Pattern.compile("profile=\"(.*?)\"");
    public static final Pattern ACCEPT_MEDIA_TYPE_PATTERN = Pattern.compile("^(http|https):(\\/\\/)iiif.io\\/api\\/presentation\\/(2|3)\\/context.json");

    public static final String ACCEPT                 = "Accept";
    public static final String ACCEPT_JSON            = "Accept=" + MEDIA_TYPE_JSON;
    public static final String ACCEPT_JSONLD          = "Accept=" + MEDIA_TYPE_JSONLD;
    public static final String ACCEPT_VERSION_INVALID = "Unknown IIIF version in Accept header or format parameter";

    public static final String CONTENT_TYPE           = "Content-Type";

    public static final String REQUEST_VERSION_2 = "2";
    public static final String REQUEST_VERSION_3 = "3";


    private AcceptUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Retrieve the requested version from accept header if present
     * OR if not present, check the provided format parameter value.
     * If nothing is specified then 2 is returned as default
     * @param request the incoming httpservletrequest to process
     * @param format the format parameter value (if available, can be null is not provided)
     * @return either version 2, 3 or null (if invalid)
     */
    public static String getRequestVersion(HttpServletRequest request, String format) {
        String accept = request.getHeader(ACCEPT);
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = ACCEPT_PROFILE_PATTERN.matcher(accept);
            if (m.find()) { // found a Profile parameter in the Accept header
                String profiles = m.group(1);
                return getVersionFromProfile(profiles); // if accept header present and contains profile; return the value processed
            }
        }
        // Check if format is present in the request param
        if (StringUtils.isBlank(format)) {
            return REQUEST_VERSION_2;    // if format not given, fall back to default REQUEST_VERSION_2
        } else if (REQUEST_VERSION_2.equals(format) || REQUEST_VERSION_3.equals(format)) {
            return format; // else use the format parameter
        }
        return null;
    }

    /**
     * Matches the Accept header media type pattern and
     * fetches the version present in it.
     *
     * NOTE : we will accept profile starting with http and https both. See: EA-3141
     * @param profile
     * @return
     */
    private static String getVersionFromProfile(String profile){
        Matcher matcher = ACCEPT_MEDIA_TYPE_PATTERN.matcher(profile);
        while (matcher.find()) { // found valid version in the profile url in group 3
            String version = matcher.group(3);
            if (version.equals(REQUEST_VERSION_3)) {
                return REQUEST_VERSION_3;
            } else if (version.equals(REQUEST_VERSION_2)) {
                return REQUEST_VERSION_2;
            }
        }
        return null; // in case profile is invalid
    }

    /**
     * Adds the appropriate Content-Type response headers
     * @param headers the HttpHeaders to which the Content-Type header is added
     * @param version IIIF version, either 2 or 3
     * @param isJson if true generated headr will be for json, otherwise fo json-ld
     */
    public static void addContentTypeToResponseHeader(HttpHeaders headers, String version, boolean isJson) {
        if ("3".equalsIgnoreCase(version)) {
            if (isJson) {
                headers.add(CONTENT_TYPE, IIIFDefinitions.MEDIA_TYPE_IIIF_JSON_V3);
            } else {
                headers.add(CONTENT_TYPE, IIIFDefinitions.MEDIA_TYPE_IIIF_JSONLD_V3);
            }
        } else {
            if (isJson) {
                headers.add(CONTENT_TYPE, IIIFDefinitions.MEDIA_TYPE_IIIF_JSON_V2);
            } else {
                headers.add(CONTENT_TYPE, IIIFDefinitions.MEDIA_TYPE_IIIF_JSONLD_V2);
            }
        }
    }
}
