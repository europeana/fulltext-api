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
import eu.europeana.fulltext.common.entity.AnnoPage;
import eu.europeana.fulltext.common.repository.AnnoPageRepository;
import org.bson.types.ObjectId;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


/**
 * Created by luthien on 31/05/2018.
 */
@Repository
public class AnnoPageRepositoryImpl extends BaseRepository<AnnoPage, ObjectId> implements AnnoPageRepository {

    public AnnoPageRepositoryImpl() {
        super(AnnoPage.class);
    }

    @Autowired
    private AdvancedDatastore datastore;


    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * using DBCursor = DBCollection.find().limit(1) & DBCursor.count()
     * @param datasetId
     * @param localId
     * @param pageId
     * @return true if yes, otherwise false
     */
    public boolean existsByLimitOne(String datasetId, String localId, String pageId) {
        DBCollection col = datastore.getCollection(AnnoPage.class);
        DBObject query= new BasicDBObject();
        query.put("dsId", datasetId);
        query.put("lcId", localId);
        query.put("pgId", pageId);
        DBCursor cur = col.find(query).limit(1);
        int count = cur.count();
        cur.close();
        return (count >= 1);
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * using DBCollection.findOne()
     * @param datasetId
     * @param localId
     * @param pageId
     * @return true if yes, otherwise false
     */
    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    public boolean existsByFindOne(String datasetId, String localId, String pageId) {
        DBCollection col = datastore.getCollection(AnnoPage.class);
        DBObject query= new BasicDBObject();
        query.put("dsId", datasetId);
        query.put("lcId", localId);
        query.put("pgId", pageId);
        return (null != col.findOne(query));
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * using DBCollection.count()
     * @param datasetId
     * @param localId
     * @param pageId
     * @return true if yes, otherwise false
     */
    @Deprecated // keeping this temporarily for testing speed (EA-1239)
    public boolean existsByCount(String datasetId, String localId, String pageId) {
        DBCollection col = datastore.getCollection(AnnoPage.class);
        DBObject query= new BasicDBObject();
        query.put("dsId", datasetId);
        query.put("lcId", localId);
        query.put("pgId", pageId);
        return (col.count(query) >= 1);
    }

    /**
     * Check if an AnnoPage exists that contains an Annotation that matches the given parameters
     * @param datasetId
     * @param localId
     * @param annoId
     * @return true if yes, otherwise false
     */
//    @ExistsQuery("{'dsId':'?0', 'lcId':'?1', 'ans.anId':'?2'}")
    public boolean existsWithAnnoId(String datasetId, String localId, String annoId) {
        DBCollection col = datastore.getCollection(AnnoPage.class);
        DBObject query= new BasicDBObject();
        query.put("dsId", datasetId);
        query.put("lcId", localId);
        query.put("ans.anId", annoId);
        DBCursor cur = col.find(query).limit(1);
        int count = cur.count();
        cur.close();
        return (count >= 1);
    }

    /**
     * Find and return an AnnoPage that matches the given parameters
     * @param datasetId
     * @param localId
     * @param pageId
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalPageId(String datasetId, String localId, String pageId) {
        Query<AnnoPage> findDLPQuery = datastore.createQuery(AnnoPage.class)
                .filter("dsId ==", datasetId)
                .filter("lcId ==", localId)
                .filter("pgId ==", pageId);
        return findDLPQuery.get();
    }

    /**
     * Find and return AnnoPage that contains an annotation that matches the given parameters
     * @param datasetId
     * @param localId
     * @param annoId
     * @return AnnoPage
     */
    public AnnoPage findByDatasetLocalAnnoId(String datasetId, String localId, String annoId) {
        Query<AnnoPage> findDLPQuery = datastore.createQuery(AnnoPage.class)
                                                .filter("dsId ==", datasetId)
                                                .filter("lcId ==", localId)
                                                .filter("ans.anId ==", annoId);
        return findDLPQuery.get();
    }

    // used in MongoIntegrationTest
    public AnnoPage getAnnoPageByKey(Key<AnnoPage> key){
        return datastore.getByKey(AnnoPage.class, key);
    }


    /**
     * Deletes all annotation pages part of a particular dataset
     * @param datasetId
     * @return the number of deleted annotation pages
     */
    public int deleteDataset(String datasetId) {
        DBCollection col = datastore.getCollection(AnnoPage.class);
        DBObject deleteDSQuery= new BasicDBObject();
        deleteDSQuery.put("dsId", datasetId);
        WriteResult result = col.remove(deleteDSQuery);
        return result.getN();
    }

    public AnnoPage saveAndReturn(AnnoPage apToSave){
        Key<AnnoPage> apKeySaved = create(apToSave);
        return (AnnoPage) getObjectByKey(AnnoPage.class, apKeySaved);
    }

    public void save(AnnoPage apToSave){
        create(apToSave);
    }

}
