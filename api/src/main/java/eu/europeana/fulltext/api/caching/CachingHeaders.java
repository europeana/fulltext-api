package eu.europeana.fulltext.api.caching;

/**
 * @author srishti singh
 * @since 13 December 2024
 */
public interface CachingHeaders {

    String IF_MATCH          = "If-Match";
    String IF_NONE_MATCH     = "If-None-Match";
    String IF_MODIFIED_SINCE = "If-Modified-Since";

    String CACHE_CONTROL     = "Cache-Control";
    String ETAG              = "ETag";
    String LAST_MODIFIED     = "Last-Modified";
}
