package eu.europeana.fulltext.api.caching;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * @author luthien on 16/10/2018 (copied from IIIF Api's CacheUtils utility class to facilitate handling
 * If-Modified-Since, If-None-Match and If-Match request caching)
 * changes by @author Patrick Ehlert
 */
public class CachingUtils implements CachingHeaders {

    private static final Logger LOG = LogManager.getLogger(CachingUtils.class);
    private static final String ANY = "*";
    private static final String ALLOWED = "GET, HEAD";
    private static final String ALLOWHEADERS = "If-Match, If-None-Match, If-Modified-Since";
    private static final String EXPOSEHEADERS = "Allow, ETag, Last-Modified, Vary";
    private static final String CACHE_CONTROL_VALUE = "public, max-age=";
    private static final String DEFAULT_MAX_AGE = "86400"; // 1 day
    private static final String ACCEPT = "Accept";
    private static final String MAX_AGE_600 = "600";

    private CachingUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Method will check caching headers are present
     *
     * @param request
     * @return
     */
    public static boolean cachingHeadersPresent(HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getHeader(IF_MODIFIED_SINCE)) ||
                StringUtils.isNotBlank(request.getHeader(IF_NONE_MATCH)) ||
                StringUtils.isNotBlank(request.getHeader(IF_MATCH));
    }


    /**
     * Generates an eTag surrounded with double quotes -
     * base64("<LAST_MODIFIED>|<IIIF_VERSION>|<API_VERSION>")
     *
     * @param modified modified ZonedDateTime contained within MongoDB document
     * @param version  concatenated: requested IIIF version [2|3] + version of this API as defined in the pom.xml
     * @param weakETag if True, then the eTag will start with W/
     * @return eTag      generated eTag (String)
     */
    public static String generateETag(ZonedDateTime modified, String version, boolean weakETag) {
        String data = zonedDateTimeToString(modified) + version;
        String eTag = "\"" + getSHA256Hash(data) + "\"";
        if (weakETag) {
            return "W/" + eTag;
        }
        return eTag;
    }


    /**
     * Formats the given date according to the RFC 1123 pattern (e.g. Thu, 4 Oct 2018 10:34:20 GMT)
     *
     * @param zonedDateTime input ZonedDateTime to convert to RFC 1123 pattern
     * @return String containing RFC 1123 formatted ZonedDateTime
     */
    public static String zonedDateTimeToString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * Transforms a java.util.Date object to a ZonedDateTime object
     *
     * @param date input java.util.Date object to be converted to ZonedDateTime format for time zone UTC
     * @return ZonedDateTime converted from input Date object
     */
    public static ZonedDateTime dateToZonedUTC(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC).toZonedDateTime().withNano(0);
    }

    /**
     * returns a ZonedDateTime initiated to Jan 11, 1990
     *
     * @return ZonedDateTime
     */
    public static ZonedDateTime januarificator() {
        return ZonedDateTime.of(1990, 1, 11, 0, 0,
                0, 0, ZoneId.of("UTC"));
    }

    /**
     * Should be RFC-7232 compliant, incl. ability to process multiple eTags for an If-*-Match header
     *
     * @param request  incoming HttpServletRequest
     * @param modified ZonedDateTime that indicates the lastModified date of the requested data
     * @param eTag     String with the calculated eTag of the requested data
     * @return ResponseEntity with 304 or 312 status if requested object has not changed, otherwise null
     */
    public static ResponseEntity<String> checkCached(HttpServletRequest request, ZonedDateTime modified, String eTag) {
        HttpHeaders headers;
        // If “If-None-Match” is supplied (as defined in RFC7232), check if at least one of the “ETag”
        // values within the header is the same as the current “ETag” of the resource OR if it is “*”
        if (StringUtils.isNotBlank(request.getHeader(IF_NONE_MATCH))) {
            if (doesAnyETagMatch(request.getHeader(IF_NONE_MATCH), eTag)) {
                //if the request is a “GET” or “HEAD”, respond with HTTP 304 including the cache related header fields (current “ETag”);
                if (isReadAccessMethod(request)) {
                    headers = generateHeaders(request, eTag, zonedDateTimeToString(modified));
                    return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
                }
                //for the other request types, respond with a HTTP 412
                return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            }

            // If “If-Match” is supplied, check if at least one of the “ETag” values is the same
            // as the current “ETag” of the resource OR if it is “*”: if false, respond with HTTP 412;
        } else if (StringUtils.isNotBlank(request.getHeader(IF_MATCH))) {
            if (doesPreconditionFail(request, eTag)) {
                return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
            }
            //If “If-Modified-Since” is supplied:check if the date is earlier than the current
            // modification date of the resource or the release date of the API; if false respond with HTTP 304
        } else if (isNotModifiedSince(request, modified)) {
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
        return null;
    }

    private static boolean isReadAccessMethod(HttpServletRequest request) {
        return StringUtils.equalsAny(request.getMethod(), HttpMethod.GET.name(), HttpMethod.HEAD.name());
    }

    /**
     * Generate the default headers for sending a response with caching
     *
     * @param request  required to determine whether the 'Origin' request header is set
     * @param eTag     optional, if not null then an ETag header is added
     * @param modified optional, if not null then a Last-Modified header is added
     * @return HttpServletResponse
     */
    public static HttpHeaders generateHeaders(HttpServletRequest request, String eTag, String modified) {
        return generateHeaders(request, eTag, modified, null);
    }

    /**
     * Generate the default headers for sending a response with caching
     *
     * @param request  required to determine whether the 'Origin' request header is set
     * @param eTag     optional, if not null then an ETag header is added
     * @param modified optional, if not null then a Last-Modified header is added
     * @param maxAge   optional, if null DEFAULT_MAX_AGE set in the Cache-Control header
     * @return HttpServletResponse
     */
    public static HttpHeaders generateHeaders(HttpServletRequest request, String eTag, String modified, Integer maxAge) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotBlank(request.getHeader("Origin"))) {
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWHEADERS);
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSEHEADERS);
        }
        if (StringUtils.isNotBlank(eTag)) {
            headers.add(ETAG, eTag);
        }
        if (StringUtils.isNotBlank(modified)) {
            headers.add(LAST_MODIFIED, modified);
        }
        headers.add("Access-Control-Max-Age", MAX_AGE_600);
        headers.add("Allow", ALLOWED);
        headers.add(CACHE_CONTROL, CACHE_CONTROL_VALUE + (maxAge == null ? DEFAULT_MAX_AGE : maxAge));
        headers.add("Vary", ACCEPT);
        return headers;
    }

    /**
     * Supports multiple values in the "If-None-Match" header
     *
     * @param request incoming HttpServletRequest
     * @param eTag    String with the calculated eTag of the requested data
     * @return boolean true IF ( If-None-Match header is supplied AND
     * ( contains matching eTag OR == "*" ) )
     * Otherwise false
     */
    private static boolean doesAnyIfNoneMatch(HttpServletRequest request, String eTag) {
        return (StringUtils.isNotBlank(request.getHeader(IF_NONE_MATCH)) &&
                (doesAnyETagMatch(request.getHeader(IF_NONE_MATCH), eTag)));
    }

    /**
     * @param request  incoming HttpServletRequest
     * @param modified ZonedDateTime representing the FullBean's timestamp_updated
     * @return boolean true IF If-Modified-Since header is supplied AND
     * is after or on the timestamp_updated
     * Otherwise false
     */
    private static boolean isNotModifiedSince(HttpServletRequest request, ZonedDateTime modified) {
        return (StringUtils.isNotBlank(request.getHeader(IF_MODIFIED_SINCE)) &&
                Objects.requireNonNull(stringToZonedUTC(request.getHeader(IF_MODIFIED_SINCE)))
                        .compareTo(modified) >= 0);
    }

    /**
     * Supports multiple values in the "If-Match" header
     *
     * @param request incoming HttpServletRequest
     * @param eTag    String with the calculated eTag of the requested data
     * @return boolean true IF ("If-Match" header is supplied AND
     * NOT (contains matching eTag OR == "*") )
     * otherwise false
     */
    private static boolean doesPreconditionFail(HttpServletRequest request, String eTag) {
        return (StringUtils.isNotBlank(request.getHeader(IF_MATCH)) &&
                (!doesAnyETagMatch(request.getHeader(IF_MATCH), eTag)));
    }

    /**
     * @param eTags       etags from the headers
     * @param eTagToMatch etag to match
     * @return
     */
    private static boolean doesAnyETagMatch(String eTags, String eTagToMatch) {
        if (StringUtils.equals(ANY, eTags)) {
            return true;
        }
        if (StringUtils.isNoneBlank(eTags, eTagToMatch)) {
            for (String eTag : StringUtils.stripAll(StringUtils.split(eTags, ","))) {
                if (StringUtils.equalsIgnoreCase(spicAndSpan(eTag), spicAndSpan(eTagToMatch))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String spicAndSpan(String header) {
        return StringUtils.remove(StringUtils.stripStart(header, "W/"), "\"");
    }

    /**
     * Calculates SHA256 hash of a particular data string
     *
     * @param data String of data on which the hash is based
     * @return SHA256Hash   String
     */
    private static String getSHA256Hash(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(CachingUtils.class).error("Error generating SHA-265 hash from record timestamp_update", e);
        }
        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte bt : hash) {
            String hex = Integer.toHexString(0xff & bt);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Parses the given string into a ZonedDateTime object
     *
     * @param dateString String to be converted to ZonedDateTime object
     * @return ZonedDateTime
     */
    private static ZonedDateTime stringToZonedUTC(String dateString) {
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        // Note that Apache DateUtils can parse all 3 date format patterns allowed by RFC 2616
        Date date = DateUtils.parseDate(dateString);
        if (date == null) {
            LOG.error("Error parsing request header Date string: {}", dateString);
            return null;
        }
        return dateToZonedUTC(date);
    }

    public static void updateCorsExposeHeader(HttpServletRequest request, HttpHeaders headers) {
        if (StringUtils.isNotBlank(request.getHeader(HttpHeaders.ORIGIN))) {
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.VARY);
        }
    }
}
