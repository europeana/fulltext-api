package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.exception.RecordDoesNotExistException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private FTService fulltextRepo;

    FTSearchService(SolrNewspaperRepo solrRepo, FTService fulltextService){
        this.solrRepo = solrRepo;
        this.fulltextRepo = fulltextService;
    }

    /**
     * Searches fulltext for one particular newspaper issue (CHO)
     * @param searchId string that is set as id of the search (endpoint, path and query parameters)
     * @param europeanaId europeana id of the issue to search
     * @param query the string to search
     * @throws FTException when there is a problem processing the request (e.g. issue doesn't exist)
     * @return SearchResult object (can be empty if no hits were found)
     */
    public SearchResult searchIssue(String searchId, EuropeanaId europeanaId, String query, String snippet) throws FTException {
        // TODO Figure out why it only works when I specify a PageRequest
        HighlightPage<SolrNewspaper> solrResult = solrRepo.findByEuropeanaId(europeanaId, query, new PageRequest(0,1));

        SearchResult result = new SearchResult(searchId);
        if (solrResult.isEmpty()) {
            LOG.debug("Solr return empty result");
            // check if there are 0 hits because the record doesn't exist
            if (!fulltextRepo.doesAnnoPageExist(europeanaId.getDatasetId(), europeanaId.getLocalId(), "1")) {
                throw new RecordDoesNotExistException(europeanaId);
            }
        } else {
            LOG.debug("Solr returned {} document", solrResult.getSize());

            // we have 2 ways of generation Hits and finding corresponding Annotations
            // 1. Use (entire) Solr snippet
            if ("solr".equalsIgnoreCase(snippet)) {
                matchSnippetFromSolr(result, solrResult, europeanaId);
            } else {
                // 2. Use only Solr exact string to find occurances in Mongo and use Mongo to generate snippet
                matchExactInMongo(result, solrResult, europeanaId);
            }
        }
        return result;
    }

    /**
     * We use the entire snippet returned by Solr as the basis and try to find those in Mongo.
     */
    private SearchResult matchSnippetFromSolr(SearchResult result, HighlightPage<SolrNewspaper> solrResult,
                                              EuropeanaId europeanaId) {
        // Since we use the solr snippet as the basis, we can already create a list of hit selectors
        // We use those hitSelectors to find the same occurance in mongo
        List<HitSelector> hitsToFind = createHitSelectorsFromSolr(solrResult);

        // TODO tmp hack to get things working: for now we have to search through all annopages to get the right fulltext
        long start = System.currentTimeMillis();
        List<AnnoPage> pages = fulltextRepo.fetchAnnoPages(europeanaId.getDatasetId(), europeanaId.getLocalId());
        LOG.debug("Found {} annopages in {} ms", pages.size(), System.currentTimeMillis() - start);
        for (AnnoPage page : pages) {
            // check each page if we can find our hitSelector there
            List<HitSelector> hitsFound = new ArrayList<>();
            for (HitSelector hitToFind : hitsToFind) {
                Hit hitFound = findFullSnippetFromSolr(hitToFind, page);
                if (hitFound != null) {
                    findAnnotation(result, hitFound, page);
                    hitsFound.add(hitToFind);
                }
            }
            // when the hitSelector is found, we remove it from our list of hitSelectors we want to find.
            hitsToFind.removeAll(hitsFound);
            if (hitsToFind.isEmpty()) {
                break;
            }
        }
        if (!hitsToFind.isEmpty()) {
            LOG.warn("Failed to find {} Solr snippets {}", hitsToFind.size(), hitsToFind);
        }
        return result;
    }

    /**
     * We use all exact strings found by Solr as keywords to do a search in fulltext. We ignore the rest of the Solr snippet
     * and generate our own snippet based on data from Mongo
     */
    private SearchResult matchExactInMongo(SearchResult result, HighlightPage<SolrNewspaper> solrResult,
                                           EuropeanaId europeanaId) {
        // Create a list of all different keywords found in Solr.
        Set<String> hitsToFind = createKeywordListForMongo(solrResult);

        // TODO tmp hack to get things working: for now we have to search through all annopages to get the right fulltext
        long start = System.currentTimeMillis();
        List<AnnoPage> pages = fulltextRepo.fetchAnnoPages(europeanaId.getDatasetId(), europeanaId.getLocalId());
        LOG.debug("Found {} annopages in {} ms", pages.size(), System.currentTimeMillis() - start);
        for (AnnoPage page : pages) {
            // check each page if we can find our keywords
            LOG.trace("Searching through page {}, fulltext = {}", page.getPgId(), page.getRes().getId());
            for (String hitToFind : hitsToFind) {
                List<Hit> hitsFound = findExactStringInMongo(hitToFind, page);
                for (Hit hit : hitsFound) {
                    findAnnotation(result, hit, page);
                }
            }
        }
        return result;
    }

    /**
     * Create HitSelectors based on full Solr snippet
     */
    private List<HitSelector> createHitSelectorsFromSolr(HighlightPage<SolrNewspaper> solrResult) {
        List<HitSelector> result = new ArrayList<>();
        for (HighlightEntry<SolrNewspaper> content : solrResult.getHighlighted()) {
            LOG.debug("Record = {}", content.getEntity().europeanaId);
            for (HighlightEntry.Highlight highlight : content.getHighlights()) {
                for (String snipplet : highlight.getSnipplets()) {
                    HitSelector hitSelector = createHitSelectorFromSolr(snipplet);
                    LOG.debug("  Keyword = '{}', Solr snippet = {}", hitSelector.getExact(), snipplet);
                    result.add(hitSelector);
                }
            }
        }
        return result;
    }

    /**
     * Get the word(s) found by Solr which are surrounded by HIT_START_TAG and HIT_END_TAG
     */
    private HitSelector createHitSelectorFromSolr(String snipplet) {
        String prefix = StringUtils.substringBefore(snipplet, FTSearchDefinitions.HIT_TAG_START);
        String exact = StringUtils.substringBefore(StringUtils.substringAfter(snipplet, FTSearchDefinitions.HIT_TAG_START), FTSearchDefinitions.HIT_TAG_END);
        String suffix = StringUtils.substringAfter(snipplet, FTSearchDefinitions.HIT_TAG_END);
        // TODO check if suffix contains another hit (support multiple keywords?)
        return new HitSelector(prefix, exact, suffix);
    }

    /**
     * Create HitSelectors based on full Solr snippet so we can use those to search in Mongo fulltexts
     */
    private Set<String> createKeywordListForMongo(HighlightPage<SolrNewspaper> solrResult) {
        Set<String> result = new HashSet<>();
        for (HighlightEntry<SolrNewspaper> content : solrResult.getHighlighted()) {
            LOG.debug("Record = {}", content.getEntity().europeanaId);
            for (HighlightEntry.Highlight highlight : content.getHighlights()) {
                for (String snipplet : highlight.getSnipplets()) {
                    String exact = StringUtils.substringBefore(
                            StringUtils.substringAfter(snipplet, FTSearchDefinitions.HIT_TAG_START), FTSearchDefinitions.HIT_TAG_END);
                    LOG.debug("  Keyword = '{}', Solr snippet = {}", exact, snipplet);
                    result.add(exact);
                }
            }
        }
        return result;
    }

    /**
     * We check if we can find a Solr hit (using entire snippet) in a particular AnnoPage / Resource.
     * If found we calculate the start and end index in the text and create a Hit object
     * @return Hit object if there is a match, otherwise null;
     */
    private Hit findFullSnippetFromSolr(HitSelector hitToFind, AnnoPage annoPage) {
        LOG.trace("Searching on page {} for Solr snippet {}:", annoPage, hitToFind);
        int startIndex = annoPage.getRes().getValue().indexOf(hitToFind.toString());
        if (startIndex >= 0) {
            // we found the correct AnnoPage! set startIndex and endIndex according to 'exact' string
            startIndex = startIndex + hitToFind.getPrefix().length();
            int endIndex = startIndex + hitToFind.getExact().length();
            LOG.debug("Found Solr snippet {} in fulltext {}, hit start = {}, end = {}",
                    hitToFind, annoPage.getRes().getId(), startIndex, endIndex);
            return new Hit(startIndex, endIndex, hitToFind);
        }
        return null;
    }

    /**
     * We check if we can find one or more of the hits (exact string) in a particular AnnoPage / Resource.
     * If so we calculate the start and end index in the text and add it to result list
     * @return a list of hits (can be empty if there are not matches)
     */
    private List<Hit> findExactStringInMongo(String hitToFind, AnnoPage annoPage) {
        LOG.trace("Searching on page {} for Solr exact string {}", annoPage, hitToFind);
        List<Hit> result = new ArrayList<>();

        // TODO OPEN QUESTION; I was searching for Amsterdam and Solr only found that keyword. So I went through all
        //  the pages and found 'Amsterdamsche'. In this case this case it's a nice extra catch, but what if we search
        //  for word 'ama' for example, we could end up returning words such as 'amadeus', 'pyama', 'llama', etc.
        // ALSO what about case-sensitivity?

        String resourceTxt = annoPage.getRes().getValue();
        int startIndex = resourceTxt.indexOf(hitToFind);
        while (startIndex >= 0) {
            int endIndex = startIndex + hitToFind.length();
            LOG.debug("Found exact string {} on page {}, fulltext {}, hit start = {}, end = {}",
                    hitToFind, annoPage.getPgId(), annoPage.getRes().getId(), startIndex, endIndex);
            result.add(new Hit(startIndex, endIndex, generateSnippetFromMongo(hitToFind, resourceTxt, startIndex, endIndex)));

            startIndex = resourceTxt.indexOf(hitToFind, startIndex + 1);
        }
        return result;
    }

    /**
     * We have found our hit, now we need to generate a snippet based on the Mongo fulltext
     * The strategy is to find the closest dot (start and end of sentence). If we can't find that we search for the
     * closest newline as fallback solution.
     */
    protected HitSelector generateSnippetFromMongo(String hitToFind, String fullText, int startIndex, int endIndex) {
        int startSentence;
        int endPreviousSentence = fullText.lastIndexOf('.', startIndex);
        if (endPreviousSentence == -1) {
            // not dot found, search for newline as fallback
            endPreviousSentence = fullText.lastIndexOf('\n', startIndex);
            if (endPreviousSentence == -1) {
                startSentence = 0; // no dot or newline found.
            } else {
                startSentence = endPreviousSentence + 1;
            }
        } else {
            startSentence = endPreviousSentence + 1;
        }
        // find first non-space character (trim leading spaces)
        while (fullText.charAt(startSentence) == ' ') {
            startSentence++;
        }

        String prefix = fullText.substring(startSentence, startIndex);

        int endSentence = fullText.indexOf('.', endIndex);
        if (endSentence == -1) {
            // not dot found, search for newline as fallback
            endSentence = fullText.indexOf('\n', endIndex);
            if (endSentence == -1) {
                endSentence = fullText.length(); // no dot or newline found.
            }
        } else {
            endSentence++; // include the dot
        }
        String suffix = fullText.substring(endIndex, endSentence);

        return new HitSelector(prefix, hitToFind, suffix);
    }

    /**
     * Given a hit, we go over all Annotations to see which one(s) match. When we found one we add it to the search result
     */
    private void findAnnotation(SearchResult result, Hit hit, AnnoPage annoPage) {
        boolean annotationFound = false;
        for (Annotation anno : annoPage.getAns()) {
            if (anno.getDcType() == ANNOTATION_HIT_MATCH_LEVEL &&
                    overlap(hit.getStartIndex(), hit.getEndIndex(), anno.getFrom(), anno.getTo())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found overlap between hit {},{} ({}) and annotation {},{} ({})",
                            hit.getStartIndex(), hit.getEndIndex(),
                            annoPage.getRes().getValue().substring(hit.getStartIndex(), hit.getEndIndex()),
                            anno.getFrom(), anno.getTo(),
                            annoPage.getRes().getValue().substring(anno.getFrom(), anno.getTo()));
                }
                // sometimes a trailing character like a dot or comma directly after the keyword is regarded as
                // another annotation (word). So we filter those out.
//                    if (anno.getTo() - anno.getFrom() > 1) {
                result.addAnnotationHit(annoPage, anno, hit);
                annotationFound = true;
                // TODO think about whether we want to break when we find our first hit, or continue searching?

//                    } else {
//                        LOG.debug("Ignoring this overlap because it's too short");
//                    }
            }
        }
        if (!annotationFound) {
            LOG.warn("Could not find any annotation for hit {} on page {}{}", hit, annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId());
        }
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
