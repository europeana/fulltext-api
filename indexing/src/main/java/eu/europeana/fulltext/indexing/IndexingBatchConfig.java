package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.IndexingConstants.BATCH_THREAD_EXECUTOR;

import eu.europeana.fulltext.exception.SolrDocumentException;
import eu.europeana.fulltext.exception.SolrServiceException;
import eu.europeana.fulltext.indexing.listener.FulltextIndexingListener;
import eu.europeana.fulltext.indexing.listener.MetadataSyncProcessListener;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
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
import eu.europeana.fulltext.indexing.solr.MetadataSolrService;
import eu.europeana.fulltext.indexing.writer.FulltextSolrDeletionWriter;
import eu.europeana.fulltext.indexing.writer.FulltextSolrInsertionWriter;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class IndexingBatchConfig {
  private static final Logger logger = LogManager.getLogger(IndexingBatchConfig.class);

  private final JobLauncher jobLauncher;
  /** Job param used to ensure unique runs */
  private final JobParameters jobParams =
      new JobParametersBuilder().addDate("startTime", new Date()).toJobParameters();

  private final JobBuilderFactory jobs;
  private final StepBuilderFactory steps;

  private final TaskExecutor indexingTaskExecutor;

  private final IndexingAppSettings appSettings;
  private final FulltextSolrService fulltextSolr;
  private final MetadataSolrService metadataSolr;

  private final IndexingActionProcessor indexingActionProcessor;
  private final MetadataSyncActionProcessor metadataSyncActionProcessor;
  private final IndexingFulltextUpdateProcessor fulltextUpdateProcessor;
  private final IndexingMetadataCreateProcessor metadataCreateProcessor;

  private final FulltextIndexingListener fulltextIndexingListener;
  private final MetadataSyncProcessListener metadataSyncListener;

  private final FulltextSolrInsertionWriter fulltextSolrInsertionWriter;
  private final FulltextSolrDeletionWriter fulltextSolrDeletionWriter;

  private IndexingAnnoPageRepository repository;

  public IndexingBatchConfig(
      JobBuilderFactory jobs,
      StepBuilderFactory steps,
      @Qualifier(BATCH_THREAD_EXECUTOR) TaskExecutor indexingTaskExecutor,
      IndexingAppSettings appSettings,
      FulltextSolrService fulltextSolr,
      MetadataSolrService metadataSolr,
      IndexingActionProcessor indexingActionProcessor,
      MetadataSyncActionProcessor metadataSyncActionProcessor,
      IndexingFulltextUpdateProcessor fulltextUpdateProcessor,
      IndexingMetadataCreateProcessor metadataCreateProcessor,
      FulltextIndexingListener fulltextIndexingListener,
      MetadataSyncProcessListener metadataSyncListener,
      FulltextSolrInsertionWriter fulltextSolrInsertionWriter,
      FulltextSolrDeletionWriter fulltextSolrDeletionWriter,
      IndexingAnnoPageRepository repository,
      JobLauncher jobLauncher) {
    this.jobs = jobs;
    this.steps = steps;
    this.indexingTaskExecutor = indexingTaskExecutor;
    this.appSettings = appSettings;
    this.fulltextSolr = fulltextSolr;
    this.metadataSolr = metadataSolr;
    this.indexingActionProcessor = indexingActionProcessor;
    this.metadataSyncActionProcessor = metadataSyncActionProcessor;
    this.fulltextUpdateProcessor = fulltextUpdateProcessor;
    this.metadataCreateProcessor = metadataCreateProcessor;
    this.fulltextIndexingListener = fulltextIndexingListener;
    this.metadataSyncListener = metadataSyncListener;
    this.fulltextSolrInsertionWriter = fulltextSolrInsertionWriter;
    this.fulltextSolrDeletionWriter = fulltextSolrDeletionWriter;
    this.repository = repository;
    this.jobLauncher = jobLauncher;
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
        List.of(indexingActionProcessor, fulltextUpdateProcessor, metadataCreateProcessor));
    return processor;
  }

  private ItemProcessor<String, IndexingWrapper> compositeMetadataSyncProcessor() {
    CompositeItemProcessor<String, IndexingWrapper> processor = new CompositeItemProcessor<>();
    processor.setDelegates(
        List.of(metadataSyncActionProcessor, fulltextUpdateProcessor, metadataCreateProcessor));
    return processor;
  }

  private Step syncFulltextStep(Optional<Instant> from, Instant to) {
    return this.steps
        .get("syncFulltextStep")
        .<AnnoPageRecordId, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(recordIdReader(from, to))
        .listener((ItemReadListener<? super AnnoPageRecordId>) fulltextIndexingListener)
        .processor(compositeFulltextSyncProcessor())
        .listener(
            (ItemProcessListener<? super AnnoPageRecordId, ? super IndexingWrapper>)
                fulltextIndexingListener)
        .writer(compositeFulltextSyncWriter())
        .listener((ItemWriteListener<? super IndexingWrapper>) fulltextIndexingListener)
        .faultTolerant()
        // skip all exceptions up to the configurable limit
        .skip(Exception.class)
        .skipLimit(appSettings.getSkipLimit())
        .taskExecutor(indexingTaskExecutor)
        .throttleLimit(appSettings.getBatchThrottleLimit())
        .build();
  }

  private Step syncMetadataStep() {
    return this.steps
        .get("syncMetadataStep")
        .<String, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(fulltextSolrReader())
        .processor(compositeMetadataSyncProcessor())
        .listener(metadataSyncListener)
        .writer(compositeFulltextSyncWriter())
        .listener(fulltextIndexingListener)
        .faultTolerant()
        // skip all exceptions up to the configurable limit
        .skip(Exception.class)
        .skipLimit(appSettings.getSkipLimit())
        .taskExecutor(indexingTaskExecutor)
        .throttleLimit(appSettings.getBatchThrottleLimit())
        .build();
  }

  public void indexFulltext() throws Exception {
    Optional<Instant> from =
        fulltextSolr.getMostRecentValue(IndexingConstants.TIMESTAMP_UPDATE_FULLTEXT);
    Instant to = Instant.now();

    logger.info("Indexing Fulltext records modified between {} and {}", from, to);

    jobLauncher.run(
        this.jobs.get("fulltextIndexJob").start(syncFulltextStep(from, to)).build(), jobParams);
  }

  public void syncMetadataJob() throws Exception {

    Optional<Instant> fulltextMetadataUpdateValue =
        fulltextSolr.getMostRecentValue(IndexingConstants.TIMESTAMP_UPDATE_METADATA);

    Optional<Instant> metadataUpdateValue =
        metadataSolr.getMostRecentValue(IndexingConstants.TIMESTAMP_UPDATE_METADATA);

    if (fulltextMetadataUpdateValue.isEmpty() || metadataUpdateValue.isEmpty()) {
      logger.warn(
          "Missing metadata update timestamp. Fulltext value={}, Metadata value={}",
          fulltextMetadataUpdateValue,
          metadataUpdateValue);
      return;
    }

    if (fulltextMetadataUpdateValue.get().isBefore(metadataUpdateValue.get())) {
      logger.info(
          "Fulltext Solr is out of date. Triggering metadata sync. Fulltext value={}, Metadata value={}",
          fulltextMetadataUpdateValue,
          metadataUpdateValue);
      jobLauncher.run(
          this.jobs.get("syncMetadataJob").start(syncMetadataStep()).build(), jobParams);

    } else {
      logger.info(
          "Fulltext Solr is up to date. Not triggering metadata update. Fulltext value={}, Metadata value={}",
          fulltextMetadataUpdateValue,
          metadataUpdateValue);
    }
  }
}
