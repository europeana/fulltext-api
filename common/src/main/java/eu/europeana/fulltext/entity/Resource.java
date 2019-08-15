package eu.europeana.fulltext.entity;

import dev.morphia.annotations.*;

/**
 * Created by luthien on 31/05/2018.
 */
@Entity(value = "Resource")
@Indexes(@Index(fields = { @Field("dsId"), @Field("lcId"), @Field("_id") }, options = @IndexOptions(unique = true)))
public class Resource {

    @Id
    private String id;    // custom Mongo ID
    private String dsId;  // IIIF_API_BASE_URL/{dsId}/      /annopage/
    private String lcId;  // IIIF_API_BASE_URL/      /{lcId}/annopage/
    private String lang;
    private String value;

    public Resource() {
    }

    public Resource(String id, String lang, String value) {
        this.id = id;
        this.lang = lang;
        this.value = value;
    }

    public Resource(String id, String lang, String value, String dsId, String lcId) {
        this(id, lang, value);
        this.dsId = dsId;
        this.lcId = lcId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getDsId() {
        return dsId;
    }

    public void setDsId(String dsId) {
        this.dsId = dsId;
    }

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
    }

}
