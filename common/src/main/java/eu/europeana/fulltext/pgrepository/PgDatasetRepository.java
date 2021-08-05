package eu.europeana.fulltext.pgrepository;

import eu.europeana.fulltext.pgentity.PgDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Created by luthien on 04/08/2021.
 */
@Repository
public interface PgDatasetRepository extends JpaRepository<PgDataset, Integer> {

    Optional<PgDataset> findByValue(String value);

}
