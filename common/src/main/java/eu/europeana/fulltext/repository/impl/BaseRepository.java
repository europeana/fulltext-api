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

package eu.europeana.fulltext.repository.impl;

import java.io.Serializable;

import com.mongodb.WriteResult;
import eu.europeana.fulltext.repository.CrudRepository;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Created by luthien on 01/10/2018.
 */
public class BaseRepository <T, ID extends Serializable> implements CrudRepository<T, ID> {

    @Autowired
    private AdvancedDatastore datastore;
    private Class<T>          t;

	BaseRepository(Class<T> t) {
        this.t = t;
    }

    public Object getObjectByKey(Class<T> type, Key<T> key){
	    return datastore.getByKey(type, key);
    }

    @Override
    public Key<T> create(T entity) {
        return datastore.save(entity);
    }

    @Override
    public T read(ID id) {
        return datastore.get(t, id);
    }

    @Override
    public UpdateResults update(T entity, UpdateOperations< T > operations) {
        return datastore.update(entity, operations);
    }

    @Override
    public WriteResult delete(T entity) {
        return datastore.delete(entity);
    }

    @Override
    public UpdateOperations<T> createOperations() {
        return datastore.createUpdateOperations(t);
    }

}
