package eu.europeana.fulltext.api.service;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.pgentity.*;
import eu.europeana.fulltext.pgrepository.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by luthien on 05/08/2021.
 */
public class PgService {

    @Autowired
    private PgDatasetRepository  pgDatasetRepository;
    @Autowired
    private PgLocaldocRepository pgLocaldocRepository;
    @Autowired
    private PgLanguageRepository pgLanguageRepository;
    @Autowired
    private PgRightsRepository   pgRightsRepository;
    @Autowired
    private PgAnnotationRepository pgAnnotationRepository;
    @Autowired
    private PgResourceRepository   pgResourceRepository;
    @Autowired
    private PgAnnopageRepository   pgAnnopageRepository;

    /**
     * Persist Annopage in PostgreSQL
     * @param  resource input data
     * @param  annoPage input data
     */
    public void saveFTRecord(Resource resource, AnnoPage annoPage){
        List<PgAnnotation> pgAnnotationList = new ArrayList<>();

        PgResource pgResource = new PgResource(addOrGetLanguage(resource.getLang()),
                                               addOrGetRights(resource.getRights()),
                                               true,
                                               resource.getSource());

        PgAnnopage pgAnnopage = new PgAnnopage(addOrGetDataset(annoPage.getDsId()),
                                               addOrGetLocaldoc(annoPage.getLcId()),
                                               annoPage.getPgId());

        for (Annotation annotation : annoPage.getAns()){
            pgAnnotationList.add(new PgAnnotation(pgAnnopage,
                                                  annotation.getDcType(),
                                                  annotation.getFrom(),
                                                  annotation.getTo()));
        }
        pgAnnopage.setTargetUrl(annoPage.getTgtId());
        pgAnnopage.setPgAnnotations(pgAnnotationList);
        pgAnnopage.setDateModified(annoPage.getModified());
        pgAnnopage.setPgResource(pgResource);
        pgAnnopageRepository.save(pgAnnopage);
    }

    private PgDataset addOrGetDataset(String value) {
        Optional<PgDataset> optPgDataset = pgDatasetRepository.findByValue(value);
        return optPgDataset.orElseGet(() -> pgDatasetRepository.save(new PgDataset(value)));
    }

    private PgLocaldoc addOrGetLocaldoc(String value) {
        Optional<PgLocaldoc> optPgLocaldoc = pgLocaldocRepository.findByValue(value);
        return optPgLocaldoc.orElseGet(() -> pgLocaldocRepository.save(new PgLocaldoc(value)));
    }

    private PgLanguage addOrGetLanguage(String value) {
        Optional<PgLanguage> optPgLanguage = pgLanguageRepository.findByValue(value);
        return optPgLanguage.orElseGet(() -> pgLanguageRepository.save(new PgLanguage(value)));
    }

    private PgRights addOrGetRights(String value) {
        Optional<PgRights> optPgRights = pgRightsRepository.findByValue(value);
        return optPgRights.orElseGet(() -> pgRightsRepository.save(new PgRights(value)));
    }

}
