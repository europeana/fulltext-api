package eu.europeana.fulltext.repository;

import java.io.Serializable;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.WriteResult;

/**
 * Created by luthien on 01/10/2018.
 */
public interface CrudRepository<T, ID extends Serializable> {
    Key<T> create(T entity);

    T read(ID id);

    UpdateResults update(T entity, UpdateOperations<T> operations);

    WriteResult delete(T entity);

    UpdateOperations<T> createOperations();
}
