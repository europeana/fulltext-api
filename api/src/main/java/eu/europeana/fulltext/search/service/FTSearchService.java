package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.exception.RecordDoesNotExistException;
import eu.europeana.fulltext.search.model.FTSearchDefinitions;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrNewspaper;
import eu.europeana.fulltext.search.model.response.Debug;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitSelector;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.repository.SolrNewspaperRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

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
     * @param debug if true we include debug information
     * @throws FTException when there is a problem processing the request (e.g. issue doesn't exist)
     * @return SearchResult object (can be empty if no hits were found)
     */
    public SearchResult searchIssue(String searchId, EuropeanaId europeanaId, String query, boolean debug) throws FTException {
        // TODO Figure out why it only works when I specify a PageRequest
        long start = System.currentTimeMillis();
        HighlightPage<SolrNewspaper> solrResult = solrRepo.findByEuropeanaId(europeanaId, query, new PageRequest(0,1));

        SearchResult result = new SearchResult(searchId, debug);
        if (solrResult.isEmpty()) {
            LOG.debug("Solr return empty result");
            // check if there are 0 hits because the record doesn't exist
            if (!fulltextRepo.doesAnnoPageExist(europeanaId.getDatasetId(), europeanaId.getLocalId(), "1")) {
                throw new RecordDoesNotExistException(europeanaId);
            }
        } else {
            LOG.debug("Solr returned {} document", solrResult.getSize());
            findHitsInIssue(result, solrResult, europeanaId, result.getDebug());
        }
        LOG.debug("Search done in {} ms. Found {} annotations", (System.currentTimeMillis() - start), result.getItems().size());
        return result;
    }

    /**
     * We use all the strings found by Solr as keywords to do a search in a record/newspaper issue to find the correct
     * annopages and annotations.
     */
    private SearchResult findHitsInIssue(SearchResult result, HighlightPage<SolrNewspaper> solrResult,
                                         EuropeanaId europeanaId, Debug debug) {
        Collection<HitSelector> hitsToFind = getHitsFromSolrSnippets(solrResult, debug);

        // TODO tmp hack to get things working: for now we have to search through all annopages to get the right fulltext
        long start = System.currentTimeMillis();
        List<AnnoPage> pages = fulltextRepo.fetchAnnoPages(europeanaId.getDatasetId(), europeanaId.getLocalId());
        LOG.debug("Found {} annopages in {} ms", pages.size(), System.currentTimeMillis() - start);

        for (AnnoPage page : pages) {
            // check each page if we can find our keywords
            LOG.trace("Searching through page {}, fulltext = {}", page.getPgId(), page.getRes().getId());
            for (HitSelector hitToFind : hitsToFind) {
                List<Hit> hitsFound = findHitInFullText(hitToFind, page);
                for (Hit hit : hitsFound) {
                    findAnnotation(result, hit, page);
                }
            }
        }
        return result;
    }

    /**
     * Use the Solr snippets to create a set of unique hits. We use those to search in Mongo fulltexts ourselves,
     * to find the correct annopage(s) and annotations
     */
    private Collection<HitSelector> getHitsFromSolrSnippets(HighlightPage<SolrNewspaper> solrResult, Debug debug) {
        // we use a hashmap where the string is the hit in String representation, so we can quickly see if we already
        // a similar hit or not
        Map<String, HitSelector> hits = new HashMap<>();
        for (HighlightEntry<SolrNewspaper> content : solrResult.getHighlighted()) {
            LOG.debug("Record = {}", content.getEntity().europeanaId);

            for (HighlightEntry.Highlight highlight : content.getHighlights()) {
                for (String snippet : highlight.getSnipplets()) {
                    addHitsFromSolrSnippet(hits, snippet, debug);
                }
            }
        }
        LOG.debug("Found {} distinct keywords: {} ", hits.keySet().size(), hits.keySet());
        return hits.values();
    }

    /**
     * Extracts the keyword(s) from the solr snippet. Note that we 'misuse' the HitSelector object to also get 1
     * leading and 1 trailing character. We do this so we can search using word delimiters
     */
    // TODO write unit test
    protected Map<String, HitSelector> addHitsFromSolrSnippet(Map<String, HitSelector> hitsFound, String snippet, Debug debug) {
        LOG.debug("  Processing snippet {}", snippet);
        if (debug != null) {
            debug.addSolrSnippet(snippet);
        }

        int start = snippet.indexOf(FTSearchDefinitions.HIT_TAG_START);
        while (start != -1) {
            int end = snippet.indexOf(FTSearchDefinitions.HIT_TAG_END, start);
            String exact = snippet.substring(start + FTSearchDefinitions.HIT_TAG_START.length(), end);
            // append character in front (if available)
            Character prefix = null;
            if (start > 0) {
                prefix = snippet.charAt(start - 1);
            }
            // append character after (if available)
            Character suffix = null;
            end = end + FTSearchDefinitions.HIT_TAG_END.length();
            if (end < snippet.length()) {
                suffix = snippet.charAt(end);
            }
            HitSelector newHit = new HitSelector((prefix == null ? null : prefix.toString()), exact,
                                              (suffix == null ? null : suffix.toString()));

            if (hitsFound.containsKey(newHit.toString())) {
                LOG.debug("    Existing keyword = '{}'", newHit);
            } else {
                LOG.debug("    New keyword = '{}'", newHit);
                hitsFound.put(newHit.toString(), newHit);
                if (debug != null) {
                    debug.addKeywords(newHit);
                }
            }

            start = snippet.indexOf(FTSearchDefinitions.HIT_TAG_START, end);
        }
        return hitsFound;
    }

    /**
     * We check if we can find one or more of the keywords/hits in the resource (fulltext) of a particular AnnoPage
     * If so we calculate the start and end index in the text and add it to the result list
     * @return a list of hits (can be empty if there are no matches)
     */
    private List<Hit> findHitInFullText(HitSelector hitToFind, AnnoPage annoPage) {
        LOG.trace("Searching on page {} for Solr exact string '{}'", annoPage, hitToFind);
        List<Hit> result = new ArrayList<>();

        String resourceTxt = annoPage.getRes().getValue();
        int startIndex = getHitStartInFulltext(hitToFind, 0, resourceTxt);
        while (startIndex >= 0) {
            startIndex = startIndex + hitToFind.getPrefix().length();
            int endIndex = startIndex + hitToFind.getExact().length();
            LOG.debug("Found hit '{}' on page {}, fulltext {}, word start = {}, end = {}",
                    hitToFind, annoPage.getPgId(), annoPage.getRes().getId(), startIndex, endIndex);
            result.add(new Hit(startIndex, endIndex, generateSnippetFromFullText(hitToFind, resourceTxt, startIndex, endIndex)));

            startIndex = getHitStartInFulltext(hitToFind, startIndex + 1, resourceTxt);
        }
        return result;
    }

    private int getHitStartInFulltext(HitSelector hitToFind, int startIndex, String fulltext) {
        if (StringUtils.isEmpty(hitToFind.getPrefix())) {
            // hit could be at start of fulltext
            if (fulltext.startsWith(hitToFind.getExact() + hitToFind.getSuffix())) {
                return 0;
            }
            // or at start of a sentence (after a newline)
            int startSentence = fulltext.indexOf("\n" + hitToFind.toString(), startIndex);
            if (startSentence > 0) {
                return startSentence + 1;
            }
            // or at start of a sentence part (after a space)
            startSentence = fulltext.indexOf(" " + hitToFind.toString(), startIndex);
            if (startSentence > 0) {
                return startSentence + 1;
            }
            return -1;
        }
        if (StringUtils.isEmpty(hitToFind.getSuffix())) {
            // hit could be at end of fulltext
            if (fulltext.endsWith(hitToFind.getPrefix() + hitToFind.getExact())) {
                return 0;
            }
            // or end of sentence (before a newline)
            int endSentence = fulltext.indexOf(hitToFind.toString() + "\n", startIndex);
            if (endSentence > 0) {
                return endSentence;
            }
            // or end of sentence part (before a space)
            endSentence = fulltext.indexOf(hitToFind.toString() + " ", startIndex);
            if (endSentence > 0) {
                return endSentence;
            }
        }
        return fulltext.indexOf(hitToFind.toString(), startIndex);
    }

    /**
     * We have found our hit, now we need to generate a snippet based on the Mongo fulltext
     * The strategy is to find the closest dot (start and end of sentence). If we can't find that we search for the
     * closest newline as fallback solution.
     */
    protected HitSelector generateSnippetFromFullText(HitSelector hitToFind, String fullText, int startIndex, int endIndex) {

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

        // TODO create new hit based on word-level annotation
        // but for demo purposes we temporarily generate our own 'sentence' hit

        return new HitSelector(prefix, hitToFind.getExact(), suffix);
    }

    /**
     * Given a hit, we go over all Annotations to see which one(s) match. When we found one we add it to the search result
     */
    private void findAnnotation(SearchResult result, Hit hit, AnnoPage annoPage) {
        boolean annotationFound = false;
        LOG.trace("Searching annotations for hit {},{}", hit.getStartIndex(), hit.getEndIndex());
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
