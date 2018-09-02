package eu.europeana.fulltext.loader.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Utility class to log progress of long processes
 * @author Patrick Ehlert
 * Created on 30-03-2018
 */
public class ProgressLogger {

    private static final Logger LOG = LogManager.getLogger(ProgressLogger.class);

    private long expectedItems;
    private long itemsOk;
    private long itemsFail;

    private long startTime;
    private int logAfterSeconds;
    private long lastLogTime;

    public ProgressLogger(int logAfterSeconds) {
        this.startTime = System.currentTimeMillis();
        this.lastLogTime = startTime;
        this.logAfterSeconds = logAfterSeconds;
    }

    /**
     * Create a new progressLogger. This also sets the operation start Time
     * @param expectedItems total number of items that are expected to be retrieved. The value is optional, if
     *                      specified then logging will include an estimate of the remaining time
     * @param logAfterSeconds to prevent too much logging, only log every x seconds
     */
    public ProgressLogger(long expectedItems, int logAfterSeconds) {
        this.startTime = System.currentTimeMillis();
        this.lastLogTime = startTime;
        this.expectedItems = expectedItems;
        this.logAfterSeconds = logAfterSeconds;
    }

    public void setExpectedItems(long expectedItems) {
        this.expectedItems = expectedItems;
    }

    /**
     * Report that another item is processed fine.
     */
    public void addItemOk() {
        this.itemsOk++;
        logProgress();
    }

    public long getItemsDone() {
        return this.itemsFail + this.itemsOk;
    }

    /**
     * Report that another item is processed fine.
     */
    public void addItemFail() {
        this.itemsFail++;
        logProgress();
    }

    /**
     * Log the number of items that are left to retrieve and an estimate of the remaining time, but only every x seconds
     * as specified by logAfterSeconds
     */
    public void logProgress() {
        Duration d = new Duration(lastLogTime, System.currentTimeMillis());
        if (logAfterSeconds > 0 && d.getMillis() / 1000 > logAfterSeconds) {
            Long itemsDone = getItemsDone();
            if (expectedItems > 0) {
                Double itemsPerMS = itemsDone * 1d / (System.currentTimeMillis() - startTime);
                if (itemsPerMS * 1000 > 1.0) {
                    LOG.info("Processed {} items of {} ({} failed, {} item/sec). Expected time remaining is {}",
                            itemsDone, expectedItems, itemsFail,
                            Math.round(itemsPerMS * 1000), getDurationText(Math.round((expectedItems - itemsDone) / itemsPerMS)));
                } else {
                    LOG.info("Processed {} items of {} ({} failed, {} item/min). Expected time remaining is {}",
                            itemsDone, expectedItems, itemsFail,
                            Math.round(itemsPerMS * 1000 * 60), getDurationText(Math.round((expectedItems - itemsDone) / itemsPerMS)));
                }
            } else {
                LOG.info("Processed {} items. {} failed.", itemsDone, itemsFail);
            }
            lastLogTime = System.currentTimeMillis();
        }
    }

    /**
     * Return current results
     */
    public String getResults() {
        return("Processed " +getItemsDone()+" files in " +getDurationText(System.currentTimeMillis()-startTime)+
                " (expected "+expectedItems+" files). " +itemsFail+" files were skipped");
    }

    /**
     * @param durationInMs
     * @return string containing duration in easy readable format
     */
    public static String getDurationText(long durationInMs) {
        String result;
        Period period = new Period(durationInMs);
        if (period.getDays() >= 1) {
            result = String.format("%d days, %d hours and %d minutes", period.getDays(), period.getHours(), period.getMinutes());
        } else if (period.getHours() >= 1) {
            result = String.format("%d hours and %d minutes", period.getHours(), period.getMinutes());
        } else if (period.getMinutes() >= 1){
            result = String.format("%d minutes and %d seconds", period.getMinutes(), period.getSeconds());
        } else {
            result = String.format("%d.%d seconds", period.getSeconds(), period.getMillis());
        }
        return result;
    }
}
