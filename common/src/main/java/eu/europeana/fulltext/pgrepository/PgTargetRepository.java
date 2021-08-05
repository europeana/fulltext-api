package eu.europeana.fulltext.pgrepository;

import eu.europeana.fulltext.pgentity.PgTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgTargetRepository extends JpaRepository<PgTarget, Long> {


}
