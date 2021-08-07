package eu.europeana.fulltext.api.pgrepository;

import eu.europeana.fulltext.api.pgentity.PgLocaldoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgLocaldocRepository extends JpaRepository<PgLocaldoc, Long> {

    Optional<PgLocaldoc> findByValue(String value);

}
