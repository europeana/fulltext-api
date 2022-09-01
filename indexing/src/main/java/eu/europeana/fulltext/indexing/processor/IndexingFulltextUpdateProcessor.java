package eu.europeana.fulltext.indexing.processor;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.indexing.IndexingConstants;
import eu.europeana.fulltext.indexing.model.IndexingAction;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor that copies Fulltext Resources to Solr. Expects to run BEFORE {@link
 * IndexingMetadataCreateProcessor}
 */
@Component
public class IndexingFulltextUpdateProcessor
    implements ItemProcessor<IndexingWrapper, IndexingWrapper> {

  private final FulltextSolrService fulltextSolr;
  private final IndexingAnnoPageRepository repository;

  private static final Logger logger = LogManager.getLogger(IndexingFulltextUpdateProcessor.class);

  public IndexingFulltextUpdateProcessor(
      FulltextSolrService fulltextSolr, IndexingAnnoPageRepository repository) {
    this.fulltextSolr = fulltextSolr;
    this.repository = repository;
  }

  @Override
  public IndexingWrapper process(IndexingWrapper indexingWrapper) throws Exception {
    // Processor only runs on "Create" and "Update" action
    if (!indexingWrapper.getAction().equals(IndexingAction.CREATE)
        && !indexingWrapper.getAction().equals(IndexingAction.UPDATE)) {
      return indexingWrapper;
    }

    AnnoPageRecordId recordId = indexingWrapper.getRecordId();
    String europeanaId = recordId.toEuropeanaId();

    Map<String, List<String>> langFtContent = new HashMap<>();

    SolrInputDocument doc =  new SolrInputDocument(IndexingConstants.EUROPEANA_ID, europeanaId);

    List<AnnoPage> annoPages =
        repository.getAnnoPagesWithProjection(recordId.getDsId(), recordId.getLcId());

    // only necessary when Solr is the source
    if (annoPages.isEmpty()) {
      logger.info("No AnnoPage exists in Fulltext database for {}; Will delete Fulltext doc in Solr", recordId);
      // mark for deletion (will be handled in the writer)
      indexingWrapper.setAction(IndexingAction.DELETE);
      return indexingWrapper;
    }

    // creates a mapping between AnnoPage deprecation status and AnnoPages.
    // True -> Active
    // False -> Deprecated
    Map<Boolean, List<AnnoPage>> annoPageMap =
        annoPages.stream().collect(Collectors.groupingBy(AnnoPage::isActive));

    Date modified = Date.from(Instant.EPOCH);
    for (AnnoPage ap : annoPageMap.getOrDefault(Boolean.TRUE, Collections.emptyList())) {
      String fulltext = ap.getRes().getValue();
      String lang = ap.getLang();
      if (!isLangSupported(lang, fulltextSolr.getSchema())) {
        if (logger.isTraceEnabled()) {
          logger.trace(
              "Record {} - language not supported: {} . Indexing in fulltext.", europeanaId, lang);
        }

        lang = "";
      }

      String target = ap.getTgtId();
      Date apModified = ap.getModified();
      if (modified.before(apModified)) {
        modified = apModified;
      }
      String content = addFulltextPrefix(target, fulltext);
      List<String> listContents = langFtContent.computeIfAbsent(lang, k -> new ArrayList<>());
      listContents.add(content);
    }

    // handle deleted AnnoPages
    for (AnnoPage ap : annoPageMap.getOrDefault(Boolean.FALSE, Collections.emptyList())) {
      String lang = ap.getLang();
      if (logger.isTraceEnabled() && !isLangSupported(lang, fulltextSolr.getSchema())) {
        logger.trace(
            "Record {} - language not supported: {} . Indexing in fulltext.", europeanaId, lang);
      }
      if (!langFtContent.containsKey(lang)) {
        langFtContent.put(
            lang, new ArrayList<>()); // hopefully removes content (although not the field)
      }
    }

    for (Entry<String, List<String>> entry : langFtContent.entrySet()) {
      doc.addField(
          IndexingConstants.FULLTEXT + "." + entry.getKey(), Map.of("set", entry.getValue()));
    }

    doc.addField(IndexingConstants.TIMESTAMP_UPDATE_FULLTEXT, Map.of("set", modified));

    indexingWrapper.setSolrDocument(doc);
    return indexingWrapper;
  }

  private boolean isLangSupported(String language, SchemaRepresentation schema) {
    if (language == null || language.isEmpty()) {
      return false;
    }

    return schema.getFields().stream()
        .map(p -> p.get("name"))
        .collect(Collectors.toList())
        .contains(IndexingConstants.FULLTEXT + "." + language);
  }

  /**
   * Prefix added to each fulltext content. It will be stored but not indexed. It is useful for the
   * API to locate the matched terms and highlight them in the image (if applicable)
   *
   * @param fulltext
   * @param target
   * @return
   */
  private String addFulltextPrefix(String target, String fulltext) {
    return "{" + target + "} " + fulltext;
  }
}
