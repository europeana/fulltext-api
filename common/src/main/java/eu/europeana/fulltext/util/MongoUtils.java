package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;

public class MongoUtils {

    // Morphia deletes the first matching document by default. This can be used for deleting all matches
    public static final DeleteOptions MULTI_DELETE_OPTS = new DeleteOptions().multi(true);

}
