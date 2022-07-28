package eu.europeana.fulltext.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.query.internal.MorphiaCursor;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.api.config.FTDefinitions;
import eu.europeana.fulltext.api.config.FTSettings;
import eu.europeana.fulltext.api.model.FTResource;
import eu.europeana.fulltext.api.model.info.SummaryAnnoPage;
import eu.europeana.fulltext.api.model.info.SummaryCanvas;
import eu.europeana.fulltext.api.model.info.SummaryManifest;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.impl.SubtitleFulltextAdapter;
import eu.europeana.fulltext.api.service.impl.TranscriptionFulltextAdapter;
import eu.europeana.fulltext.edm.EdmFullTextPackage;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.*;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.util.AnnotationUtils;
import eu.europeana.fulltext.util.EdmToFullTextConverter;
import eu.europeana.fulltext.util.GeneralUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static eu.europeana.fulltext.subtitles.FulltextType.SRT;
import static eu.europeana.fulltext.subtitles.FulltextType.WEB_VTT;
import static eu.europeana.fulltext.util.GeneralUtils.*;

/**
 * @author Lúthien Created on 27-02-2018
 */
@Service
public class FTService {

    private static final String GENERATED_IN = "Generated in {} ms ";
    private static final String ANNOPAGE_ID_FORMAT = "/%s/%s/annopage/%s";
    private static final String ANNOPAGE_ID_LANG_FORMAT = "/%s/%s/annopage/%s";
    private static final String FETCHED_AGGREGATED = "Originals fetched in {} ms";
    private static final Logger LOG = LogManager.getLogger(FTService.class);

    private final ResourceRepository resourceRepository;
    private final AnnoPageRepository annoPageRepository;
    private final FTSettings ftSettings;
    private final ObjectMapper mapper;

    private static final Map<FulltextType, FulltextAdapter> fulltextAdapterMap =
            Map.of(WEB_VTT, new SubtitleFulltextAdapter(), SRT, new TranscriptionFulltextAdapter());

    @Value("${spring.profiles.active:}")
    private String activeProfileString;

    /*
     * Constructs an FTService object with autowired dependencies
     */
    public FTService(ResourceRepository resourceRepository, AnnoPageRepository annoPageRepository,
                     FTSettings ftSettings, ObjectMapper mapper) {
        this.resourceRepository = resourceRepository;
        this.annoPageRepository = annoPageRepository;
        this.ftSettings = ftSettings;
        this.mapper = mapper;
    }

