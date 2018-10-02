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

package eu.europeana.fulltext.api.repository.impl;

import eu.europeana.fulltext.api.entity.Resource;
import eu.europeana.fulltext.api.repository.ResourceRepository;
import org.mongodb.morphia.Datastore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class ResourceRepositoryImpl extends BaseRepository<Resource, String> implements ResourceRepository {

    public ResourceRepositoryImpl() {
        super(Resource.class);
    }



    // psst ... fake to let it build
    public boolean existsWithDatasetLocalAndResId(String datasetId, String localId, String resId) {
        return true;
    }

    @Autowired
    private Datastore datastore;
    public Resource findByDatasetLocalAndResId(String datasetId, String localId, String resId) {
        org.mongodb.morphia.query.Query<Resource> findDLPQuery = datastore.createQuery(Resource.class)
                                                                          .filter("dsId ==", datasetId)
                                                                          .filter("lcId ==", localId)
                                                                          .filter("id ==", resId);
        return findDLPQuery.get();
    }


//    /**
//     * Check if a Resource exists that matches the given parameters
//     * @param datasetId
//     * @param localId
//     * @param resId
//     * @return Boolean.TRUE if yes, otherwise Boolean.FALSE
//     */
//    @ExistsQuery("{'dsId':'?0', 'lcId':'?1', 'id':'?2'}")
//    Boolean existsWithDatasetLocalAndResId(String datasetId, String localId, String resId);
//
//    /**
//     * Find a Resource that matches the given parameters
//     * @param datasetId
//     * @param localId
//     * @param resId
//     * @return List containing matching Resource(s) (should be just one)
//     */
//    @Query("{'dsId':'?0', 'lcId':'?1', 'id':'?2'}")
//    List<Resource> findByDatasetLocalAndResId(String datasetId, String localId, String resId);
//
//    /**
//     * Deletes all resources part of a particular dataset
//     * @param datasetId
//     * @return the number of deleted resources
//     */
//    @DeleteQuery("{'dsId':'?0'}")
//    long deleteDataset(String datasetId);
}
