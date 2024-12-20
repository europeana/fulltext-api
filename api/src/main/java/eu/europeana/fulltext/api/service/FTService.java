package eu.europeana.fulltext.api.service;

import static eu.europeana.fulltext.api.service.EDM2IIIFMapping.getTextGranularitiesForSummary;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.exception.AnnoPageDoesNotExistException;
import eu.europeana.fulltext.exception.AnnoPageGoneException;
import eu.europeana.fulltext.exception.ResourceDoesNotExistException;
import eu.europeana.fulltext.exception.SerializationException;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.service.CommonFTService;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.util.AnnotationUtils;
import eu.europeana.fulltext.util.GeneralUtils;
import eu.europeana.iiif.IIIFDefinitions;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Lúthien Created on 27-02-2018
 */
@Service
public class FTService extends CommonFTService {

    private static final String GENERATED_IN = "Generated in {} ms ";
    private static final String ANNOPAGE_ID_FORMAT = "/%s/%s/annopage/%s";
    private static final String ANNOPAGE_ID_LANG_FORMAT = "/%s/%s/annopage/%s";
    private static final Logger LOG = LogManager.getLogger(FTService.class);

    private final FTSettings ftSettings;
    private final ObjectMapper mapper;


    @Value("${spring.profiles.active:}")
    private String activeProfileString;

