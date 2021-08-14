package eu.europeana.fulltext.api.pgrepository;

import eu.europeana.fulltext.api.pgentity.PgAPView;
import eu.europeana.fulltext.api.pgentity.PgAnnopage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgAPViewRepository extends JpaRepository<PgAPView, Long> {

    PgAPView findFirstByDatasetAndLocaldocAndPageAndLanguage(Integer dataset, String localdoc, Integer page, String language);

    PgAPView findFirstByDatasetAndLocaldocAndPageAndOriginalTrue(Integer dataset, String localdoc, Integer page);


}
