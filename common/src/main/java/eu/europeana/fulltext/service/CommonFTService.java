package eu.europeana.fulltext.service;

import static eu.europeana.fulltext.subtitles.FulltextType.PLAIN;
import static eu.europeana.fulltext.subtitles.FulltextType.SUB_RIP;
import static eu.europeana.fulltext.subtitles.FulltextType.TTML;
import static eu.europeana.fulltext.subtitles.FulltextType.WEB_VTT;
import static eu.europeana.fulltext.util.GeneralUtils.getAnnoPageToString;
import static eu.europeana.fulltext.util.GeneralUtils.getDsId;
import static eu.europeana.fulltext.util.GeneralUtils.getLocalId;

import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.fulltext.edm.EdmFullTextPackage;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.exception.DatabaseQueryException;
import eu.europeana.fulltext.exception.InvalidFormatException;
import eu.europeana.fulltext.repository.AnnoPageRepository;
import eu.europeana.fulltext.repository.ResourceRepository;
import eu.europeana.fulltext.subtitles.AnnotationPreview;
import eu.europeana.fulltext.subtitles.FulltextType;
import eu.europeana.fulltext.util.EdmToFullTextConverter;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonFTService {

  private static final Logger LOG = LogManager.getLogger(CommonFTService.class);
  private static final Map<FulltextType, FulltextConverter> fulltextConverterMap =
      Map.of(WEB_VTT, new SubtitleFulltextConverter(), SUB_RIP, new SubtitleFulltextConverter(),
          TTML, new SubtitleFulltextConverter(),
          PLAIN, new TranscriptionFulltextConverter());
  protected final ResourceRepository resourceRepository;
  protected final AnnoPageRepository annoPageRepository;

  public CommonFTService(ResourceRepository resourceRepository,
      AnnoPageRepository annoPageRepository) {
    this.resourceRepository = resourceRepository;
    this.annoPageRepository = annoPageRepository;
  }

  /**
   * Converts the Annotation preview to Annopage Gets the appropriate handler based on the fulltext
   * Type to do the convert the input into EDMFulltextPackage
   *
   * @param annotationPreview
   * @param isContributed
   * @return
   * @throws EuropeanaApiException
   */
  public AnnoPage createAnnoPage(AnnotationPreview annotationPreview, boolean isContributed)
      throws EuropeanaApiException {
    FulltextConverter converter = fulltextConverterMap.get(annotationPreview.getFulltextType());

    if (converter == null) {
      throw new InvalidFormatException(
          String.format(
              "No converter implemented for FulltextType '%s'. Supported types are %s",
              annotationPreview.getFulltextType().getMimeType(), fulltextConverterMap.keySet()));
    }

    EdmFullTextPackage fulltext = converter.convert(annotationPreview);
    String recordId = annotationPreview.getRecordId();
    return EdmToFullTextConverter.createAnnoPage(
        getDsId(recordId), getLocalId(recordId), annotationPreview, fulltext, isContributed);
  }

  /**
   * For each AnnoPage in the input list: - updates the existing records in AnnoPage and Resource
   * collections in the database (matching on dsId, lcId, pgId and lang); or - creates new records
   * in AnnoPage and Resource collections if none exist
   *
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

}