    /*
     * Constructs an FTService object with autowired dependencies
     */
    public FTService(ResourceRepository resourceRepository, AnnoPageRepository annoPageRepository,
                     FTSettings ftSettings, ObjectMapper mapper) {
        super(resourceRepository, annoPageRepository);
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
        // if no lang is provided, fetch the original AnnoPage (ie. translation=false)
        if (StringUtils.isEmpty(lang)) {
            result = annoPageRepository.findOriginalByPageId(datasetId, localId, pageId, textGranValues, false);
            if (result == null) {
                throw new AnnoPageDoesNotExistException(
                        String.format(ANNOPAGE_ID_FORMAT, datasetId, localId, pageId));
            }
        } else {
            result = annoPageRepository.findByPageIdLang(datasetId, localId, pageId, textGranValues, lang,
                    true);
            if (result == null) {
                throw new AnnoPageDoesNotExistException(String.format(ANNOPAGE_ID_LANG_FORMAT, datasetId, localId, pageId),
                        lang);
            } else if (result.isDeprecated()) {
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
     * @param targetIds IDs of the targets (images)
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

        if (result.isDeprecated()) {
            throw new AnnoPageGoneException(String.format("/%s/%s/anno/%s", datasetId, localId, annoId));
        }
        return result;
    }

    /**
     * Handles fetching an Annotation page (aka AnnoPage) containing the Annotation with given annoId
     *
     * @param datasetId identifier of the dataset that contains the Annopage that refers to Resource
     * @param localId   identifier of the record that contains the Annopage that refers to Resource
     * @param pageId    Identifier of the item's page
     * @param lang      Language of the resource
     * @return FTResource
     * @throws ResourceDoesNotExistException when the Resource can't be found
     */
    public FTResource fetchFTResource(String datasetId, String localId, String pageId, String lang)
            throws ResourceDoesNotExistException {

        Resource result;
        if (StringUtils.isEmpty(lang)) {
            result = resourceRepository.findOriginalByPageId(datasetId, localId, pageId);
            if (result == null) {
                throw new ResourceDoesNotExistException(
                        String.format("/%s/%s/%s", datasetId, localId, pageId));
            }

        } else {
            result = resourceRepository.findByPageIdLang(datasetId, localId, pageId, lang);
            if (result == null) {
                throw new ResourceDoesNotExistException(
                        String.format("/%s/%s/%s?lang=%s", datasetId, localId, pageId, lang));
            }
        }
        return generateFTResource(result);
    }

    // = = [ collect summary information ]= = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    public AnnoPage getSingleAnnoPage(String datasetId, String localId, boolean fetchFullDoc) throws AnnoPageDoesNotExistException {
        AnnoPage annoPage = annoPageRepository.findPage(datasetId, localId, fetchFullDoc);
        if (annoPage == null) {
            throw new AnnoPageDoesNotExistException(datasetId + "/" + localId);
        }
        return annoPage;
    }

    /**
     * Collects the data needed to construct the Summary Canvas
     * The array of TextGranularities is added on the SummaryManifest level as it is in its current implementation
     * only dependent on whether the record is a text (eg. newspaper) or a media record (eg. caption).
     * The Summary endpoint does not allow the user to filter on textgranularities and they are not displayed in the
     * summary either, so this merely enumerates what values can be expected when retrieving Annopages / Annotations
     *
     * @param datasetId identifier of the dataset that contains the Annopage that refers to Resource
     * @param localId   identifier of the record that contains the Annopage that refers to Resource
     * @return SummaryManifest
     */
    public SummaryManifest collectionAnnoPageInfo(String datasetId, String localId) {
        Instant start                         = Instant.now();
        SummaryManifest apInfoSummaryManifest = new SummaryManifest(datasetId, localId);
        // fetch annopages with dsId and lcId.
        // fetch the document  with unique dctypes list and translation field instead of making queries later for text granularity and original language value
        List<AnnoPage> annoPages = annoPageRepository.getAnnoPagesWithAnnotationsDcType(datasetId, localId, null, false);

        Instant finish = Instant.now();
        LOG.debug("Time taken to fetch all the annoPages for {}/{} is - {}", datasetId, localId, Duration.between(start, finish).toMillis());

        // there are Annopages with same dsId, lcId and pgId but with different language, hence cluster the annopages with same dsId/lcId/pgID
        Map<String, List<AnnoPage>> annoPerPage = new LinkedHashMap<>();
        for (AnnoPage annoPage : annoPages) {
            annoPerPage.computeIfAbsent(annoPage.getPgId(), k -> new ArrayList<>()).add(annoPage);
        }

        for (Map.Entry<String, List<AnnoPage>> pageList : annoPerPage.entrySet()) {
            SummaryCanvas summaryCanvas = new SummaryCanvas(makeSummaryCanvasID(datasetId, localId, pageList.getKey()));

            // If there is no original Annopage, original lanaguge value should not be set.
            // if translation is set to false then we have original AnnoPage. (only translation=true values are stored in DB)
            Optional<AnnoPage> originalAnnoPage = pageList.getValue().stream().filter(annopage-> !annopage.isTranslation()).findFirst();
            if (originalAnnoPage.isPresent()) {
                summaryCanvas.setOriginalLanguage(originalAnnoPage.get().getLang());
            }

            for (AnnoPage annoPage: pageList.getValue()) {
                // for every language, add SummaryAnnoPage to the SummaryCanvas
                summaryCanvas.addAnnotation(
                        new SummaryAnnoPage(
                                makeLangAwareAnnoPageID(datasetId, localId, annoPage.getPgId(), annoPage.getLang()),
                                annoPage.getLang(),
                                getTextGranularitiesForSummary(annoPage),
                                annoPage.getSource()));
            }
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
                .append(IIIFDefinitions.FULLTEXT_ANNOPAGE_PATH)
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
                .append(IIIFDefinitions.FULLTEXT_ANNOPAGE_PATH)
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
     * @param lang      in which language should the AnnoPage be
     * @return true if it exists, otherwise false
     */
    public boolean doesAnnoPageExist(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated) {
        if (StringUtils.isEmpty(lang)) {
            // if no lang is provided, check if an original AnnoPage exists (ie. translation=false)
            return annoPageRepository.existsOriginalByPageId(datasetId, localId, pageId, includeDeprecated);
        }
        return annoPageRepository.existsByPageIdLang(datasetId, localId, pageId, lang, includeDeprecated);
    }


    // = = [ generate JSON objects ] = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    /**
     * Generates an AnnotationPageV3 (IIIF V3 response type) object with the AnnoPage as input
     *
     * @param annoPage       AnnoPage input object
     * @param textGranValues user-supplied filter on textGranularities, used here to limit searching for actually
     *                       present textGranularities
     * @param derefResource  boolean indicating whether to dereference the Resource object on the top level Annotation
     * @return AnnotationPageV3
     */
    public AnnotationPageV3 generateAnnoPageV3(AnnoPage annoPage, List<AnnotationType> textGranValues, boolean derefResource) {
        long start = System.currentTimeMillis();
        AnnotationPageV3 result = EDM2IIIFMapping.getAnnotationPageV3(annoPage, textGranValues, derefResource);
        if (LOG.isDebugEnabled()) {
            LOG.debug(GENERATED_IN, System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates an AnnotationPageV2 (IIIF V2 response type) object with the AnnoPage as input
     *
     * @param annoPage       AnnoPage input object
     * @param textGranValues user-supplied filter on textGranularities, used here to limit searching for actually
     *                       present textGranularities
     * @param derefResource  boolean indicating whether or not to load and dereference the Resource
     * @return AnnotationPageV2
     */
    public AnnotationPageV2 generateAnnoPageV2(AnnoPage annoPage, List<AnnotationType> textGranValues, boolean derefResource) {
        long start = System.currentTimeMillis();
        AnnotationPageV2 result = EDM2IIIFMapping.getAnnotationPageV2(annoPage, textGranValues, derefResource);
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
    public AnnotationV3 generateAnnotationV3(AnnoPage annoPage, String annoId, boolean profileText) {
        long start = System.currentTimeMillis();
        AnnotationV3 result = EDM2IIIFMapping.getSingleAnnotationV3(annoPage, annoId, profileText);
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
    public AnnotationV2 generateAnnotationV2(AnnoPage annoPage, String annoId, boolean profileText) {
        long start = System.currentTimeMillis();
        AnnotationV2 result = EDM2IIIFMapping.getSingleAnnotationV2(annoPage, annoId, profileText);
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


    public AnnoPage updateAnnoPage(
            AnnotationPreview annotationPreview, AnnoPage existingAnnoPage)
            throws EuropeanaApiException {
        AnnoPage annoPage = getAnnoPageToUpdate(annotationPreview, existingAnnoPage);
        resourceRepository.saveResource(annoPage.getRes());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated Resource in db : id={}", annoPage.getRes().getId());
        }

        if (AnnotationUtils.isAnnoPageUpdateRequired(annotationPreview)) {
            UpdateResult results = annoPageRepository.updateAnnoPage(existingAnnoPage, annoPage);
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
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG (if provided), if not then return a HTTP 404
     * <p>
     * If present, check if the annoPages are deprecated already, respond with HTTP 410
     * NOTE : We may have multiple AnnoPages if lang is NOT provided,
     * hence respond with HTTP 410 only if all the AnnoPages are already deprecated.
     *
     * @param datasetId
     * @param localId
     * @param pageId
     * @param lang
     * @param includeDeprecated
     * @throws AnnoPageDoesNotExistException
     * @throws AnnoPageGoneException
     */
    public void checkForExistingAndDeprecatedAnnoPages(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated)
            throws AnnoPageDoesNotExistException, AnnoPageGoneException {
        if (lang != null) {
            AnnoPage annoPage = getShellAnnoPageById(datasetId, localId, pageId, lang, includeDeprecated);

            if (annoPage == null) {
                throw new AnnoPageDoesNotExistException(
                        GeneralUtils.getAnnoPageUrl(datasetId, localId, pageId, lang));
            }

            if (annoPage.isDeprecated()) {
                throw new AnnoPageGoneException(String.format("/%s/%s/annopage/%s", datasetId, localId, pageId), lang);
            }
        } else {
            List<AnnoPage> existingAnnoPages = getAnnoPages(datasetId, localId, pageId, includeDeprecated);

            if (existingAnnoPages.isEmpty()) {
                throw new AnnoPageDoesNotExistException(
                        GeneralUtils.getAnnoPageUrl(datasetId, localId, pageId, lang));
            }

            boolean isAllDeprecated = existingAnnoPages.stream().allMatch(annoPage -> annoPage.isDeprecated());
            if (isAllDeprecated) {
                throw new AnnoPageGoneException(String.format("/%s/%s/annopage/%s", datasetId, localId, pageId));
            }
        }
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

    public AnnoPage getShellAnnoPageById(String datasetId, String localId, String pageId, String lang, boolean includeDeprecated) {
        return annoPageRepository.getShellAnnoPageById(datasetId, localId, pageId, lang, includeDeprecated);
    }

    public List<AnnoPage> getAnnoPages(String datasetId, String localId, String pageId, boolean includeDeprecated) {
        return annoPageRepository.getAnnoPages(datasetId, localId, pageId, includeDeprecated, false);
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

    public boolean resourceExists(String datasetId, String localId, String pageId, String lang) {
        return resourceRepository.resourceExists(datasetId, localId, pageId, lang);
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
