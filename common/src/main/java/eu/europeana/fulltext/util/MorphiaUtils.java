package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;

public class MorphiaUtils {

    private MorphiaUtils() {
        // private constructor to prevent instantiation
    }

    public static final MapperOptions MAPPER_OPTIONS = MapperOptions
            .builder()
            // use legacy settings for backwards-compatibility
            .discriminatorKey("className")
            .discriminator(DiscriminatorFunction.className())
            .fieldNaming(NamingStrategy.identity())
            .build();

    // Morphia deletes the first matching document by default. This can be used for deleting all matches
    public static final DeleteOptions MULTI_DELETE_OPTS = new DeleteOptions().multi(true);

}
