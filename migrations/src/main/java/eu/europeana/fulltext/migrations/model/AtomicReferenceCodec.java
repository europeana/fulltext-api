package eu.europeana.fulltext.migrations.model;

import static eu.europeana.fulltext.migrations.model.AtomicReferenceClassHelper.ATOMIC_REF_TYPE;

import java.util.concurrent.atomic.AtomicReference;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

public class AtomicReferenceCodec implements Codec<AtomicReference<ObjectId>> {

  @Override
  public AtomicReference<ObjectId> decode(BsonReader bsonReader, DecoderContext decoderContext) {
    return new AtomicReference<>(bsonReader.readObjectId());
  }

  @Override
  public void encode(
      BsonWriter bsonWriter,
      AtomicReference<ObjectId> atomicReference,
      EncoderContext encoderContext) {
    bsonWriter.writeObjectId(atomicReference.get());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<AtomicReference<ObjectId>> getEncoderClass() {
    return (Class<AtomicReference<ObjectId>>) ATOMIC_REF_TYPE.getClass();
  }
}
