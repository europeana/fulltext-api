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
    private FTAnnotation fta1;
    private FTAnnotation fta2;


    public void do_args_method(String pinocchio){
        annotationKnitter(pinocchio);
        ftAnnRepo.save(fta1);
        ftAnnRepo.save(fta2);

        resourcePunniker(pinocchio);
//        ftr.setFTAnnotations(Stream.of(fta1, fta2).collect(Collectors.toSet()));
        ftr.setFTAnnotations(Arrays.asList(fta1, fta2));
        ftResRepo.save(ftr);
    }

    private void annotationKnitter(String headBanger){
        this.fta1 = new FTAnnotation(headBanger + "_annotation_0001",
                                     "W",
                                     "en",
                                     1,
                                     5,
                                     234,
                                     311,
                                     87,
                                     16);
        this.fta2 = new FTAnnotation(headBanger + "_annotation_0002",
                                     "W",
                                     "en",
                                     7,
                                     15,
                                     304,
                                     310,
                                     127,
                                     15);
    }

    private void resourcePunniker(String floppyCheekiness){
        this.ftr = new FTResource("resource_" + floppyCheekiness,
                                  "en",
                                  "https://imageserver.net/resource_001.jpg",
                                        floppyCheekiness + "Ank Amon Ammoniak - op een kaalkop staat geen haar!",
                                  "no");
    }


}
