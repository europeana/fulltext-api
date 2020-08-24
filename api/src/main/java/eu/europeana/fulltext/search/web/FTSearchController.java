package eu.europeana.fulltext.search.web;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.RequestUtils;
import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.exception.InvalidParameterException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.service.FTSearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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

    private static final Logger LOG  = LogManager.getLogger(FTSearchController.class);

    private FTSearchService searchService;

    public FTSearchController(FTSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search the provided issue (CHO) for a particular string
     * @param datasetId datasetId of the issue to search
     * @param localId itemId of the issue to search
     * @param query search query
     * @param q alternative search query (will override query if specified both
     * @param qf
     * @param pageSize maximum number of hits
     * @param textGranularity one-letter abbreviation or name of an Annotation type
     * @param page
     * @param lang
     * @param debug if specified then include debug information in the response
     * @throws FTException when there is an error processing the request
     */
    @GetMapping(value = "/{datasetId}/{localId}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResult searchIssue(@PathVariable String datasetId, @PathVariable String localId,
                                    @RequestParam (required = false) String query,
                                    @RequestParam (required = false) String q,
                                    @RequestParam (required = false) String[] qf,
                                    @RequestParam (required = false, defaultValue = "0") int page,
                                    @RequestParam (required = false, defaultValue = "12") int pageSize,
                                    @RequestParam (required = false, defaultValue = "W") String textGranularity,
                                    @RequestParam (required = false) String lang,
                                    @RequestParam(value = "format", required = false) String versionParam,
                                    @RequestParam (required = false) String debug,
                                    HttpServletRequest request) throws FTException {
        // validate input
        String qry = validateQuery(query, q);
        if (pageSize < 1 || pageSize > SearchConfig.MAXIMUM_HITS) {
            throw new InvalidParameterException("Page size should be between 1 and " + SearchConfig.MAXIMUM_HITS);
        }
        AnnotationType annoType = validateAnnoType(textGranularity);
        String requestVersion = RequestUtils.getRequestVersion(request, versionParam);

        // start processing
        String searchId = request.getRequestURI() + "?" + request.getQueryString();
        return searchService.searchIssue(searchId, new EuropeanaId(datasetId, localId), qry, pageSize, annoType, (debug != null), requestVersion);
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
        AnnotationType result = AnnotationType.fromAbbreviationOrName(textGranularity);
        if (AnnotationType.WORD.equals(result) || AnnotationType.LINE.equals(result) || AnnotationType.BLOCK.equals(result)) {
            return result;
        }
        throw new InvalidParameterException(("Invalid text granularity value. Possible values are: Word, Line or Block"));
    }

}
