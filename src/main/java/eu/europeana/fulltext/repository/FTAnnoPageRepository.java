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

package eu.europeana.fulltext.repository;

import eu.europeana.fulltext.entity.FTAnnoPage;
import eu.europeana.fulltext.entity.FTAnnotation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Created by luthien on 31/05/2018.
 */
@Repository
@RepositoryRestResource(collectionResourceRel = "ftAnnoPage", path = "ftAnnoPage")
public interface FTAnnoPageRepository extends MongoRepository<FTAnnoPage, String> {


    //Supports native JSON query string
    @Query("{ftAnnoPage:'?0'}")
    FTAnnoPage findFTAnnoPageByDomain(String ftAnnoPage);

    @Query("{ftAnnoPage: { $regex: ?0 } })")
    List<FTAnnoPage> findFTAnnoPageByRegEx(String ftAnnoPage);

    @Query("{'dsId':'?0', 'lcId':'?1', 'pgId':'?2'}")
    List<FTAnnoPage> findByDatasetLocalAndPageId(String datasetId, String localId, String pageId);

    @Query("{'dsId':'?0', 'lcId':'?1', 'ans.anId':'?2'}")
    List<FTAnnoPage> findByDatasetLocalAndAnnoId(String datasetId, String localId, String annoId);


}
