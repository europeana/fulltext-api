package eu.europeana.fulltext.search.service;

import static eu.europeana.iiif.IIIFDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.iiif.IIIFDefinitions.MEDIA_TYPE_IIIF_V2;
import static eu.europeana.iiif.IIIFDefinitions.TEXT_GRANULARITY_CONTEXT;

import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.service.FTService;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.search.config.SearchConfig;
import eu.europeana.fulltext.search.exception.RecordDoesNotExistException;
import eu.europeana.fulltext.search.exception.SearchEngineDatabaseMismatch;
import eu.europeana.fulltext.search.model.query.EuropeanaId;
import eu.europeana.fulltext.search.model.query.SolrHit;
import eu.europeana.fulltext.search.model.response.Debug;
import eu.europeana.fulltext.search.model.response.Hit;
import eu.europeana.fulltext.search.model.response.HitFactory;
import eu.europeana.fulltext.search.model.response.SearchResult;
import eu.europeana.fulltext.search.model.response.SearchResultFactory;
import eu.europeana.fulltext.search.repository.SolrRepo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.util.NamedList;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * @param annoTypes      requested types of annotations
     * @param debug          if true we include debug information
     * @param requestVersion API version for request. If empty, version 2 is used by default
     * @return SearchResult object (can be empty if no hits were found)
     * @throws EuropeanaApiException when there is a problem processing the request (e.g. issue doesn't exist)
     */
    public SearchResult searchIssue(String searchId, EuropeanaId europeanaId, String query, int pageSize,
                                    List<AnnotationType> annoTypes, String requestVersion, boolean debug)
            throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        SearchResult result = SearchResultFactory.createSearchResult(searchId, debug, requestVersion);

        Map<String, List<String>> solrResult = solrRepo.getHighlightsWithOffsets(europeanaId, query, pageSize, result.getDebug());
        if (solrResult.isEmpty()) {
            LOG.debug("Solr returned empty result in {} ms", System.currentTimeMillis() - start);
            // check if there Annopage exists by dsId and lcId in Mongo
            List<AnnoPage> existingAnnoPages = fulltextRepo.getAnnoPages(europeanaId.getDatasetId(), europeanaId.getLocalId(), null, false);
            if (existingAnnoPages.isEmpty()) {
                LOG.debug("No record for europeana Id {} in Mongo", europeanaId);
                throw new RecordDoesNotExistException(europeanaId);
            }
        } else {
            LOG.debug("Solr returned {} document in {} ms", solrResult.size(), System.currentTimeMillis() - start);
            findAnnopageAndAnnotations(result, solrResult, europeanaId, pageSize, annoTypes, requestVersion);
        }
        LOG.debug("Search done in {} ms. Found {} annotations", (System.currentTimeMillis() - start), result.itemSize());
        result.setContext(new String[]{MEDIA_TYPE_IIIF_V2, TEXT_GRANULARITY_CONTEXT,MEDIA_TYPE_EDM_JSONLD});
        return result;
    }

    private void findAnnopageAndAnnotations(SearchResult result, Map<String, List<String>> highlightInfo,
                                            EuropeanaId europeanaId, int pageSize, List<AnnotationType> annoTypes, String requestVersion)
            throws EuropeanaApiException {
        // Group Solr hits by imageId so we can link an AnnoPage to its corresponding hit(s)
        Map<String, List<SolrHit>> solrHitsByImageId = parseHighlightData(highlightInfo, result.getDebug())
                .stream()
                .collect(Collectors.groupingBy(SolrHit::getImageId));

        // if solr results were parsed correctly, else do nothing
        if (!solrHitsByImageId.isEmpty()) {
            long start = System.currentTimeMillis();
            List<String> targetIds = new ArrayList<>(solrHitsByImageId.keySet());
            try (MorphiaCursor<AnnoPage> annoPageCursor = fulltextRepo.fetchAnnoPageFromTargetId(europeanaId.getDatasetId(),
                    europeanaId.getLocalId(), targetIds, annoTypes, false)) {
                if (annoPageCursor == null || !annoPageCursor.hasNext()) {
                    LOG.error("Solr record {} with targetIds {} not found in Mongo!", europeanaId, targetIds);
                    throw new SearchEngineDatabaseMismatch();
                } else {
                    LOG.debug("Retrieved AnnoPages for {} in {} ms", europeanaId, System.currentTimeMillis() - start);
                }

                while (annoPageCursor.hasNext()) {
                    AnnoPage annoPage = annoPageCursor.next();
                    LOG.debug("Processing annoPage {}", annoPage);
                    // get relevant SolrHits by imageId (which match annoPage.tgId)
                    for (SolrHit solrHit : solrHitsByImageId.get(annoPage.getTgtId())) {
                        // use the annopage to find the matching annotations
                        findAnnotations(result, solrHit, annoPage, pageSize, annoTypes, requestVersion);
                        if (result.itemSize() >= pageSize) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private void findAnnotations(SearchResult result, SolrHit solrHit, AnnoPage annoPage, int pageSize,
                                 List<AnnotationType> annoTypes, String requestVersion) {
        LOG.trace("  Searching for {} annotations that overlap with {}...", annoTypes, solrHit.getDebugInfo());
        boolean annotationsFound = false;
        for (Annotation anno : annoPage.getAns()) {
            if (anno.getFrom() != null && anno.getTo() != null &&
                    overlap(solrHit.getStart(), solrHit.getEnd(), anno.getFrom(), anno.getTo())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  Found overlap between {} and annotation {},{} with text '{}'", solrHit.getDebugInfo(),
                            anno.getFrom(), anno.getTo(), annoPage.getRes().getValue().substring(anno.getFrom(), anno.getTo()));
                }

                // Sometimes a trailing character like a dot or comma directly after the keyword is regarded as
                // another annotation (word). So we filter those out.
                if (anno.getTo() - anno.getFrom() > 1) {
                    annotationsFound = true;
                    if (anno.getDcType() == AnnotationType.WORD.getAbbreviation()) {
                        // Don't output hit data for word level annotations
                        result.addAnnotationHit(annoPage, anno, null);
                    } else {
                        Hit hit = HitFactory.createHit(solrHit.getStart(), solrHit.getEnd(), annoPage, anno, requestVersion);
                        result.addAnnotationHit(annoPage, anno, hit);
                    }
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
    }



    /**
     * Expected data in snippets: {<imageid>}<snippet_text>
     * Expected data in passages:
     *  {"startOffsetUtf16=<number>,matchStartsUtf16=[<number1>,<number2>....],matchEndsUtf16=[<number1><number2>....]}
     */
    private List<SolrHit> parseHighlightData(Map<String, List<String>> highlightInfo, Debug debug) throws EuropeanaApiException {
        // TODO for now we assume there will always be only 1 language, so 1 set of snippets and offsets
        Object highlightObj = highlightInfo.values().iterator().next();
        List<String> snippetsTxt;
        ArrayList<NamedList> offsetsLists;
        if (highlightObj instanceof NamedList) {
            NamedList namedList = (NamedList) highlightObj;
            snippetsTxt = (ArrayList<String>) namedList.get(SNIPPETS);
            offsetsLists = (ArrayList<NamedList>) namedList.get(OFFSETS);
        } else {
            throw new EuropeanaApiException("Unexpected highlights object type: " +
                    (highlightObj == null ? null : highlightObj.getClass()));
        }

        List<SolrHit> result = new ArrayList<>();
        int nrMergedHits = 0;
        for (int i = 0; i < snippetsTxt.size(); i++) {
            // parse snippets data
            String snippetTxt = snippetsTxt.get(i);
            // only if we get the imageId/url/webresource-url  back in the solr response
            if (StringUtils.contains(snippetTxt, "}")) {
                int imageIdEnd = snippetTxt.indexOf('}');
                // the imageIds sent by Solr can contain encoded characters such as &amp; so we need to decode/unescape
                String imageId = StringEscapeUtils.unescapeXml(snippetTxt.substring(1, imageIdEnd));
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
        }
        if (result.isEmpty()) {
            LOG.error("Unexpected data in the snippets, Image id url data missing");
        } else {
            LOG.debug("Parsed {} solr hits, {} merged", result.size() + nrMergedHits, nrMergedHits);
        }
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
