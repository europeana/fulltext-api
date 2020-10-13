package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;
import dev.morphia.mapping.MapperOptions;

public class MorphiaUtils {

    private MorphiaUtils() {
        // private constructor to prevent instantiation
    }

    public static final MapperOptions MAPPER_OPTIONS = MapperOptions
            .builder()
            // use legacy key for backwards-compatibility
            .discriminatorKey("className")
            .build();

    // Morphia deletes the first matching document by default. This can be used for deleting all matches
    public static final DeleteOptions MULTI_DELETE_OPTS = new DeleteOptions().multi(true);

}
