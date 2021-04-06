package eu.europeana.fulltext.entity;

import dev.morphia.annotations.*;

/**
 * Resource stored as translation
 * Created by P.Ehlert on 25 March 2021.
 */
@Entity(value = "TranslationResource", useDiscriminator = false)
@Indexes(@Index(fields = { @Field("dsId"), @Field("lcId"), @Field("_id") }, options = @IndexOptions(unique = true)))
public class TranslationResource extends Resource {

}
