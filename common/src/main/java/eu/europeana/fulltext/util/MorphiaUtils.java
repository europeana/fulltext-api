package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;

public final class MorphiaUtils {

    public static final MapperOptions MAPPER_OPTIONS = MapperOptions.builder()
                                                                    // use legacy settings for backwards-compatibility
                                                                    .discriminatorKey(Fields.CLASSNAME)
                                                                    .discriminator(DiscriminatorFunction.className())
                                                                    .fieldNaming(NamingStrategy.identity())
                                                                    .build();

    // Morphia deletes the first matching document by default. This is required for deleting all matches.
    public static final DeleteOptions MULTI_DELETE_OPTS = new DeleteOptions().multi(true);


    private MorphiaUtils() {
        // private constructor to prevent instantiation
    }

    // Collection field names
    public static final class Fields {

        public static final String DOC_ID      = "_id";
        public static final String DATASET_ID  = "dsId";
        public static final String LOCAL_ID    = "lcId";
        public static final String PAGE_ID     = "pgId";
        public static final String IMAGE_ID    = "tgtId";
        public static final String LANGUAGE    = "lang";
        public static final String CLASSNAME   = "className";
        public static final String ANNOTATIONS = "ans";
        public static final String RESOURCE    = "res";
        public static final String MODIFIED    = "modified";

        //Mongo Fields
        public static final String MONGO_MATCH      = "$match";
        public static final String MONGO_PROJECT    = "$project";
        public static final String MONGO_EXPRESSION = "$expr";
        public static final String MONGO_AND        = "$and";
        public static final String MONGO_EQUALS     = "$eq";
        public static final String MONGO_LOOKUP     = "$lookup";
        public static final String MONGO_FROM       = "from";
        public static final String MONGO_LET        = "let";
        public static final String MONGO_AS         = "as";
        public static final String MONGO_PIPELINE   = "pipeline";
        public static final String MONGO_DATASET_ID = "$" + DATASET_ID;
        public static final String MONGO_LOCAL_ID   = "$" + LOCAL_ID;
        public static final String MONGO_PAGE_ID    = "$" + PAGE_ID;
        public static final String TRANSLATIONS     = "translations";

        public static final String ANNOTATIONS_DCTYPE = ANNOTATIONS + ".dcType";
        public static final String ANNOTATIONS_ID     = ANNOTATIONS + ".anId";

        private Fields() {
            // private constructor to prevent instantiation
        }
    }

}
