package eu.europeana.fulltext.batch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;

public class BatchUtils {

  private BatchUtils() {
    // hide implicit public constructor
  }

  public static final String ANNO_SYNC_JOB = "synchroniseAnnoJob";

  /**
   * Gets the start time of the most recent "COMPLETED" run of the anno sync job.
   *
   * @param jobExplorer Spring Batch repository JobExplorer
   * @return start time of most recent completed job, or null if no previous jobs completed
   *     successfully
   */
  public static Instant getMostRecentSuccessfulStartTime(JobExplorer jobExplorer) {
    /*
     * Restrict jobInstances to consider, so we don't load all entries in DB.
     */
    int maxJobInstancesToConsider = 30;
    List<JobInstance> jobInstances =
        jobExplorer.getJobInstances(ANNO_SYNC_JOB, 0, maxJobInstancesToConsider);
    List<JobExecution> jobExecutions =
        jobInstances.stream()
            .map(jobExplorer::getJobExecutions)
            .flatMap(List<JobExecution>::stream)
            .filter(param -> param.getExitStatus().equals(ExitStatus.COMPLETED))
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(jobExecutions)) {
      Optional<JobExecution> jobExecutionOptional =
          jobExecutions.stream()
              .min(
                  (JobExecution execution1, JobExecution execution2) ->
                      execution2.getStartTime().compareTo(execution1.getStartTime()));
      if (jobExecutionOptional.isPresent()) {
        return jobExecutionOptional.get().getStartTime().toInstant();
      }
    }
    return null;
  }
}
