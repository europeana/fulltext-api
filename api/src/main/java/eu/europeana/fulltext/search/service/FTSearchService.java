package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.model.FTSearchDefinitions;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitSelector;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.repository.SolrNewspaperRepo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.europeana.fulltext.api.config.FTDefinitions.ANNO_TYPE_WORD;

// TODO really make loading this optional, currently Spring-Boot will auto-connect to Solr for the actuator

/**
 * Service for querying solr, retrieving fulltext data and sending back results
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
//@Lazy
@Service
public class FTSearchService {

    private static final Logger LOG  = LogManager.getLogger(FTSearchService.class);

    private static final char ANNOTATION_HIT_MATCH_LEVEL = ANNO_TYPE_WORD; // only check word-level annotations to find matches

    private SolrNewspaperRepo solrRepo;
    private FTService fulltextService;

    FTSearchService(SolrNewspaperRepo solrRepo, FTService fulltextService){
        this.solrRepo = solrRepo;
        this.fulltextService = fulltextService;
    }

    /**
     * Searches fulltext for one particular newspaper issue (CHO)
     * @param searchId string that is set as id of the search (endpoint, path and query parameters)
     * @param europeanaId europeana id of the issue to search
     * @param query the string to search
     * @return SearchResult object (can be empty if no hits were found)
     */
    public SearchResult searchIssue(String searchId, EuropeanaId europeanaId, String query) {
        // TODO Figure out why it only works when I specify a PageRequest
        HighlightPage<SolrNewspaper> solrResult = solrRepo.findByEuropeanaId(europeanaId, query, new PageRequest(0,1));

        SearchResult result = new SearchResult(searchId);
        if (solrResult.isEmpty()) {
            LOG.debug("Solr return empty result");
        } else {
            LOG.debug("Solr returned {} document", solrResult.getSize());
            List<HitSelector> hitsToFind = getHitSelectors(solrResult);

            // TODO tmp hack to get things working: for now we have to search through all annopages to get the right fulltext
            // maybe second best option is to get this from Mongo
            List<AnnoPage> pages = fulltextService.fetchAnnoPages(europeanaId.getDatasetId(), europeanaId.getLocalId());
            LOG.debug("Found {} annopages", pages.size());

            for (AnnoPage page : pages) {
                // check each page if we can find our hits there
                List<HitSelector> hitsFound = new ArrayList<>();
                LOG.debug("Page {}, fulltext = {}", page.getPgId(), page.getRes().getValue());
                for (HitSelector hitToFind : hitsToFind) {
                    HitSelector hitFound = checkMatch(result, hitToFind, page);
                    if (hitFound != null) {
                        hitsFound.add(hitFound);
                    }
                }
                // check how far along we are (how many hits left to find)
                hitsToFind.removeAll(hitsFound);
                if (hitsToFind.isEmpty()) {
                    break;
                }
            }
            if (!hitsToFind.isEmpty()) {
                LOG.warn("Failed to find {} hits {}", hitsToFind.size(), hitsToFind);
            }

        }


        return result;

    }

    /**
     * Create a list of all hits found by Solr
     */
    private List<HitSelector> getHitSelectors(HighlightPage<SolrNewspaper> solrResult) {
        List<HitSelector> result = new ArrayList<>();
        for (HighlightEntry<SolrNewspaper> content : solrResult.getHighlighted()) {
            LOG.debug("Record = {}", content.getEntity().europeanaId);
            for (HighlightEntry.Highlight highlight : content.getHighlights()) {
                for (String snipplet : highlight.getSnipplets()) {
                    HitSelector hitSelector = getHitFromLine(snipplet);
                    LOG.debug("  Found '{}' in field {}, snippet: {}", hitSelector.getExact(), highlight.getField(), snipplet);
                    result.add(hitSelector);
                }
            }
        }
        return result;
    }

    /**
     * We check if we can find a hit in a particular AnnoPage / Resource. If so we calculate the start and end index
     * in the text and see if we can find a corresponding annotation.
     * @return the hitSelector if there is a match, otherwise null
     */
    private HitSelector checkMatch(SearchResult result, HitSelector hitToFind, AnnoPage annoPage) {
        LOG.debug("Checking page {}, hit {}", annoPage.getPgId(), hitToFind.toString());
        int startIndex = annoPage.getRes().getValue().indexOf(hitToFind.toString());
        if (startIndex >= 0) {
            // we found the correct AnnoPage!
            // set startIndex and endIndex according to 'exact' string
            startIndex = startIndex + hitToFind.getPrefix().length();
            int endIndex = startIndex + hitToFind.getExact().length();
            LOG.debug("Found {} in fulltext {}, start = {}, end = {}",
                    hitToFind.getExact(), annoPage.getRes().getId(), startIndex, endIndex);
            Hit hit = new Hit(startIndex, endIndex, hitToFind);

            // now find the correct annotation to match to the hit, and add to search results
            for (Annotation anno : annoPage.getAns()) {
                if (anno.getDcType() == ANNOTATION_HIT_MATCH_LEVEL &&
                        overlap(hit.getStartIndex(), hit.getEndIndex(), anno.getFrom(), anno.getTo())) {
                    LOG.debug("Found overlap between {},{} and {},{}", hit.getStartIndex(), hit.getEndIndex(),
                            anno.getFrom(), anno.getTo());
                    result.addAnnotationHit(annoPage, anno, hit);
                }
            }
            return hitToFind;
        }
        return null;
    }

    /**
     * Checks if there is an overlap between 2 start and end indexes.
     * Implementation based on https://stackoverflow.com/a/36035369
     * @param s1 start index1
     * @param e1 end index1
     * @param s2 start index2
     * @param e2 end index2
     * @return true if there is overlap, otherwise false
     */
    private boolean overlap(int s1, int e1, int s2, int e2) {
        return (s1 <= e2 && e1 >= s2);
    }

    /**
     * Get the word(s) found by Solr which are surrounded by HIT_START_TAG and HIT_END_TAG

     */
    private HitSelector getHitFromLine(String snipplet) {
        // TODO optimize code below
        String prefix = StringUtils.substringBefore(snipplet, FTSearchDefinitions.HIT_TAG_START);
        String exact = StringUtils.substringBefore(StringUtils.substringAfter(snipplet, FTSearchDefinitions.HIT_TAG_START), FTSearchDefinitions.HIT_TAG_END);
        String suffix = StringUtils.substringAfter(snipplet, FTSearchDefinitions.HIT_TAG_END);
        // TODO check if suffix contains another hit (support multiple keywords?)
        return new HitSelector(prefix, exact, suffix);
    }


    public SearchResult searchCollection(String searchId, String query, int page, int pageSize) {
        HighlightPage<SolrNewspaper> result = solrRepo.findByFulltextIn(query, new PageRequest(page, pageSize));
        LOG.error("result = {}", result);

        for (HighlightEntry<SolrNewspaper> record : result.getHighlighted()) {
            LOG.error("  Found record {}, with language {} and fulltext {}", record.getEntity().europeanaId,
                    record.getEntity().language, record.getEntity().fulltext);
        }

        return null;
    }

}
