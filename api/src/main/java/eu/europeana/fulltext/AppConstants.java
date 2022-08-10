package eu.europeana.fulltext;


import com.dotsub.converter.model.Configuration;

public class AppConstants {

  private AppConstants() {
    // hide implicit public constructor
  }

  // Bean names
  public static final String FULLTEXT_DATASTORE_BEAN = "fulltextDatastore";
  public static final String SPRINGBATCH_DATASTORE_BEAN = "springBatchDatastore";
  public static final String ANNO_SYNC_TASK_EXECUTOR = "annoSyncTaskExecutor";
  public static final String JOB_LAUNCHER_TASK_EXECUTOR = "jobLauncherTaskExecutor";
  public static final String ANNO_SYNC_TASK_SCHEDULER = "annoSyncTaskScheduler";

  public static final String ANNO_ITEM_READER = "annoItemReader";

  public static final Configuration defaultSubtitleConfig = new Configuration();

  public static final String CONTENT_TYPE_VTT = "text/vtt";
  public static final String CONTENT_TYPE_PLAIN = "text/plain";

  // Mongo Constants
  public static final String MONGO_LOCAL_FIELD = "localField";
  public static final String MONGO_FOREIGN_FIELD = "foreignField";
  public static final String MONGO_RESOURCE_REF_ID = "res.$id";
  public static final String RIGHTS = "rights";
  public static final String VALUE = "value";


  // Annotation Sync
  public static final String ANNOTATION_SEARCH_PATH  = "/annotation/search";
  public static final String ANNOTATION_DELETED_PATH  = "/annotations/deleted";
}
