package eu.europeana.fulltext.indexing.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Buffer<T> {
    private static final Logger logger = LogManager.getLogger(Buffer.class);
    private List<T> buffer = new ArrayList<T>();
    private Integer maxCapacity;
    private Integer itemsBuffered;
    private BatchProcessor<T> batchProcessor;
    private int batchNumber;

    //TEST
    public Set<String> ids = new HashSet<String>();
    public int maxChars = 0;

    public Integer getCapacity() {
        return maxCapacity;
    }

    public Integer getItemsBuffered() {
        return itemsBuffered;
    }

    protected int getBatchNumber() {
        return batchNumber;
    }

    public Buffer(Integer maxCapacity, BatchProcessor<T> batchProcessor) {
        this.maxCapacity = maxCapacity;
        this.itemsBuffered = 0;
        this.batchProcessor = batchProcessor;
        this.batchNumber = 0;
    }

    synchronized public void add(T item) throws IOException, SolrServerException {
        if (this.isFull()) {
            logger.info("Batch " + ++batchNumber + " / " + buffer.size() + " items (total sent to process: " + getItemsBuffered() + ")");
            batchProcessor.process(this.retrieve());
        }
        buffer.add(item);
        itemsBuffered++;
    }

    synchronized public List<T> retrieve(){
        List<T> items = new ArrayList<T>(buffer);
        buffer.clear();
        return items;
    }

    synchronized public void clear() {
        buffer.clear();
    }

    synchronized public Boolean isFull() {
        if (buffer.size() >= maxCapacity) {
            return true;
        }
        return false;
    }

    synchronized public Boolean isEmpty() {
        return buffer.isEmpty();
    }

    synchronized public void dispose() throws IOException, SolrServerException {
        if (!buffer.isEmpty()) {
            logger.info("(Last) Batch " + ++batchNumber + " / " + buffer.size() + " items (total sent to process: " + getItemsBuffered() + ")");
            batchProcessor.process(this.retrieve());
            buffer.clear();
        }
    }

}
