package eu.europeana.fulltext.pgrepository;

import eu.europeana.fulltext.pgentity.PgAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgAnnotationRepository extends JpaRepository<PgAnnotation, Long> {

}