    /**
     * @return ManifestSettings object containing settings loaded from properties file
     */
    public FTSettings getSettings() {
        return ftSettings;
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Handles fetching an Annotation page (aka AnnoPage) with all its annotations
     *
     * @param datasetId      identifier of the AnnoPage's dataset
     * @param localId        identifier of the AnnoPage's record
     * @param pageId         identifier of the AnnoPage
     * @param textGranValues dcType values to filter annotations with
     * @param lang           optional, if provided we'll check if there's an annopage with this
     *                       language
     * @return AnnoPage
     * @throws AnnoPageDoesNotExistException when the Annopage cannot be found
     */
    public AnnoPage fetchAnnoPage(String datasetId, String localId, String pageId, List<AnnotationType> textGranValues,
        String lang) throws AnnoPageDoesNotExistException, AnnoPageGoneException {
        AnnoPage result;
        if (StringUtils.isEmpty(lang)) {
            result = annoPageRepository.findByPageId(datasetId, localId, pageId, textGranValues, true);
            if (result == null) {
                throw new AnnoPageDoesNotExistException(
                    String.format(ANNOPAGE_ID_FORMAT, datasetId, localId, pageId));
            }
            else if(result.isDeprecated()){
                throw new AnnoPageGoneException(
                    String.format(ANNOPAGE_ID_FORMAT, datasetId, localId, pageId));
            }
        } else {
            result = annoPageRepository.findByPageIdLang(datasetId, localId, pageId, textGranValues, lang,
                true);
            if (result == null) {
                throw new AnnoPageDoesNotExistException(String.format(ANNOPAGE_ID_LANG_FORMAT, datasetId, localId, pageId),
                    lang);
            }
            else if(result.isDeprecated()){
                throw new AnnoPageGoneException(String.format(ANNOPAGE_ID_LANG_FORMAT, datasetId, localId, pageId),
                    lang);
            }
        }

        return result;
    }


    /**
     * Retrieve a cursor to AnnoPages with the provided datasetId, localId and targetIds. If the annotationType is
     * specified the returned AnnoPages will only contain annotations of that type. If annotationType is null or empty
     * then all annotations of that type will be returned. The cursor must be closed when the caller is done!
     *
     * @param datasetId ID of the dataset
     * @param localId   ID of the parent of the Annopage object
     * @param targetIds  IDs of the targets (images)
     * @param annoTypes type of annotations that should be retrieved, if null or empty all annotations of that annopage
     *                  will be retrieved
     * @return MorphiaCursor containing AnnoPage entries.
     */
    public MorphiaCursor<AnnoPage> fetchAnnoPageFromTargetId(String datasetId, String localId, List<String> targetIds,
        List<AnnotationType> annoTypes, boolean includeDeprecated) {
        return annoPageRepository.findByTargetId(datasetId, localId, targetIds, annoTypes, includeDeprecated);
    }


    /**
     * Handles fetching an Annotation page (aka AnnoPage) containing the Annotation with given annoId
     *
     * @param datasetId identifier of the AnnoPage's dataset
     * @param localId   identifier of the AnnoPage's record
     * @param annoId    identifier of the Annotation to be found
     * @return AnnoPage
     * @throws AnnoPageDoesNotExistException when the Annopage containing the required Annotation can't be found
     */
    public AnnoPage fetchAPAnnotation(String datasetId, String localId, String annoId)
        throws EuropeanaApiException {
        AnnoPage result = annoPageRepository.findByAnnoId(datasetId, localId, annoId, true);

        if (result == null) {
            throw new AnnoPageDoesNotExistException(String.format("/%s/%s/anno/%s", datasetId, localId, annoId));
        }

        if(result.isDeprecated()){
            throw new AnnoPageGoneException(String.format("/%s/%s/anno/%s", datasetId, localId, annoId));
        }
        return result;
    }


    /**
     * Handles fetching an Annotation page (aka AnnoPage) containing the Annotation with given annoId
     *
     * @param datasetId identifier of the dataset that contains the Annopage that refers to Resource
     * @param localId   identifier of the record that contains the Annopage that refers to Resource
     * @param resId     identifier of the Resource
     * @return FTResource
     * @throws ResourceDoesNotExistException when the Resource can't be found
     */
    public FTResource fetchFTResource(String datasetId, String localId, String resId)
        throws ResourceDoesNotExistException {
        Resource result = resourceRepository.findByResId(datasetId, localId, resId);
        if (result == null) {
            throw new ResourceDoesNotExistException(String.format("/%s/%s/%s", datasetId, localId, resId));
        }
        return generateFTResource(result);
    }

    // = = [ collect summary information ]= = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnoPage getSingleAnnoPage(String datasetId, String localId) throws AnnoPageDoesNotExistException {
        AnnoPage annoPage = annoPageRepository.findPage(datasetId, localId);
        if (annoPage == null) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + localId);
        }
        return annoPage;
    }

    public SummaryManifest collectionAnnoPageInfo(String datasetId, String localId) {
        Instant start = Instant.now();
        SummaryManifest apInfoSummaryManifest = new SummaryManifest(datasetId, localId);
        List<AnnoPage> annoPages = annoPageRepository.getAnnoPages(datasetId, localId);
        Instant finish = Instant.now();
        LOG.debug(FETCHED_AGGREGATED,  Duration.between(start, finish).toMillis());

        for (AnnoPage annoPage : annoPages){
            SummaryCanvas summaryCanvas = new SummaryCanvas(makeSummaryCanvasID(datasetId, localId,
                annoPage.getPgId()));

            // add original SummaryAnnoPage to the SummaryCanvas
            summaryCanvas.addAnnotation(
                new SummaryAnnoPage(
                    makeLangAwareAnnoPageID(datasetId, localId, annoPage.getPgId(),
                        annoPage.getLang()),
                    annoPage.getLang()));
            summaryCanvas.setOriginalLanguage(annoPage.getLang());

            // add SummaryCanvas to SummaryManifest
            apInfoSummaryManifest.addCanvas(summaryCanvas);
        }
        return apInfoSummaryManifest;
    }

    @Deprecated
    private String makeSummaryCanvasID(AnnoPage ap) {
        return ftSettings.getAnnoPageBaseUrl() + ap.getDsId() + "/" + ap.getLcId() + FTDefinitions.CANVAS_PATH + "/"
            + ap.getPgId();
    }

    private String makeSummaryCanvasID(String dsId, String lcId, String pgId) {
        return ftSettings.getAnnoPageBaseUrl() + dsId + "/" + lcId + FTDefinitions.CANVAS_PATH + "/" + pgId;
    }

    @Deprecated
    private String makeLangAwareAnnoPageID(AnnoPage ap) {
        StringBuilder result = new StringBuilder(100);
        result.append(ftSettings.getAnnoPageBaseUrl())
            .append(ap.getDsId())
            .append("/")
            .append(ap.getLcId())
            .append(FTDefinitions.ANNOPAGE_PATH)
            .append("/")
            .append(ap.getPgId());
        if (!StringUtils.isEmpty(ap.getLang())) {
            result.append("?")
                .append(FTDefinitions.LANGUAGE_PARAM)
                .append(ap.getLang());
        }
        return result.toString();
    }

    private String makeLangAwareAnnoPageID(String dsId, String lcId, String pgId, String lang) {
        StringBuilder result = new StringBuilder(100);
        result.append(ftSettings.getAnnoPageBaseUrl())
            .append(dsId)
            .append("/")
            .append(lcId)
            .append(FTDefinitions.ANNOPAGE_PATH)
            .append("/")
            .append(pgId);
        if (!StringUtils.isEmpty(lang)) {
            result.append("?")
                .append(FTDefinitions.LANGUAGE_PARAM)
                .append(lang);
        }
        return result.toString();
    }

    // = = [ check Document existence ]= = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Check if a particular annotation page with the provided ids exists or not
     *
     * @param datasetId Identifier of the dataset
     * @param localId   Identifier of the item
     * @param pageId    Identifier of the item's page
     * @param lang      optional, in which language should the AnnoPage be
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnoPageExist(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated) {
        if (StringUtils.isEmpty(lang)) {
            return annoPageRepository.existsByPageId(datasetId, localId, pageId, includeDeprecated);
        }
       return annoPageRepository.existsByPageIdLang(datasetId, localId, pageId, lang, includeDeprecated);
    }


    // = = [ generate JSON objects ] = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Generates an AnnotationPageV3 (IIIF V3 response type) object with the AnnoPage as input
     *
     * @param annoPage      AnnoPage input object
     * @param derefResource boolean indicating whether to dereference the Resource object on the top level Annotation
     * @return AnnotationPageV3
     */
    public AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage, boolean derefResource) {
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationPageV2 (IIIF V2 response type) object with the AnnoPage as input
     *
     * @param annoPage      AnnoPage input object
     * @param derefResource boolean indicating whether or not to load and dereference the Resource
     * @return AnnotationPageV2
     */
    public AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage, boolean derefResource) {
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationV3 (IIIF V3 response type) object of the Annotation with ID annoId, found within the the
     * AnnoPage input
     *
     * @param annoPage AnnoPage input object
     * @param annoId   String the Annotation idenfifier
     * @return AnnotationV3
     */
    public AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId) {
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }


    /**
     * Generates an AnnotationV2 (IIIF V2 response type) object of the Annotation with ID annoId, found within the the
     * AnnoPage input
     *
     * @param annoPage AnnoPage input object
     * @param annoId   String the Annotation idenfifier
     * @return AnnotationV2
     */
    public AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId) {
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    private FTResource generateFTResource(Resource resource) {
        long start = System.currentTimeMillis();
        FTResource result = EDM2IIIFMapping.getFTResource(resource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Saves the given AnnoPage in the database.
     *
     * @param annoPage AnnoPage to save
     */
    public void saveAnnoPage(AnnoPage annoPage) {
        annoPageRepository.saveAnnoPage(annoPage);
        if (annoPage.getRes() != null) {
            resourceRepository.saveResource(annoPage.getRes());
        }
        LOG.info("Saved annoPage to database - {} ", annoPage);
    }

    /**
     * Converts the Annotation preview to Annopage
     * Gets the appropriate handler based on the fulltext Type to do the convert the input into EDMFulltextPackage
     *
     * @param annotationPreview
     * @param isContributed
     * @return
     * @throws EuropeanaApiException
     */
    public AnnoPage createAnnoPage(AnnotationPreview annotationPreview, boolean isContributed) throws EuropeanaApiException {
        EdmFullTextPackage fulltext = fulltextAdapterMap.get(annotationPreview.getFulltextType()).adapt(annotationPreview);
        String recordId = annotationPreview.getRecordId();
        return EdmToFullTextConverter.getAnnoPage(
                getDsId(recordId), getLocalId(recordId), annotationPreview, fulltext, isContributed);
    }

  public AnnoPage updateAnnoPage(
      AnnotationPreview annotationPreview, AnnoPage existingAnnoPage)
          throws EuropeanaApiException {
    AnnoPage annoPage = getAnnoPageToUpdate(annotationPreview, existingAnnoPage);
    resourceRepository.saveResource(annoPage.getRes());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Updated Resource in db : id={}", annoPage.getRes().getId());
    }

    if (AnnotationUtils.isAnnoPageUpdateRequired(annotationPreview)) {
      UpdateResult results = annoPageRepository.updateAnnoPage(annoPage);
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Updated annoPage in db : dsId={}, lcId={}, pgId={}, lang={}, matched={}, modified={}",
            annoPage.getDsId(),
            annoPage.getLcId(),
            annoPage.getPgId(),
            annoPage.getLang(),
            results.getMatchedCount(),
            results.getModifiedCount());
      }
    }
    return annoPage;
  }

    /**
     * Deprecates AnnoPage with the specified dsId, lcId, pgId and lang values.
     * Deprecation deletes the Resource associated to an AnnoPage and its annotations. Other properties
     * are retained within the AnnoPage – effectively making it a "shell" record.
     *
     * Can deprecate max 1 AnnoPage.
     */
    public void deprecateAnnoPages(String datasetId, String localId, String pageId, String lang) {
        long resourceCount = resourceRepository.deleteResource(datasetId, localId, lang);
        long annoPageCount = annoPageRepository.deprecateAnnoPage(datasetId, localId, pageId, lang);
        LOG.info(
            "AnnoPage and Resource with datasetId={}, localId={}, pageId={}, lang={} are deprecated. resourceCount={}, annoPageCount={}",
            datasetId,
            localId,
            pageId,
            lang,
            resourceCount,
            annoPageCount);
    }

    /** Deprecates AnnoPage(s) with the specified dsId, lcId and pgId.
     * Deprecation deletes the Resource associated to an AnnoPage and its annotations. Other properties
     * are retained within the AnnoPage – effectively making it a "shell" record.
     *
     *
     *  Could deprecate multiple records
     */
    public void deprecateAnnoPages(String datasetId, String localId, String pageId) {
        long resourceCount = resourceRepository.deleteResources(datasetId, localId);
        long annoPageCount = annoPageRepository.deprecateAnnoPages(datasetId, localId, pageId);
        LOG.info(
            "{} AnnoPage and {} Resource with datasetId={}, localId={}, pageId={} are deprecated",
            annoPageCount,
            resourceCount,
            datasetId,
            localId,
            pageId);
    }

    private AnnoPage getAnnoPageToUpdate(
        AnnotationPreview annotationPreview, AnnoPage existingAnnoPage) throws EuropeanaApiException {
        AnnoPage annoPageTobeUpdated = null;
        // if there is no subtitles ie; content was empty, only update rights in the resource
        if (StringUtils.isEmpty(annotationPreview.getAnnotationBody())) {
            annoPageTobeUpdated = existingAnnoPage;
            annoPageTobeUpdated.getRes().setRights(annotationPreview.getRights());
            // if new source value is present, add the value in annoPage
            if (StringUtils.isNotEmpty(annotationPreview.getSource())) {
                annoPageTobeUpdated.setSource(annotationPreview.getSource());
            }
        } else { // process the new content body and update annotations in AnnoPage.
            // Also, rights and value in Resource
            annoPageTobeUpdated = createAnnoPage(annotationPreview, false);
            if (StringUtils.isEmpty(annoPageTobeUpdated.getSource())
                && StringUtils.isNotEmpty(existingAnnoPage.getSource())) {
                annoPageTobeUpdated.setSource(existingAnnoPage.getSource());
            }
        }
        return annoPageTobeUpdated;
    }

    /**
     * Gets AnnoPage with the specified source. Only identifying properties (ie. dsId,
     * lcId, pgId, tgId, lang) are populated.
     *
     * @param source source to query for
     * @return AnnoPage
     */
    public AnnoPage getShellAnnoPageBySource(String source, boolean includeDeprecated) {
        return annoPageRepository.getAnnoPageWithSource(source, false, includeDeprecated);
    }

    public AnnoPage getShellAnnoPageById(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated){
        return annoPageRepository.getShellAnnoPageById(datasetId, localId, pageId, lang, includeDeprecated);
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Serialize data from MongoDB to JSON-LD
     *
     * @param data input data
     * @return JSON-LD string
     * @throws SerializationException when serialisation seriously severely snaps somewhere
     */
    public String serialise(Object data) throws SerializationException {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (IOException e) {
            throw new SerializationException("Error serialising data: " + e.getMessage(), e);
        }
    }

    /**
     * Deprecates AnnoPage(s) with the specified source
     *
     * @param sources sources to query
     * @return number of deprecated documents
     */
    public long deprecateAnnoPagesWithSources(List<? extends String> sources) {
        List<String> resourceIds = annoPageRepository.getResourceIdsForAnnoPageSources(sources);
        long resourceCount = resourceRepository.deleteResourcesById(resourceIds);
        long annoPageCount = annoPageRepository.deprecateAnnoPagesWithSources(sources);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deprecated {} AnnoPages and deleted {} Resources for for sources {}", annoPageCount, resourceCount, sources);
        }

        return annoPageCount;
    }

    /**
     * For each AnnoPage in the input list:
     *      - updates the existing records in AnnoPage and Resource collections in the database (matching on dsId, lcId, pgId and lang); or
     *      - creates new records in AnnoPage and Resource collections if none exist
     * @param annoPageList List of AnnoPages to upsert
     * @throws DatabaseQueryException if
     */
    public BulkWriteResult upsertAnnoPage(List<? extends AnnoPage> annoPageList)
        throws DatabaseQueryException {
        BulkWriteResult resourceWriteResult = resourceRepository.upsertFromAnnoPage(annoPageList);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                "Saved resources to db: replaced={}; new={}",
                resourceWriteResult.getModifiedCount(),
                resourceWriteResult.getUpserts().size());
        }

        BulkWriteResult annoPageWriteResult = annoPageRepository.upsertAnnoPages(annoPageList);
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                "Saved annoPages to db: replaced={}; new={}; annoPages={}",
                annoPageWriteResult.getModifiedCount(),
                annoPageWriteResult.getUpserts().size(),
                getAnnoPageToString(annoPageList));
        }

        return annoPageWriteResult;
    }

    /**
     * Retrieves the AnnoPage with the specified dcId, lcId, pgId and lang
     *
     * @return AnnoPage or null if none found
     */
    public AnnoPage getAnnoPageByPgId(
        String datasetId, String localId, String pgId, String lang, boolean includeDeprecated) {
        return annoPageRepository.findByPageIdLang(datasetId, localId, pgId, List.of(), lang, includeDeprecated);
    }

    /**
     * Drops the AnnoPage and Resource collections. Can only be successfully
     * invoked from tests
     */
    public void dropCollections() {
        annoPageRepository.deleteAll();
        resourceRepository.deleteAll();
    }

    public long countAnnoPage() {
        if (GeneralUtils.testProfileNotActive(activeProfileString)) {
            LOG.warn(
                "Repository count is temporarily disabled because of bad performance with large collections");
            return 0;
        }

        return annoPageRepository.count();
    }

    public long countResource() {
        if (GeneralUtils.testProfileNotActive(activeProfileString)) {
            LOG.warn(
                "Repository count is temporarily disabled because of bad performance with large collections");
            return 0;
        }

        return resourceRepository.count();
    }

    public boolean resourceExists(String datasetId, String localId, String resId) {
        return resourceRepository.resourceExists(datasetId, localId, resId);
    }


    public void deleteAll() {
        if (GeneralUtils.testProfileNotActive(activeProfileString)) {
            LOG.warn(
                "Attempting to delete repository from non-test code...");
            return;
        }
        annoPageRepository.deleteAll();
        resourceRepository.deleteAll();
    }
}
