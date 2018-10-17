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

package eu.europeana.fulltext.common.repository.impl;

import com.mongodb.*;
import eu.europeana.fulltext.common.entity.Resource;
import eu.europeana.fulltext.common.repository.ResourceRepository;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Key;
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

    @Autowired
    private AdvancedDatastore datastore;

    /**
     * Check if a Resource exists that matches the given parameters
     * @param datasetId
     * @param localId
     * @param resId
     * @return true if yes, otherwise false
     */
    public boolean existsByLimitOne(String datasetId, String localId, String resId) {
        DBCollection col = datastore.getCollection(Resource.class);
        DBObject query= new BasicDBObject();
        query.put("dsId", datasetId);
        query.put("lcId", localId);
        query.put("_id", resId);
        DBCursor cur   = col.find(query).limit(1);
        int      count = cur.count();
        cur.close();
        return (count >= 1);
    }

    /**
     * Find a Resource that matches the given parameters
     * @param datasetId
     * @param localId
     * @param resId
     * @return List containing matching Resource(s) (should be just one)
     */
    public Resource findByDatasetLocalResId(String datasetId, String localId, String resId) {
        org.mongodb.morphia.query.Query<Resource> findDLPQuery =
                datastore.createQuery(Resource.class)
                         .filter("dsId ==", datasetId)
                         .filter("lcId ==", localId)
                         .filter("_id ==", resId);
        return findDLPQuery.get();
    }

    /**
     * Deletes all resources part of a particular dataset
     * @param datasetId
     * @return the number of deleted resources
     */
    public int deleteDataset(String datasetId) {
        DBCollection col           = datastore.getCollection(Resource.class);
        DBObject     deleteDSQuery = new BasicDBObject();
        deleteDSQuery.put("dsId", datasetId);
        WriteResult result = col.remove(deleteDSQuery);
        return result.getN();
    }

    public Resource saveAndReturn(Resource resToSave){
        Key<Resource> resKeySaved = create(resToSave);
        return (Resource) getObjectByKey(Resource.class, resKeySaved);
    }

    public void save(Resource resToSave){
        create(resToSave);
    }
}
