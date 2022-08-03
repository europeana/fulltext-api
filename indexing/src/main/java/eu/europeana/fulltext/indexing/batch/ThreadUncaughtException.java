package eu.europeana.fulltext.indexing.batch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO: to be used for threading
public class ThreadUncaughtException implements Thread.UncaughtExceptionHandler {
    private static final Logger logger = LogManager.getLogger(ThreadUncaughtException.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error("Thread Exception at thread "+ t.getName() + " - Message: " + e.getMessage());
        e.printStackTrace();
    }

}
