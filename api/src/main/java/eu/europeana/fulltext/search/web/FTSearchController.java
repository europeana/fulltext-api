package eu.europeana.fulltext.search.web;

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
import org.springframework.web.bind.annotation.*;

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
                                    @RequestParam (required = false) String lang,
                                    @RequestParam (required = false) String debug,
                                    HttpServletRequest request) throws FTException {
        // validate input
        String qry = validateQuery(query, q);
        if (pageSize < 1 || pageSize > SearchConfig.MAXIMUM_HITS) {
            throw new InvalidParameterException("Page size should be between 1 and " + SearchConfig.MAXIMUM_HITS);
        }

        // start processing
        String searchId = request.getRequestURI() + "?" + request.getQueryString();
        return searchService.searchIssue(searchId, new EuropeanaId(datasetId, localId), qry, pageSize, (debug != null));
    }

//    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
//    public SearchResult searchEntireCollection(
//                                      @RequestParam (required = false) String query,
//                                      @RequestParam (required = false) String q,
//                                      @RequestParam (required = false) String[] qf,
//                                      @RequestParam (required = false, defaultValue = "0") int page,
//                                      @RequestParam (required = false, defaultValue = "12") int pageSize,
//                                      @RequestParam (required = false) String lang,
//                                      HttpServletRequest request) throws FTException {
//        // validate input
//        String qry = validateQuery(query, q);
//
//        // do query
//        String searchId = request.getRequestURI() + "?" + request.getQueryString();
//        return searchService.searchCollection(searchId, qry, page, pageSize);
//    }


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

}
