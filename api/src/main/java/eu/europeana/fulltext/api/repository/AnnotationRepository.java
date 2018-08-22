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

import eu.europeana.fulltext.api.entity.Annotation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luthien on 31/05/2018.
 */
@Repository
@RepositoryRestResource(collectionResourceRel = "Annotation", path = "Annotation")
public interface AnnotationRepository extends MongoRepository<Annotation, String> {


    //Supports native JSON query string
    @Query("{Annotation:'?0'}")
    Annotation findAnnotationByDomain(String annotation);

    @Query("{Annotation: { $regex: ?0 } })")
    List<Annotation> findAnnotationByRegEx(String annotation);


}
