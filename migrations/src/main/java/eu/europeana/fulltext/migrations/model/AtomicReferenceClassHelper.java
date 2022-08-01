package eu.europeana.fulltext.migrations.model;

import java.util.concurrent.atomic.AtomicReference;
import org.bson.types.ObjectId;

public class AtomicReferenceClassHelper {

  public static final AtomicReference<ObjectId> ATOMIC_REF_TYPE = new AtomicReference<>();

}
