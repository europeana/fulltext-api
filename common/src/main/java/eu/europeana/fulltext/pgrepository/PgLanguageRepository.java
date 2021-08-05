package eu.europeana.fulltext.pgrepository;

import eu.europeana.fulltext.pgentity.PgLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgLanguageRepository extends JpaRepository<PgLanguage, Integer> {

    Optional<PgLanguage> findByValue(String value);

}
