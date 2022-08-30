package eu.europeana.fulltext.indexing;

import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.batch.IndexingWrapper;
import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.processor.IndexingActionProcessor;
import eu.europeana.fulltext.indexing.processor.IndexingFulltextUpdateProcessor;
import eu.europeana.fulltext.indexing.processor.IndexingMetadataCreateProcessor;
import eu.europeana.fulltext.indexing.processor.MetadataSyncActionProcessor;
import eu.europeana.fulltext.indexing.reader.AnnoPageRecordIdReader;
import eu.europeana.fulltext.indexing.reader.FulltextSolrDocumentReader;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.writer.FulltextSolrDeletionWriter;
import eu.europeana.fulltext.indexing.writer.FulltextSolrInsertionWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class IndexingBatchConfig {
  private static final Logger logger = LogManager.getLogger(IndexingBatchConfig.class);

  private final JobBuilderFactory jobs;
  private final StepBuilderFactory steps;

  private final TaskExecutor migrationTaskExecutor;

  private final IndexingAppSettings appSettings;
  private final FulltextSolrService fulltextSolr;

  private final IndexingActionProcessor indexingActionProcessor;
  private final MetadataSyncActionProcessor metadataSyncActionProcessor;
  private final IndexingFulltextUpdateProcessor fulltextUpdateProcessor;
  private final IndexingMetadataCreateProcessor metadataCreateProcessor;

  private final FulltextSolrInsertionWriter fulltextSolrInsertionWriter;
  private final FulltextSolrDeletionWriter fulltextSolrDeletionWriter;

  private IndexingAnnoPageRepository repository;

  public IndexingBatchConfig(
      JobBuilderFactory jobs,
      StepBuilderFactory steps,
      TaskExecutor migrationTaskExecutor,
      IndexingAppSettings appSettings,
      FulltextSolrService fulltextSolr,
      IndexingActionProcessor indexingActionProcessor,
      MetadataSyncActionProcessor metadataSyncActionProcessor,
      IndexingFulltextUpdateProcessor fulltextUpdateProcessor,
      IndexingMetadataCreateProcessor metadataCreateProcessor,
      FulltextSolrInsertionWriter fulltextSolrInsertionWriter,
      FulltextSolrDeletionWriter fulltextSolrDeletionWriter,
      IndexingAnnoPageRepository repository) {
    this.jobs = jobs;
    this.steps = steps;
    this.migrationTaskExecutor = migrationTaskExecutor;
    this.appSettings = appSettings;
    this.fulltextSolr = fulltextSolr;
    this.indexingActionProcessor = indexingActionProcessor;
    this.metadataSyncActionProcessor = metadataSyncActionProcessor;
    this.fulltextUpdateProcessor = fulltextUpdateProcessor;
    this.metadataCreateProcessor = metadataCreateProcessor;
    this.fulltextSolrInsertionWriter = fulltextSolrInsertionWriter;
    this.fulltextSolrDeletionWriter = fulltextSolrDeletionWriter;
    this.repository = repository;
  }

  private ItemReader<AnnoPageRecordId> recordIdReader(Optional<Instant> from, Instant to) {
    return threadSafeReader(new AnnoPageRecordIdReader(repository, from.orElse(null), to));
  }

  private ItemReader<String> fulltextSolrReader() {
    return threadSafeReader(new FulltextSolrDocumentReader(fulltextSolr));
  }

  /** Makes ItemReader thread-safe */
  private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
    final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
        new SynchronizedItemStreamReader<>();
    synchronizedItemStreamReader.setDelegate(reader);
    return synchronizedItemStreamReader;
  }

  private ItemWriter<IndexingWrapper> compositeFulltextSyncWriter() {
    CompositeItemWriter<IndexingWrapper> writer = new CompositeItemWriter<>();
    writer.setDelegates(List.of(fulltextSolrInsertionWriter, fulltextSolrDeletionWriter));
    return writer;
  }

  private ItemProcessor<AnnoPageRecordId, IndexingWrapper> compositeFulltextSyncProcessor() {
    CompositeItemProcessor<AnnoPageRecordId, IndexingWrapper> processor =
        new CompositeItemProcessor<>();
    processor.setDelegates(
        List.of(indexingActionProcessor, metadataCreateProcessor, fulltextUpdateProcessor));
    return processor;
  }

  private ItemProcessor<String, IndexingWrapper> compositeMetadataSyncProcessor() {
    CompositeItemProcessor<String, IndexingWrapper> processor = new CompositeItemProcessor<>();
    processor.setDelegates(List.of(metadataSyncActionProcessor, fulltextUpdateProcessor));
    return processor;
  }

  private Step syncFulltextStep(Optional<Instant> from, Instant to) {
    return this.steps
        .get("syncFulltextStep")
        .<AnnoPageRecordId, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(recordIdReader(from, to))
        .processor(compositeFulltextSyncProcessor())
        .writer(compositeFulltextSyncWriter())
        .build();
  }

  private Step syncMetadataStep() {
    return this.steps
        .get("syncMetadataStep")
        .<String, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(fulltextSolrReader())
        .processor(compositeMetadataSyncProcessor())
        .writer(fulltextSolrInsertionWriter)
        .build();
  }

  public Job indexFulltext() throws SolrServiceException {
    Optional<Instant> from = fulltextSolr.getLastUpdateTime();
    Instant to = Instant.now();

    logger.info("Indexing Fulltext records modified between {} and {}", from, to);

    return this.jobs.get("fulltextIndexJob").start(syncFulltextStep(from, to)).build();
  }

  public Job syncMetadataJob() {
    return this.jobs.get("syncMetadataJob").start(syncMetadataStep()).build();
  }
}
