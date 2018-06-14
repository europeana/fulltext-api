package eu.europeana.fulltext.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.fulltext.config.FTSettings;
import eu.europeana.fulltext.entity.FTAnnotation;
import eu.europeana.fulltext.entity.FTResource;
import eu.europeana.fulltext.repository.FTAnnotationRepository;
import eu.europeana.fulltext.repository.FTResourceRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 *
 * @author Lúthien
 * Created on 27-02-2018
 */
@Service
public class FTService {

    @Autowired
    FTResourceRepository   ftResRepo;

    @Autowired
    FTAnnotationRepository ftAnnRepo;

    private static final Logger LOG = LogManager.getLogger(FTService.class);

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    private FTSettings FTSettings;

    public FTService(FTSettings FTSettings) {
        this.FTSettings = FTSettings;

        // configure jsonpath: we use jsonpath in combination with Jackson because that makes it easier to know what
        // type of objects are returned (see also https://stackoverflow.com/a/40963445)
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonNodeJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                if (FTSettings.getSuppressParseException()) {
                    // we want to be fault tolerant in production, but for testing we may want to disable this option
                    return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
                } else {
                    return EnumSet.noneOf(Option.class);
                }
            }
        });

        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule());
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }


    public Optional<FTAnnotation> findAnnotation(String datasetId, String recordId, String annoId){
            return ftAnnRepo.findById(datasetId + "/" + recordId + "/" + annoId);
    }

    public FTAnnotation listAllAnnotations(String datasetId, String recordId, String pageId){
        return ftAnnRepo.findById(pageId).get();
    }

    /**
     * @return FulltextConfig object containing properties and Mongo datastore
     */
    public FTSettings getConfig() {
        return FTSettings;
    }



    /**
     * initial Mongo and Morphia setup testing
     */
    private FTResource   ftr;
    private FTAnnotation fta0;
    private FTAnnotation fta1;
    private FTAnnotation fta2;


    public void do_args_method(String zampano, String gelsomina){
        final String ilMatto = "9200396/BibliographicResource_3000118436096/";
        annotationKnitter(ilMatto, zampano, gelsomina);
        ftAnnRepo.save(fta0);
        ftAnnRepo.save(fta1);
        ftAnnRepo.save(fta2);

        resourcePunniker(ilMatto, zampano);
        ftr.setPageAnnotation(fta0);
        ftr.setFTAnnotations(Arrays.asList(fta1, fta2));
        ftResRepo.save(ftr);
    }

    private void annotationKnitter(String ilMatto, String zampano, String gelsomina){
        this.fta0 = new FTAnnotation(ilMatto + gelsomina + "_page",
                                     "A",
                                     "en",
                                     0,
                                     0,
                                     0,
                                     0,
                                     0,
                                     0,
                                     ilMatto + zampano);
        this.fta1 = new FTAnnotation(ilMatto + gelsomina + "_0001",
                                     "W",
                                     "en",
                                     1,
                                     5,
                                     234,
                                     311,
                                     87,
                                     16,
                                     ilMatto + zampano);
        this.fta2 = new FTAnnotation(ilMatto + gelsomina + "_0002",
                                     "W",
                                     "en",
                                     7,
                                     15,
                                     304,
                                     310,
                                     127,
                                     15,
                                     ilMatto + zampano);
    }

    private void resourcePunniker(String ilMatto, String zampano){
        this.ftr = new FTResource(ilMatto + zampano,
                                  "en",
                                  "https://imageserver.net/" + zampano + "_001.jpg",
                                  zampano + " ende staet hier istu ghescreyphen, ghy deckschelse snoodaart!");
    }


}
