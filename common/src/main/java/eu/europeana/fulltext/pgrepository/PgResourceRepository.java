package eu.europeana.fulltext.pgrepository;

import eu.europeana.fulltext.pgentity.PgResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgResourceRepository extends JpaRepository<PgResource, Long> {

}
