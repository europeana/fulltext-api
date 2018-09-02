/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.api.repository;

import eu.europeana.fulltext.api.entity.AnnoPage;
import org.springframework.data.mongodb.repository.CountQuery;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 */
@Repository
@RepositoryRestResource(collectionResourceRel = "AnnoPage", path = "AnnoPage")
public interface AnnoPageRepository extends MongoRepository<AnnoPage, String> {


    @Query("{'dsId':'?0', 'lcId':'?1', 'pgId':'?2'}")
    List<AnnoPage> findByDatasetLocalAndPageId(String datasetId, String localId, String pageId);

    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    @Query("{'dsId':'?0', 'lcId':'?1', 'pgId':'?2'}{ _id : 1}")
    AnnoPage findOneWithId(String datasetId, String localId, String pageId);

    @ExistsQuery("{'dsId':'?0', 'lcId':'?1', 'pgId':'?2'}")
    Boolean existsWithId(String datasetId, String localId, String pageId);

    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    @CountQuery("{'dsId':'?0', 'lcId':'?1', 'pgId':'?2'}")
    Integer countWithId(String datasetId, String localId, String pageId);

    @Query("{'dsId':'?0', 'lcId':'?1', 'ans.anId':'?2'}")
    List<AnnoPage> findByDatasetLocalAndAnnoId(String datasetId, String localId, String annoId);


}
