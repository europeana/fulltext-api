package eu.europeana.fulltext.search.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.ControllerUtils;
import eu.europeana.fulltext.api.service.exception.InvalidVersionException;
import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.exception.SearchDisabledException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.service.FTSearchService;
import eu.europeana.iiif.AcceptUtils;
import eu.europeana.iiif.IIIFDefinitions;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static eu.europeana.iiif.AcceptUtils.*;

/**
 * Rest controller that handles search requests
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@Api(tags = {"Full-text search"}, description = "Search all full-texts that are part of an item (e.g. newspaper issue)")
@RestController
@RequestMapping(IIIFDefinitions.PRESENTATION_PATH)
public class FTSearchController {

    public static final Set<AnnotationType> ALLOWED_ANNOTATION_TYPES = EnumSet.of(
            AnnotationType.BLOCK, AnnotationType.LINE, AnnotationType.WORD);

    private static final Logger LOG = LogManager.getLogger(FTSearchController.class);

    private final FTSearchService searchService;
    private final FTSettings settings;

    public FTSearchController(FTSearchService searchService, FTSettings settings) {
        this.searchService = searchService;
        this.settings = settings;
    }

    /**
     * Search the provided issue (CHO) for a particular string
     *
     * @param datasetId       datasetId of the issue to search
     * @param localId         itemId of the issue to search
     * @param query           search query
     * @param q               alternative search query (will override query if specified both
     * @param pageSize        maximum number of hits
     * @param textGranularity one-letter abbreviation or name of an Annotation type
     * @param debug           if specified then include debug information in the response
     * @throws EuropeanaApiException when there is an error processing the request
     */
    @GetMapping(value = "/{datasetId}/{localId}/search", headers = ACCEPT_JSON)
    public ResponseEntity searchIssueJson(@PathVariable String datasetId, @PathVariable String localId,
                                      @RequestParam(required = false) String query,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false, defaultValue = "12") int pageSize,
                                      @RequestParam(required = false) String textGranularity,
                                      @RequestParam(value = "format", required = false) String versionParam,
                                      @RequestParam(required = false) String debug,
                                      HttpServletRequest request) throws EuropeanaApiException {
        return serachIssue(datasetId, localId, query, q, pageSize, textGranularity, versionParam, debug, request, true);
    }

    /**
     * Search the provided issue (CHO) for a particular string
     *
     * @param datasetId       datasetId of the issue to search
     * @param localId         itemId of the issue to search
     * @param query           search query
     * @param q               alternative search query (will override query if specified both
     * @param pageSize        maximum number of hits
     * @param textGranularity one-letter abbreviation or name of an Annotation type
     * @param debug           if specified then include debug information in the response
     * @throws EuropeanaApiException when there is an error processing the request
     */
    @GetMapping(value = "/{datasetId}/{localId}/search", headers = ACCEPT_JSONLD)
    public ResponseEntity searchIssueJsonLd(@PathVariable String datasetId, @PathVariable String localId,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false, defaultValue = "12") int pageSize,
                                    @RequestParam(required = false) String textGranularity,
                                    @RequestParam(value = "format", required = false) String versionParam,
                                    @RequestParam(required = false) String debug,
                                    HttpServletRequest request) throws EuropeanaApiException {
        return serachIssue(datasetId, localId, query, q, pageSize, textGranularity, versionParam, debug, request, false);
    }

    private ResponseEntity serachIssue(String datasetId, String localId, String query, String q, int pageSize, String textGranularity,
                                       String versionParam, String debug, HttpServletRequest request, boolean isJson) throws EuropeanaApiException {
        // validate the format
        if(!settings.isSolrEnabled()){
            throw new SearchDisabledException();
        }

        String requestVersion = getRequestVersion(request, versionParam);
        if (StringUtils.isEmpty(requestVersion)) {
            throw new InvalidVersionException(ACCEPT_VERSION_INVALID);
        }

        // validate input
        String qry = validateQuery(query, q);
        if (pageSize < 1 || pageSize > SearchConfig.MAXIMUM_HITS) {
            throw new InvalidParameterException("Page size should be between 1 and " + SearchConfig.MAXIMUM_HITS);
        }
        List<AnnotationType> annoTypes = validateTextGranularity(textGranularity);

        // start processing
        String searchId = request.getRequestURI() + "?" + request.getQueryString();
        SearchResult searchResult = searchService.searchIssue(searchId, new EuropeanaId(datasetId, localId), qry,
                pageSize, annoTypes, requestVersion, (debug != null));

        HttpHeaders headers = new HttpHeaders();
        AcceptUtils.addContentTypeToResponseHeader(headers, requestVersion, isJson);
        return new ResponseEntity(searchResult, headers, HttpStatus.OK);
    }

    private String validateQuery(String query, String q) throws EuropeanaApiException {
        if (StringUtils.isEmpty(query) && StringUtils.isEmpty(q)) {
            throw new InvalidParameterException("No or empty query parameter");
        }
        if (!StringUtils.isEmpty(query)) {
            if (!StringUtils.isEmpty(q)) {
                LOG.warn("Both query and q parameters are specified. Ignoring the q parameter");
            }
            return query;
        }
        return q;
    }

    /**
     * Validate if there's a text granularity parameter value. If not we use the default settings, if so we
     * check if the values are valid.
     */
    List<AnnotationType> validateTextGranularity(String textGranularityParams) throws InvalidParameterException {
        // if the user didn't provide a parameter, we use the configured default
        if (textGranularityParams == null) {
            return settings.getDefaultSearchTextGranularity();
        }
        return ControllerUtils.validateTextGranularity(textGranularityParams, ALLOWED_ANNOTATION_TYPES);
    }

}
