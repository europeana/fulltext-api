package eu.europeana.fulltext.repository.impl;

import java.io.Serializable;

/**
 * Created by luthien on 01/10/2018.
 */
public class BaseRepository <T, ID extends Serializable> implements CrudRepository<T, ID> {

//    @Autowired
//    private AdvancedDatastore datastore;
//    private Class<T>          t;
//
//	BaseRepository(Class<T> t) {
//        this.t = t;
//    }
//
//    public Object getObjectByKey(Class<T> type, Key<T> key){
//	    return datastore.getByKey(type, key);
//    }
//
//    @Override
//    public Key<T> create(T entity) {
//        return datastore.save(entity);
//    }
//
//    @Override
//    public T read(ID id) {
//        return datastore.createQuery(t, id);
//    }
//
//    @Override
//    public UpdateResults update(T entity, UpdateOperations< T > operations) {
//        return datastore.update(entity, operations);
//    }
//
//    @Override
//    public WriteResult delete(T entity) {
//        return datastore.delete(entity);
//    }
//
//    @Override
//    public UpdateOperations<T> createOperations() {
//        return datastore.createUpdateOperations(t);
//    }

}
