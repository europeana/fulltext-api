package eu.europeana.fulltext.migrations;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.migrations.model.MigrationJobMetadata;
import eu.europeana.fulltext.migrations.repository.MigrationRepository;
import java.util.Iterator;
import java.util.List;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

public class MigrationAnnoPageIdReader extends MigrationAnnoPageReader{

  public MigrationAnnoPageIdReader(int limit,
      MigrationRepository repository,
      MigrationJobMetadata jobMetadata) {
    super(limit, repository, jobMetadata);
  }


  @NotNull
  @Override
  protected Iterator<AnnoPage> doPageRead() {
    return super.doPageRead();
  }


  @Override
  protected String getClassName() {
    return MigrationAnnoPageIdReader.class.getName();
  }

  @Override
  protected List<AnnoPage> getAnnoPages(int count, ObjectId objectId) {
    return repository.getAnnoPages(count, objectId, true);
  }
}
