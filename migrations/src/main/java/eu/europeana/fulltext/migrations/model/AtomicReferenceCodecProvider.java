package eu.europeana.fulltext.migrations.model;

import static eu.europeana.fulltext.migrations.model.AtomicReferenceClassHelper.ATOMIC_REF_TYPE;

import org.bson.codecs.Codec;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

public class AtomicReferenceCodecProvider implements PropertyCodecProvider {

  @Override
  public <T> Codec<T> get(
      TypeWithTypeParameters<T> type, PropertyCodecRegistry propertyCodecRegistry) {
    Class<T> clazz = type.getType();
    if (ATOMIC_REF_TYPE.getClass().isAssignableFrom(clazz)) {
      new AtomicReferenceCodec();
    }
    return null;
  }
}
