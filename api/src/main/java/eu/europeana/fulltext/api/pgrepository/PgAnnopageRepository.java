package eu.europeana.fulltext.api.pgrepository;

import eu.europeana.fulltext.api.pgentity.PgAnnopage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgAnnopageRepository extends JpaRepository<PgAnnopage, Long> {


}
