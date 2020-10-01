package eu.europeana.fulltext.search.service;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.api.service.exception.FTException;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.exception.RecordDoesNotExistException;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrHit;
import eu.europeana.fulltext.search.model.response.*;
import eu.europeana.fulltext.search.repository.SolrRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.util.NamedList;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for querying solr, retrieving fulltext data and sending back results
 *
 * @author Patrick Ehlert
 * Created on 28 May 2020
 */
@Lazy
@Service
public class FTSearchService {

    private static final Logger LOG  = LogManager.getLogger(FTSearchService.class);

    private static final String SNIPPETS          = "snippets";
    private static final String OFFSETS           = "passages";
    private static final String TEXT_START_OFFSET = "startOffsetUtf16";
    private static final String HIT_START_OFFSETS = "matchStartsUtf16";
    private static final String HIT_END_OFFSETS   = "matchEndsUtf16";

    private SolrRepo solrRepo;
    private FTService fulltextRepo;

    FTSearchService(SolrRepo solrRepo, FTService fulltextService){
        this.solrRepo = solrRepo;
        this.fulltextRepo = fulltextService;
    }

    /**
     * Searches fulltext for one particular newspaper issue (CHO)
     *
     * @param searchId       string that is set as id of the search (endpoint, path and query parameters)
     * @param europeanaId    europeana id of the issue to search
     * @param query          the string to search
     * @param pageSize       maximum number of hits
     * @param annotationType requested types of annotations
     * @param debug          if true we include debug information
     * @param requestVersion API version for request. If empty, version 2 is used by default
     * @return SearchResult object (can be empty if no hits were found)
     * @throws FTException when there is a problem processing the request (e.g. issue doesn't exist)
     */
    public SearchResult searchIssue(String searchId, EuropeanaId europeanaId, String query, int pageSize, AnnotationType annotationType,
                                    String requestVersion, boolean debug) throws FTException {
        long start = System.currentTimeMillis();
        SearchResult result = SearchResultFactory.createSearchResult(searchId, debug, requestVersion);

        Map<String, List<String>> solrResult = solrRepo.getHighlightsWithOffsets(europeanaId, query, pageSize, result.getDebug());
        if (solrResult.isEmpty()) {
            LOG.debug("Solr return empty result in {} ms", System.currentTimeMillis() - start);
            // check if there are 0 hits because the record doesn't exist
            if (!fulltextRepo.doesAnnoPageExist(europeanaId.getDatasetId(), europeanaId.getLocalId(), "1")) {
                throw new RecordDoesNotExistException(europeanaId);
            }
        } else {
            LOG.debug("Solr returned {} document in {} ms", solrResult.size(), System.currentTimeMillis() - start);
            result = findAnnopageAndAnnotations(result, solrResult, europeanaId, pageSize, annotationType, requestVersion);
        }
        LOG.debug("Search done in {} ms. Found {} annotations", (System.currentTimeMillis() - start), result.itemSize());
        return result;
    }

    private SearchResult findAnnopageAndAnnotations(SearchResult result, Map<String, List<String>> highlightInfo,
              EuropeanaId europeanaId, int pageSize, AnnotationType annoType, String requestVersion) throws FTException {
        List<SolrHit> solrHits = parseHighlightData(highlightInfo, result.getDebug());

        // TODO retrieve all annopages in 1 go instead of 1 by one?
        // we keep track of retrieved anno pages to avoid retrieving the same one multiple times
        Map<String, AnnoPage> annoPagesCache = new HashMap<>();
        for (SolrHit solrHit : solrHits) {
            // retrieve the annopage
            AnnoPage annoPage = annoPagesCache.get(solrHit.getImageId());
            if (annoPage == null) {
                Long start = System.currentTimeMillis();
                annoPage = fulltextRepo.fetchAnnoPageFromImageId(europeanaId.getDatasetId(), europeanaId.getLocalId(),
                        solrHit.getImageId(), annoType);
                if (annoPage == null) {
                    throw new RecordDoesNotExistException(europeanaId);
                } else {
                    LOG.debug("Retrieved /{}/{}/annopage/{} in {} ms", annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(),
                            System.currentTimeMillis() - start);
                    annoPagesCache.put(solrHit.getImageId(), annoPage);
                }
            }

            // use the annopage to find the matching annotations
            result = findAnnotations(result, solrHit, annoPage, pageSize, annoType, requestVersion);
            if (result.itemSize() >= pageSize) {
                break;
            }
        }

        return result;
    }



