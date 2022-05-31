package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;
import eu.europeana.fulltext.entity.Resource;
import java.util.Arrays;

public final class MorphiaUtils {

    /** Matches spring.profiles.active property in test/resource application.properties file */
    public static final String ACTIVE_TEST_PROFILE = "test";

    public static final MapperOptions MAPPER_OPTIONS = MapperOptions.builder()
                                                                    // use legacy settings for backwards-compatibility
                                                                    .discriminatorKey(Fields.CLASSNAME)
                                                                    .discriminator(DiscriminatorFunction.className())
                                                                    .fieldNaming(NamingStrategy.identity())
                                                                    .build();

    public static final String SET_ON_INSERT = "$setOnInsert";
    public static final String SET = "$set";
    public static final String RESOURCE_COL = Resource.class.getSimpleName();


    // Morphia deletes the first matching document by default. This is required for deleting all matches.
    public static final DeleteOptions MULTI_DELETE_OPTS = new DeleteOptions().multi(true);

    // Indicates that an update query should be executed as an "upsert",
    // ie. creates new records if they do not already exist, or updates them if they do.
    public static final UpdateOptions UPSERT_OPTS = new UpdateOptions().upsert(true);

    public static boolean testProfileNotActive(String activeProfileString) {
        return Arrays.stream(activeProfileString.split(",")).noneMatch(ACTIVE_TEST_PROFILE::equals);
    }

    public static void validateDeletion(String activeProfileString) {
        if (MorphiaUtils.testProfileNotActive(activeProfileString)) {
            throw new IllegalStateException(
                String.format(
                    "Attempting to drop collection outside testing. activeProfiles=%s",
                    activeProfileString));
        }
    }


    private MorphiaUtils() {
        // private constructor to prevent instantiation
    }




    // Collection field names
    public static final class Fields {

        public static final String ANNOTATION   = "annotation";
        public static final String ANNOTATIONS  = "ans";
        public static final String CLASSNAME    = "className";
        public static final String DATASET_ID   = "dsId";
        public static final String DOC_ID       = "_id";
        public static final String LANGUAGE     = "lang";
        public static final String LOCAL_ID     = "lcId";
        public static final String MODIFIED     = "modified";
        public static final String PAGE_ID      = "pgId";
        public static final String RESOURCE     = "res";
        public static final String TARGET_ID    = "tgtId";
        public static final String SOURCE       = "source";
        public static final String TARGETS      = "tgs";
        public static final String AN_ID        = "anId";
        public static final String DC_TYPE      = "dcType";
        public static final String MOTIV      = "motiv";
        //Mongo Fields
        public static final String MONGO_AND         = "$and";
        public static final String MONGO_ANNOTATIONS = "$" + ANNOTATIONS;
        public static final String MONGO_AS          = "as";
        public static final String MONGO_COLLECTION  = "coll";
        public static final String MONGO_CONDITION   = "cond";
        public static final String MONGO_DATASET_ID  = "$" + DATASET_ID;
        public static final String MONGO_EQUALS      = "$eq";
        public static final String MONGO_EXPRESSION  = "$expr";
        public static final String MONGO_FILTER      = "$filter";
        public static final String MONGO_FROM        = "from";
        public static final String MONGO_IN          = "$in";
        public static final String MONGO_INPUT       = "input";
        public static final String MONGO_LET         = "let";
        public static final String MONGO_LOCAL_ID    = "$" + LOCAL_ID;
        public static final String MONGO_LOOKUP      = "$lookup";
        public static final String MONGO_MATCH       = "$match";
        public static final String MONGO_PAGE_ID     = "$" + PAGE_ID;
        public static final String MONGO_PIPELINE    = "pipeline";
        public static final String MONGO_PROJECT     = "$project";
        public static final String MONGO_UNIONWITH   = "$unionWith";

        public static final String ANNOTATIONS_DCTYPE = ANNOTATIONS + ".dcType";
        public static final String ANNOTATIONS_ID     = ANNOTATIONS + ".anId";
        public static final String MONGO_FILTER_ANS_DCTYPE  = " $$ans.dcType";

        public static final String MONGO_LOCAL_FIELD = "localField";
        public static final String MONGO_FOREIGN_FIELD = "foreignField";
        public static final String MONGO_RESOURCE_REF_ID = "res.$id";
        public static final String RIGHTS = "rights";
        public static final String VALUE = "value";
        public static final String CONTRIBUTED = "contributed";


        private Fields() {
            // private constructor to prevent instantiation
        }
    }
}
