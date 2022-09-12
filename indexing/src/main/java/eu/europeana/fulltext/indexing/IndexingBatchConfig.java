package eu.europeana.fulltext.indexing;

import static eu.europeana.fulltext.indexing.IndexingConstants.BATCH_THREAD_EXECUTOR;

import eu.europeana.fulltext.indexing.listener.FulltextIndexingListener;
import eu.europeana.fulltext.indexing.listener.MetadataSyncProcessListener;
import eu.europeana.fulltext.indexing.model.IndexingWrapper;
import eu.europeana.fulltext.indexing.config.IndexingAppSettings;
import eu.europeana.fulltext.indexing.model.AnnoPageRecordId;
import eu.europeana.fulltext.indexing.processor.FulltextIndexingWrapperCreator;
import eu.europeana.fulltext.indexing.processor.FulltextIndexingProcessor;
import eu.europeana.fulltext.indexing.processor.IndexingMetadataSyncProcessor;
import eu.europeana.fulltext.indexing.processor.MetadataSyncWrapperCreator;
import eu.europeana.fulltext.indexing.reader.AnnoPageRecordIdReader;
import eu.europeana.fulltext.indexing.reader.FulltextSolrDocumentReader;
import eu.europeana.fulltext.indexing.repository.IndexingAnnoPageRepository;
import eu.europeana.fulltext.indexing.solr.FulltextSolrService;
import eu.europeana.fulltext.indexing.writer.FulltextSolrDeletionWriter;
import eu.europeana.fulltext.indexing.writer.FulltextSolrInsertionWriter;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@EnableBatchProcessing
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


  private final FulltextIndexingWrapperCreator fulltextIndexingWrapperCreator;
  private final MetadataSyncWrapperCreator metadataSyncWrapperCreator;
  private final FulltextIndexingProcessor fulltextIndexingProcessor;
  private final IndexingMetadataSyncProcessor metadataSyncProcessor;

  private final FulltextIndexingListener fulltextIndexingListener;
  private final MetadataSyncProcessListener metadataSyncListener;

  private final FulltextSolrInsertionWriter fulltextSolrInsertionWriter;
  private final FulltextSolrDeletionWriter fulltextSolrDeletionWriter;

  private final IndexingAnnoPageRepository repository;

  public IndexingBatchConfig(
      JobBuilderFactory jobs,
      StepBuilderFactory steps,
      @Qualifier(BATCH_THREAD_EXECUTOR) TaskExecutor indexingTaskExecutor,
      IndexingAppSettings appSettings,
      FulltextSolrService fulltextSolr,
      FulltextIndexingWrapperCreator fulltextIndexingWrapperCreator,
      MetadataSyncWrapperCreator metadataSyncWrapperCreator,
      FulltextIndexingProcessor fulltextIndexingProcessor,
      IndexingMetadataSyncProcessor metadataSyncProcessor,
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
    this.fulltextIndexingWrapperCreator = fulltextIndexingWrapperCreator;
    this.metadataSyncWrapperCreator = metadataSyncWrapperCreator;
    this.fulltextIndexingProcessor = fulltextIndexingProcessor;
    this.metadataSyncProcessor = metadataSyncProcessor;
    this.fulltextIndexingListener = fulltextIndexingListener;
    this.metadataSyncListener = metadataSyncListener;
    this.fulltextSolrInsertionWriter = fulltextSolrInsertionWriter;
    this.fulltextSolrDeletionWriter = fulltextSolrDeletionWriter;
    this.repository = repository;
    this.jobLauncher = jobLauncher;
  }


  /**
   * Creates a thread-safe reader for fetching record identifiers from Mongo
   * @param from modification timestamp to use in reader
   * @return
   */
  private ItemReader<AnnoPageRecordId> recordIdReader(Optional<Instant> from) {
    return threadSafeReader(new AnnoPageRecordIdReader(repository, from.orElse(null)));
  }

  /**
   * Creates a thread-safe reader for fetching records from Fulltext Solr
   * @return
   */
  private ItemReader<SolrDocument> fulltextSolrReader() {
    return threadSafeReader(new FulltextSolrDocumentReader(fulltextSolr));
  }

  /** Makes ItemReader thread-safe */
  private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
    final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
        new SynchronizedItemStreamReader<>();
    synchronizedItemStreamReader.setDelegate(reader);
    return synchronizedItemStreamReader;
  }

  /**
   * Creates a composite ItemWriter that inserts and deletes documents from Solr.
   * Reader chains {@link FulltextSolrInsertionWriter} and {@link FulltextSolrDeletionWriter}
   * @return composite item writer
   */
  private ItemWriter<IndexingWrapper> compositeWriter() {
    CompositeItemWriter<IndexingWrapper> writer = new CompositeItemWriter<>();
    writer.setDelegates(List.of(fulltextSolrInsertionWriter, fulltextSolrDeletionWriter));
    return writer;
  }

  /**
   * Creates a composite processor for the Fulltext Indexing pipeline.
   * Chains {@link FulltextIndexingWrapperCreator}, {@link FulltextIndexingProcessor} and {@link IndexingMetadataSyncProcessor}
   * @return
   */
  private ItemProcessor<AnnoPageRecordId, IndexingWrapper> compositeFulltextIndexingProcessor() {
    CompositeItemProcessor<AnnoPageRecordId, IndexingWrapper> processor =
        new CompositeItemProcessor<>();
    processor.setDelegates(
        List.of(fulltextIndexingWrapperCreator, fulltextIndexingProcessor, metadataSyncProcessor));
    return processor;
  }

  /**
   * Creates a composite processor the Metadata Sync pipeline.
   * Chains {@link MetadataSyncWrapperCreator}, and {@link IndexingMetadataSyncProcessor}
   * @return
   */
  private ItemProcessor<SolrDocument, IndexingWrapper> compositeMetadataSyncProcessor() {
    CompositeItemProcessor<SolrDocument, IndexingWrapper> processor = new CompositeItemProcessor<>();
    processor.setDelegates(
        List.of(metadataSyncWrapperCreator, metadataSyncProcessor));
    return processor;
  }

  private Step syncFulltextStep(Optional<Instant> from) {
    return this.steps
        .get("syncFulltextStep")
        .<AnnoPageRecordId, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(recordIdReader(from))
        .listener((ItemReadListener<? super AnnoPageRecordId>) fulltextIndexingListener)
        .processor(compositeFulltextIndexingProcessor())
        .listener(
            (ItemProcessListener<? super AnnoPageRecordId, ? super IndexingWrapper>)
                fulltextIndexingListener)
        .writer(compositeWriter())
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
        .<SolrDocument, IndexingWrapper>chunk(appSettings.getBatchPageSize())
        .reader(fulltextSolrReader())
        .processor(compositeMetadataSyncProcessor())
        .listener(metadataSyncListener)
        .writer(compositeWriter())
        .listener(fulltextIndexingListener)
        .faultTolerant()
        // skip all exceptions up to the configurable limit
        .skip(Exception.class)
        .skipLimit(appSettings.getSkipLimit())
        .taskExecutor(indexingTaskExecutor)
        .throttleLimit(appSettings.getBatchThrottleLimit())
        .build();
  }

  public void indexFulltext(ZonedDateTime modifiedTimestamp) throws Exception {
    Optional<Instant> from;
    if(modifiedTimestamp != null){
      from = Optional.of(modifiedTimestamp.toInstant());
    } else {
      from = fulltextSolr.getMostRecentValue(IndexingConstants.TIMESTAMP_UPDATE_FULLTEXT);
    }


    logger.info("Indexing Fulltext records modified after {}", from);

    jobLauncher.run(
        this.jobs.get("fulltextIndexJob").start(syncFulltextStep(from)).build(), jobParams);
  }

  public void syncMetadataJob() throws Exception {
          jobLauncher.run(
          this.jobs.get("syncMetadataJob").start(syncMetadataStep()).build(), jobParams);

  }
}
