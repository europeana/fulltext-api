package eu.europeana.fulltext.util;

import dev.morphia.DeleteOptions;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.NamingStrategy;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import org.bson.Document;

import java.util.*;

import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;

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

    /**
     * Creates Aggregation pipeline to fetch the translations, and it's translations Resource
     * This is to avoid the issue of Translation Resource being fetched as AnnoPage.res
     *
     * @param datasetId
     * @param localId
     * @param pageId
     * @param lang
     * @return
     */
    public static List<Document> getAggregatePipelineForTranslation(String datasetId, String localId, String pageId, String lang) {
       return Arrays.asList(
                new Document(
                        MONGO_MATCH,
                        new Document(DATASET_ID, datasetId)
                                .append(LOCAL_ID, localId)
                                .append(PAGE_ID, pageId)
                                .append(LANGUAGE, lang)),
                new Document(
                        MONGO_LOOKUP,
                        new Document(MONGO_FROM, TranslationResource.class.getSimpleName())
                                .append(MONGO_LOCAL_FIELD, MONGO_RESOURCE_REF_ID)
                                .append(MONGO_FOREIGN_FIELD, DOC_ID)
                                .append(MONGO_AS, RESOURCE))
       );
    }

    // process the Document from Mongo mainly used for Translations
    public static TranslationAnnoPage processMongoDocument(
            Document document, String datasetId, String localId, String pageId, String lang) {
        if (document != null) {
            TranslationAnnoPage annoPage = new TranslationAnnoPage();
            annoPage.setDsId(datasetId);
            annoPage.setLcId(localId);
            annoPage.setPgId(pageId);
            annoPage.setLang(lang);
            annoPage.setAns(getAnnotations(document));
            annoPage.setTgtId(document.getString(TARGET_ID));
            annoPage.setSource(document.getString(SOURCE));
            annoPage.setModified((Date) document.get(MODIFIED));
            TranslationResource resource = getResource(((List<Document>) document.get(RESOURCE)).get(0), datasetId, localId, lang);
            annoPage.setRes(resource);
            return annoPage;
        }
        return null;
    }

    private static List<Annotation> getAnnotations(Document document) {
        List<Annotation> annotations = new ArrayList<>();
        List<Document> ansList = (List<Document>) document.get(ANNOTATIONS);
        for (Document ans: ansList) {
            Annotation annotation = new Annotation(ans.getString(AN_ID), ans.getString(DC_TYPE).charAt(0), ans.getInteger("from"), ans.getInteger("to"));
            List<Target> target = getTgs(ans);
            if (target != null && !target.isEmpty()) {
                annotation.setTgs(target);
            }
            if (ans.containsKey(LANGUAGE)) {
                annotation.setLang(ans.getString(LANGUAGE));
            }
            if (ans.containsKey(MOTIV)) {
                annotation.setMotiv(ans.getString(MOTIV));
            }
            annotations.add(annotation);
        }
        return annotations;
    }

    private static List<Target> getTgs(Document document) {
        if (document.containsKey(TARGETS)) {
            List<Target> target = new ArrayList<>();
            List<Document> tgs = (List<Document>) document.get(TARGETS);
            for (Document tg:tgs) {
                    target.add(getTargetValues(tg));
            }
            return target;
        }
        return new ArrayList<>();
    }

    private static Target getTargetValues(Document doc) {
        Target target = new Target();
        if(doc.containsKey("x")) {
            target.setX(doc.getInteger("x"));
        }
        if(doc.containsKey("y")) {
            target.setY(doc.getInteger("y"));
        }
        if(doc.containsKey("w")) {
            target.setW(doc.getInteger("w"));
        }
        if(doc.containsKey("h")) {
            target.setH(doc.getInteger("h"));
        }
        if(doc.containsKey("start")) {
            target.setStart(doc.getInteger("start"));
        }
        if(doc.containsKey("end")) {
            target.setEnd(doc.getInteger("end"));
        }
        return target;
    }

    private static TranslationResource getResource(Document res, String datasetId, String localId, String lang) {
        TranslationResource resource = new TranslationResource();
        resource.setId(res.getString(DOC_ID));
        resource.setLang(lang);
        resource.setRights(res.getString(RIGHTS));
        resource.setValue(res.getString(VALUE));
        if (res.containsKey(SOURCE)) {
            resource.setSource(res.getString(SOURCE));
        }
        resource.setDsId(datasetId);
        resource.setLcId(localId);
        return resource;
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
        public static final String TRANSLATIONS = "translations";
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


        private Fields() {
            // private constructor to prevent instantiation
        }
    }

}