    private SearchResult findAnnotations(SearchResult result, SolrHit solrHit, AnnoPage annoPage, int pageSize,
                                         AnnotationType annoType, String requestVersion) {
        LOG.trace("  Searching for {} annotations that overlap with {}...", annoType, solrHit.getDebugInfo());
        boolean annotationsFound = false;
        for (Annotation anno : annoPage.getAns()) {
            // TODO for now we have to check if the annoType matches (see TODO in AnnoPageRepository)
            if (anno.getDcType() == annoType.getAbbreviation() && anno.getFrom() != null && anno.getTo() != null &&
                    overlap(solrHit.getStart(), solrHit.getEnd(), anno.getFrom(), anno.getTo())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Found overlap between {} and annotation {},{} with text '{}'", solrHit.getDebugInfo(),
                            anno.getFrom(), anno.getTo(), annoPage.getRes().getValue().substring(anno.getFrom(), anno.getTo()));
                }

                // Sometimes a trailing character like a dot or comma directly after the keyword is regarded as
                // another annotation (word). So we filter those out.
                if (anno.getTo() - anno.getFrom() > 1) {
                    if (AnnotationType.WORD.equals(annoType)) {
                        // Don't output hit data for word level annotations
                        result.addAnnotationHit(annoPage, anno, null);
                    } else {
                        Hit hit = HitFactory.createHit(solrHit.getStart(), solrHit.getEnd(), annoPage, anno, requestVersion);
                        result.addAnnotationHit(annoPage, anno, hit);
                    }
                    annotationsFound = true;
                } else {
                    LOG.debug("Ignoring overlap with annotation {} because it's only 1 character long", anno.getAnId());
                }
            }
            if (result.itemSize() >= pageSize) {
                break;
            }
        }

        if (!annotationsFound) {
            LOG.warn("No annotations found for {},{} on /{}/{}/annopage/{}", solrHit.getStart(), solrHit.getEnd(),
                    annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId());
        }
        return result;
    }



    /**
     * Expected data in snippets: {<imageid>}<snippet_text>
     * Expected data in passages:
     *  {"startOffsetUtf16=<number>,matchStartsUtf16=[<number1>,<number2>....],matchEndsUtf16=[<number1><number2>....]}
     */
    private List<SolrHit> parseHighlightData(Map<String, List<String>> highlightInfo, Debug debug) {
        List<SolrHit> result = new ArrayList<>();
        // TODO for now we assume there will always be only 1 language, so 1 set of snippets and offsets
        List<String> snippetsTxt = (ArrayList<String>) ((NamedList) highlightInfo.values().iterator().next()).get(SNIPPETS);
        ArrayList<NamedList> offsetsLists =(ArrayList<NamedList>) ((NamedList) highlightInfo.values().iterator().next()).get(OFFSETS);
        int nrMergedHits = 0;
        for (int i = 0; i < snippetsTxt.size(); i++) {
            // parse snippets data
            String snippetTxt = snippetsTxt.get(i);
            int imageIdEnd = snippetTxt.indexOf('}');
            String imageId = snippetTxt.substring(1, imageIdEnd);
            String snippet = snippetTxt.substring(imageIdEnd + 2);

            // parse offsets data
            NamedList offsetList = offsetsLists.get(i);
            Long textStartOffset = Long.valueOf(offsetList.get(TEXT_START_OFFSET).toString());
            // the imageId that is inserted into snippets should also be subtracted
            textStartOffset = textStartOffset + imageIdEnd + 2; // + 2 because of bracket itself plus a space behind it
            List<Integer> starts = getOffsets(offsetList.get(HIT_START_OFFSETS).toString(), textStartOffset);
            List<Integer> ends = getOffsets(offsetList.get(HIT_END_OFFSETS).toString(), textStartOffset);

            SolrHit previousHit = null;
            for (int j = 0; j < starts.size(); j++) {
                SolrHit newHit = new SolrHit(imageId, snippet, starts.get(j), ends.get(j));
                // see if we there's a nearby hit we can merge with
                if (previousHit != null && (newHit.getStart() - previousHit.getEnd() <= SearchConfig.HIT_MERGE_MAX_DISTANCE)) {
                    LOG.debug("Merging {} with {}...", previousHit.getDebugInfo(), newHit.getDebugInfo());
                    previousHit.setEnd(newHit.getEnd());
                    nrMergedHits++;
                } else {
                    result.add(newHit);
                    if (debug != null) {
                        debug.addSolrSnippet(newHit);
                    }
                }
                previousHit = newHit;
            }
        }
        LOG.debug("Parsed {} solr hits, {} merged", result.size() + nrMergedHits, nrMergedHits);
        return result;
    }

    /**
     * @param numberArrayTxt string with expected format: "[<number>,<number>....]"
     * @param textStartOffset we need to subtract this from each number to get indexes within a text (instead of within
     *                        entire issue)
     */
    private List<Integer> getOffsets(String numberArrayTxt, long textStartOffset) {
        // strip opening and closing brackets
        String numbersTxt = numberArrayTxt.substring(1, numberArrayTxt.length() - 1);
        // split numbers
        String[] numbers = numbersTxt.split(", "); // the extra space is to prevent trimming later!

        List<Integer> result = new ArrayList<>();
        for (String number : numbers) {
            Long value = Long.parseLong(number) - textStartOffset;
            if (value >= 0) {
                result.add(value.intValue());
            }
        }
        return result;
    }

    /**
     * Checks if there is an overlap between 2 start and end indexes.
     * Implementation based on https://stackoverflow.com/a/36035369
     *
     * Note that we can't use this for page-level annotations at the moment because those don't have a to and from
     * coordinate
     * @param s1 start index1
     * @param e1 end index1
     * @param s2 start index2
     * @param e2 end index2
     * @return true if there is overlap, otherwise false
     */
    private boolean overlap(int s1, int e1, int s2, int e2) {
        return (s1 <= e2 && e1 >= s2);
    }
}
