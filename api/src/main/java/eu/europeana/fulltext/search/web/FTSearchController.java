package eu.europeana.fulltext.search.web;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.service.FTSearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static eu.europeana.fulltext.RequestUtils.ACCEPT_VERSION_INVALID;
import static eu.europeana.fulltext.RequestUtils.getRequestVersion;

/**
 * Rest controller that handles search requests
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
//@Lazy
@RestController
@RequestMapping("/presentation")
public class FTSearchController {

    private static final Logger LOG = LogManager.getLogger(FTSearchController.class);

    private FTSearchService searchService;
    private FTSettings settings;

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
     * @param qf
     * @param pageSize        maximum number of hits
     * @param textGranularity one-letter abbreviation or name of an Annotation type
     * @param page
     * @param lang
     * @param debug           if specified then include debug information in the response
     * @throws FTException when there is an error processing the request
     */
    @GetMapping(value = "/{datasetId}/{localId}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity searchIssue(@PathVariable String datasetId, @PathVariable String localId,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String[] qf,
                                    @RequestParam(required = false, defaultValue = "0") int page,
                                    @RequestParam(required = false, defaultValue = "12") int pageSize,
                                    @RequestParam(required = false) String textGranularity,
                                    @RequestParam(required = false) String lang,
                                    @RequestParam(value = "format", required = false) String versionParam,
                                    @RequestParam(required = false) String debug,
                                    HttpServletRequest request) throws FTException {

        String requestVersion = getRequestVersion(request, versionParam);
        if (ACCEPT_VERSION_INVALID.equals(requestVersion)){
            return new ResponseEntity<>(ACCEPT_VERSION_INVALID, HttpStatus.NOT_ACCEPTABLE);
        }

        // validate input
        String qry = validateQuery(query, q);
        if (pageSize < 1 || pageSize > SearchConfig.MAXIMUM_HITS) {
            throw new InvalidParameterException("Page size should be between 1 and " + SearchConfig.MAXIMUM_HITS);
        }
        AnnotationType annoType = validateAnnoType(textGranularity);

        // start processing
        String searchId = request.getRequestURI() + "?" + request.getQueryString();
        SearchResult searchResult = searchService.searchIssue(searchId, new EuropeanaId(datasetId, localId), qry,
                pageSize, annoType, requestVersion, (debug != null));
        return new ResponseEntity<>(searchResult, HttpStatus.OK);
    }

    private String validateQuery(String query, String q) throws FTException {
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
     * For now we only support Block, Line and Word level annotations
     */
    private AnnotationType validateAnnoType(String textGranularity) throws InvalidParameterException {
        // if the user didn't provide a parameter, we use the configured default
        if (textGranularity == null) {
            return settings.getDefaultSearchTextGranularity();
        }

        AnnotationType result = AnnotationType.fromAbbreviationOrName(textGranularity);
        if (AnnotationType.WORD.equals(result) || AnnotationType.LINE.equals(result) || AnnotationType.BLOCK.equals(result)) {
            return result;
        }
        throw new InvalidParameterException(("Invalid text granularity value. Possible values are: Word, Line or Block"));
    }

}
